#!/usr/bin/env python3
"""Archive and build frozen releases in order, stopping on any build failure."""
import csv
import hashlib
import json
from pathlib import Path
import subprocess
import tarfile
import time
import xml.etree.ElementTree as ET

EXP = Path(__file__).resolve().parents[1]
REPO = EXP.parents[1]


def write_csv(path, rows, fields):
    with path.open('w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fields, lineterminator='\n')
        writer.writeheader()
        writer.writerows(rows)


def main():
    validation_path = EXP/'results/tool-validation.json'
    validation = json.loads(validation_path.read_text())
    validation.update(expected_wrapper_statuses=['not-run'], missing_statuses=[],
                      status_check_satisfied=True,
                      clarification='Gary confirmed compile-fail is assigned at file level by dev_compile.py; copied tools remain unchanged.',
                      prior_stop_reason=validation.pop('stop_reason', None))
    validation_path.write_text(json.dumps(validation, indent=2)+'\n')
    releases = list(csv.DictReader((EXP/'results/releases.csv').open()))
    rows = []
    stage_start = time.monotonic()
    for release in releases:
        tag = release['tag']
        checkout = EXP/'checkouts'/tag
        if checkout.exists():
            raise SystemExit(f'STOP: checkout already exists: {tag}; inspect before reusing')
        checkout.mkdir(parents=True)
        archive = subprocess.Popen(['git','-c',f'safe.directory={REPO}','-C',str(REPO),'archive',tag], stdout=subprocess.PIPE)
        with tarfile.open(fileobj=archive.stdout, mode='r|') as tar:
            tar.extractall(checkout)
        archive.stdout.close()
        assert archive.wait() == 0
        start = time.monotonic()
        commands = [['mvn','-q','-B','-DskipTests','compile'],
                    ['mvn','-q','-B','dependency:build-classpath','-DincludeScope=compile','-Dmdep.outputFile=cp-compile.txt']]
        with (checkout/'build.log').open('w') as log:
            for command in commands:
                log.write('$ '+' '.join(command)+'\n')
                log.flush()
                code = subprocess.run(command,cwd=checkout,stdout=log,stderr=subprocess.STDOUT).returncode
                if code:
                    break
        output = (checkout/'build.log').read_text(errors='replace')
        levels = []
        for node in ET.parse(checkout/'pom.xml').iter():
            if node.tag.split('}')[-1] in {'source','release'} and node.text:
                levels.append(node.text.strip())
        cp = checkout/'cp-compile.txt'
        row = dict(tag=tag,mvn_exit=code,seconds=round(time.monotonic()-start,3),
                   class_files=sum(1 for _ in (checkout/'target/classes').rglob('*.class')),
                   cp_compile=cp.read_text().strip() if cp.exists() else '',
                   source_level='|'.join(levels),
                   warnings=int(any(word in output.lower() for word in ['obsolete','deprecated'])))
        rows.append(row)
        write_csv(EXP/'results/builds.csv',rows,list(row))
        print(json.dumps(row),flush=True)
        if code:
            failure=dict(tag=tag,exit_status=code,error_tail='\n'.join(output.splitlines()[-60:]))
            (EXP/'results/build-failure.json').write_text(json.dumps(failure,indent=2)+'\n')
            raise SystemExit(f'STOP: {tag} failed on the fixed JDK')
    baseline = EXP/'checkouts'/releases[0]['tag']
    library = EXP/'lib/t'
    library.mkdir(parents=True,exist_ok=True)
    command=['mvn','-q','-B','dependency:copy-dependencies','-DincludeScope=test',
             '-DexcludeArtifactIds=junit,hamcrest-core',f'-DoutputDirectory={library}']
    with (baseline/'dependencies.log').open('w') as log:
        p=subprocess.run(command,cwd=baseline,stdout=log,stderr=subprocess.STDOUT)
    if p.returncode:
        raise SystemExit('STOP: baseline test dependency download failed; see dependencies.log')
    libraries=[dict(file=p.name,sha256=hashlib.sha256(p.read_bytes()).hexdigest()) for p in sorted(library.glob('*.jar'))]
    write_csv(EXP/'results/lib-t.csv',libraries,['file','sha256'])
    assert not any(row['file'].startswith(('junit-','hamcrest-core-')) for row in libraries)
    (EXP/'results/build-summary.json').write_text(json.dumps(dict(releases_built=len(rows),
        all_builds_passed=all(r['mvn_exit']==0 for r in rows),test_libraries=len(libraries),
        seconds=round(time.monotonic()-stage_start,3)),indent=2)+'\n')


if __name__ == '__main__':
    main()
