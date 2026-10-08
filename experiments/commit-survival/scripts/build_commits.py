#!/usr/bin/env python3
"""Build each archived commit once, using the fixed container JDK."""
import argparse
from concurrent.futures import ThreadPoolExecutor, wait, FIRST_COMPLETED
import csv
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tarfile
import time
import xml.etree.ElementTree as ET
from csv_utils import write_csv

EXP = Path(__file__).resolve().parents[1]
REPO = EXP.parents[1]
PREV = EXP.parent/'survival'
FIELDS = ['index','point','mvn_exit','seconds','class_files','cp_compile','source_level','build_fail']
COMMANDS = [['mvn','-q','-B','-DskipTests','compile'],
            ['mvn','-q','-B','dependency:build-classpath','-DincludeScope=compile','-Dmdep.outputFile=cp-compile.txt']]


def save(path, value):
    path.write_text(json.dumps(value,indent=2)+'\n')


def source_digest(checkout):
    """Check that Maven has not changed any archived source or POM."""
    files=sorted(p for p in (checkout/'src').rglob('*') if p.is_file())+[checkout/'pom.xml']
    return {str(p.relative_to(checkout)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}


def build(commit):
    started=time.monotonic()
    checkout=EXP/'checkouts'/commit['point']
    if checkout.exists():
        raise RuntimeError('Refusing to reuse an existing point checkout: '+commit['point'])
    baseline=int(commit['index'])==0
    commands=[]
    if baseline:
        shutil.copytree(PREV/'checkouts/jsoup-1.11.2',checkout)
        prior=next(r for r in csv.DictReader((PREV/'results/builds.csv').open()) if r['tag']=='jsoup-1.11.2')
        code=int(prior['mvn_exit'])
        assert code==0 and (checkout/'cp-compile.txt').is_file()
        assert source_digest(checkout)==source_digest(PREV/'checkouts/jsoup-1.11.2')
    else:
        checkout.mkdir(parents=True)
        archive=subprocess.Popen(['git','-c',f'safe.directory={REPO}','-C',str(REPO),'archive',commit['sha']],stdout=subprocess.PIPE)
        try:
            with tarfile.open(fileobj=archive.stdout,mode='r|') as tar:
                tar.extractall(checkout)
        finally:
            archive.stdout.close()
        assert archive.wait()==0, 'Git archive failed'
        before=source_digest(checkout)
        code=0
        with (checkout/'build.log').open('w') as log:
            for command in COMMANDS:
                log.write('$ '+' '.join(command)+'\n'); log.flush()
                command_start=time.monotonic()
                code=subprocess.run(command,cwd=checkout,stdout=log,stderr=subprocess.STDOUT).returncode
                commands.append(dict(command=command,exit_status=code,seconds=round(time.monotonic()-command_start,3)))
                if code:
                    break
        assert before==source_digest(checkout), 'Maven changed archived source: '+commit['point']
    cp=checkout/'cp-compile.txt'
    levels=[n.text.strip() for n in ET.parse(checkout/'pom.xml').iter() if n.tag.split('}')[-1] in {'source','release'} and n.text]
    row=dict(index=int(commit['index']),point=commit['point'],mvn_exit=code,
             seconds=round(time.monotonic()-started,3),class_files=sum(1 for _ in (checkout/'target/classes').rglob('*.class')),
             cp_compile=cp.read_text().strip() if cp.exists() else '',source_level='|'.join(levels),build_fail=int(code!=0))
    if not code:
        assert row['class_files']>0 and cp.is_file(), 'Successful build lacks required outputs'
    save(checkout/'build-record.json',dict(**row,sha=commit['sha'],baseline_copied=baseline,commands=commands,source_unchanged=True))
    return row


def write_outputs(rows, commits):
    ordered=sorted(rows,key=lambda r:r['index'])
    write_csv(EXP/'results/builds.csv',ordered,FIELDS)
    failed=[]
    for row in ordered:
        if not row['build_fail']:
            continue
        commit=commits[row['index']]
        tail='\n'.join((EXP/'checkouts'/row['point']/'build.log').read_text(errors='replace').splitlines()[-30:])
        failed.append(dict(index=row['index'],point=row['point'],sha=commit['sha'],subject=commit['subject'],error_tail=tail))
    save(EXP/'results/build-failures.json',failed)
    lines=['# Build failures','']
    if not failed:
        lines.append('None.')
    for failure in failed:
        lines.extend([f"## {failure['index']} — {failure['sha']}",'',failure['subject'],'','```text',failure['error_tail'],'```',''])
    (EXP/'results/build-failures.md').write_text('\n'.join(lines)+'\n')
    return failed


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--workers',type=int,choices=range(1,5),default=4)
    args=parser.parse_args()
    started=time.monotonic()
    start_date=datetime.now(timezone.utc).isoformat()
    validation=json.loads((EXP/'results/commits-summary.json').read_text())
    assert not validation['errors']
    commits=list(csv.DictReader((EXP/'results/commits.csv').open()))
    assert [int(r['index']) for r in commits]==list(range(len(commits)))
    assert len(commits)==186
    if (EXP/'results/builds.csv').exists() or any((EXP/'checkouts'/r['point']).exists() for r in commits):
        raise SystemExit('STOP: build artifacts already exist; do not skip, cache, or rerun points silently')
    env=dict(java=subprocess.check_output(['java','-version'],stderr=subprocess.STDOUT,text=True),
             maven=subprocess.check_output(['mvn','-version'],stderr=subprocess.STDOUT,text=True))
    save(EXP/'results/build-environment.json',env)
    assert 'Temurin-11.' in env['java'], 'Wrong fixed JDK'
    rows=[build(commits[0])]
    write_outputs(rows,commits)
    pending=iter(commits[1:])
    stopped=False
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        running={pool.submit(build,next(pending)) for _ in range(args.workers)}
        while running:
            done,running=wait(running,return_when=FIRST_COMPLETED)
            for future in done:
                row=future.result()
                rows.append(row)
                failed=write_outputs(rows,commits)
                print(json.dumps(dict(**row,completed_later=len(rows)-1,failures=len(failed))),flush=True)
                if len(failed)>18:
                    stopped=True
            if not stopped:
                for _ in done:
                    commit=next(pending,None)
                    if commit is not None:
                        running.add(pool.submit(build,commit))
    failed=write_outputs(rows,commits)
    later=[r for r in rows if r['index']>0]
    summary=dict(complete=len(rows)==len(commits),later_attempted=len(later),later_built=sum(not r['build_fail'] for r in later),
                 later_failed=sum(r['build_fail'] for r in later),baseline_copied=True,rows=len(rows),workers=args.workers,
                 max_failures=18,stopped_at_failure_threshold=stopped,failed=failed,
                 started_at=start_date,finished_at=datetime.now(timezone.utc).isoformat(),seconds=round(time.monotonic()-started,3))
    save(EXP/'results/build-summary.json',summary)
    print(json.dumps(summary),flush=True)
    if stopped:
        raise SystemExit('STOP: build failures exceed the allowed threshold')
    assert summary['complete']


if __name__=='__main__':
    main()
