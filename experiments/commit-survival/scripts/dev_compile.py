#!/usr/bin/env python3
"""Compile frozen helpers and individual files; assign compile-fail at file level."""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess
import time
from dev_common import BASELINE, EXP, PREV, FROZEN_TAG, T, classpath, fqcn, inventory, point_info, test_classes, write_json


def compile_sources(output,cp,sources,log):
    output.mkdir(parents=True,exist_ok=True)
    command=['javac','-d',str(output),'-cp',cp,*map(str,sources)]
    p=subprocess.run(command,cwd=EXP,capture_output=True,text=True,errors='replace')
    log.parent.mkdir(parents=True,exist_ok=True)
    log.write_text(p.stderr)
    return p.returncode,p.stderr,dict(command=command,exit_status=p.returncode,stdout=p.stdout,stderr=p.stderr)


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--point',required=True)
    args=parser.parse_args()
    tag=args.point
    point_info(tag)
    root=EXP/'dev'/tag
    if (root/'compile-summary.json').exists():
        raise SystemExit('STOP: compilation already recorded; inspect before overwriting')
    start=time.monotonic()
    helper_sources=[T/r['path'] for r in inventory() if r['role'] in {'helper','integration'}]
    remaining=helper_sources[:]
    failed=[]
    attempts=[]
    while remaining:
        helper_output=root/'helpers'
        if helper_output.exists():
            shutil.rmtree(helper_output)
        code,errors,record=compile_sources(helper_output,classpath(tag,helpers=False),remaining,
                                          root/'javac'/f'helpers-attempt-{len(attempts)+1}.err')
        attempts.append(record)
        if not code:
            break
        named=set(re.findall(r'^(.+\.java):\d+: error:',errors,re.M))
        removed=[p for p in remaining if str(p) in named]
        if not removed:
            write_json(root/'helper-attempts.json',attempts)
            raise SystemExit('STOP: helper javac failure has no identifiable source file; inspect diagnostics')
        failed.extend(removed)
        remaining=[p for p in remaining if p not in removed]
    root.mkdir(parents=True,exist_ok=True)
    (root/'helpers-failed.txt').write_text(''.join(str(p.relative_to(T))+'\n' for p in failed))
    write_json(root/'helper-attempts.json',attempts)
    if not remaining:
        raise SystemExit('STOP: no helper files compiled')
    statuses=[]
    cross_test=[]
    compile_methods=[]
    tests=test_classes()
    names={r['class'] for r in tests}
    for row in tests:
        name=fqcn(row)
        code,errors,record=compile_sources(root/'classes'/name,classpath(tag),[T/row['path']],root/'javac'/f'{name}.err')
        status=dict(test_class=name,path=row['path'],exit_status=code,status='compiled' if not code else 'compile-fail')
        statuses.append(status)
        write_json(root/'javac'/f'{name}.command.json',record)
        for other in names-{row['class']}:
            if re.search(r'\bsymbol:\s+class\s+'+re.escape(other)+r'\b',errors):
                cross_test.append(dict(test_class=name,referenced_test_class=other))
        if code and tag!=BASELINE:
            baseline=PREV/'dev'/FROZEN_TAG/'run1'/f'{name}.summary.json'
            for method in json.loads(baseline.read_text())['listed_methods']:
                compile_methods.append(dict(test_class=name,method=method,status='compile-fail'))
        print(json.dumps(status),flush=True)
    (root/'compile-method-status.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in compile_methods))
    summary=dict(tag=tag,helpers_failed=[str(p.relative_to(T)) for p in failed],
                 helper_attempts=len(attempts),files_compiled=sum(r['exit_status']==0 for r in statuses),
                 files_compile_fail=sum(r['exit_status']!=0 for r in statuses),
                 cross_test_references=cross_test,files=statuses,seconds=round(time.monotonic()-start,3))
    write_json(root/'compile-summary.json',summary)
    if tag==BASELINE:
        write_json(EXP/'results/dev-t-compile.json',summary)
    if cross_test:
        raise SystemExit('STOP: javac names another test class; report in handover')
    if tag==BASELINE and (summary['files_compile_fail'] or failed):
        raise SystemExit('STOP: baseline compile failure')


if __name__=='__main__':
    main()
