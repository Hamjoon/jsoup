#!/usr/bin/env python3
"""Check every completed release against isolated sequential compilation/execution."""
import hashlib
import json
import re
import subprocess
import time
from dev_common import EXP, write_json
from dev_later import write_csv
from llm_common import files,source,classpath,fqcn
from llm_run import run_file
from run_class import run_class
from survival_common import releases


def class_hashes(folder):
    return {str(p.relative_to(folder)):hashlib.sha256(p.read_bytes()).hexdigest() for p in folder.rglob('*.class')}


def normalized(summary):
    return dict(listed=summary['all_listed_methods'],list_error=bool(summary['list_error']),
        methods=[(r['method'],r['status'],r.get('exception')) for r in summary['methods']])


def main():
    start=time.monotonic()
    extracted={(r['class'],r['technique']):r for r in files()}
    checks=[]
    for release in releases():
        tag=release['tag']
        root=EXP/'llm'/tag
        while not (root/'final-summary.json').exists():
            if (EXP/'results/llm-later-failure.json').exists():
                raise SystemExit('Primary sweep failed; stop sequential verification')
            time.sleep(1)
        dest=root/'sequential-check'
        record_path=dest/'verification.json'
        if record_path.exists():
            check=json.loads(record_path.read_text())
        else:
            stage=time.monotonic()
            dest.mkdir(parents=True,exist_ok=True)
            compiled=json.loads((root/'compile-summary.json').read_text())
            counts=dict(files_checked=0,absent_files_checked=0,compiled_files_checked=0,run_files_checked=0,methods_checked=0)
            mismatches=[]
            for file in compiled['files_ledger']:
                name,tech=file['class'],file['technique']
                row=extracted[(name,tech)]
                source(row)
                if file['status']=='absent':
                    assert not (root/'classes'/name/tech).exists() and not (root/'javac'/f'{name}.{tech}.err').exists()
                    counts['absent_files_checked']+=1
                    continue
                output=dest/'classes'/name/tech
                output.mkdir(parents=True,exist_ok=True)
                cp=classpath(tag,name,tech).split(':')
                cp[0]=str(output)
                cp=':'.join(cp)
                command=['javac','-d',str(output),'-cp',cp,str(source(row))]
                p=subprocess.run(command,cwd=EXP,capture_output=True,text=True,errors='replace')
                err=dest/'javac'/f'{name}.{tech}.err'
                err.parent.mkdir(exist_ok=True)
                err.write_text(p.stderr)
                counts['files_checked']+=1
                if p.returncode!=file['exit'] or p.stderr!=(root/'javac'/f'{name}.{tech}.err').read_text():
                    mismatches.append(dict(scope='compile',**{'class':name},technique=tech))
                if not p.returncode:
                    counts['compiled_files_checked']+=1
                    if class_hashes(output)!=class_hashes(root/'classes'/name/tech):
                        mismatches.append(dict(scope='bytecode',**{'class':name},technique=tech))
                    run_output=dest/'run1'/f'{name}.{tech}.json'
                    try:
                        summary=run_class(fqcn(row),cp,run_output,timeout_ms=30000,working_directory=EXP/'checkouts'/tag)
                    except RuntimeError:
                        summary=json.loads(run_output.with_suffix('.summary.json').read_text())
                        assert summary['list_error']
                    canonical=json.loads((root/'run1'/f'{name}.{tech}.summary.json').read_text())
                    counts['run_files_checked']+=1
                    counts['methods_checked']+=len(summary['methods'])
                    if normalized(summary)!=normalized(canonical):
                        mismatches.append(dict(scope='run',**{'class':name},technique=tech,
                            parallel=normalized(canonical),sequential=normalized(summary)))
            check=dict(tag=tag,parallel_workers=compiled['workers'],sequential_workers=1,**counts,
                identical=not mismatches,mismatches=mismatches,seconds=round(time.monotonic()-stage,3),
                comparison='Exact javac exits/stderr and class-file bytes; exact JUnit listing and per-method status/exception type. Timings, process IDs and incidental exception-message identity hashes are excluded.')
            write_json(record_path,check)
        checks.append(check)
        write_json(EXP/'results/llm-parallel-verification.json',dict(releases=checks,all_identical=all(r['identical'] for r in checks),
            seconds=round(time.monotonic()-start,3)))
        fields=['tag','parallel_workers','sequential_workers','files_checked','absent_files_checked','compiled_files_checked','run_files_checked','methods_checked','identical','seconds']
        write_csv(EXP/'results/llm-parallel-verification.csv',[{k:r[k] for k in fields} for r in checks],fields)
        print(json.dumps({k:v for k,v in check.items() if k not in {'mismatches','comparison'}}),flush=True)
        if not check['identical']:
            raise SystemExit('STOP: parallel/sequential mismatch; see results/llm-parallel-verification.json')


if __name__=='__main__':
    main()
