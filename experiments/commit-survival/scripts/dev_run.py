#!/usr/bin/env python3
"""Run compiled test classes through the unchanged per-method wrapper."""
import argparse
from collections import Counter
import json
import subprocess
import sys
import time
import javalang
from dev_common import BASELINE, EXP, PREV, T, classpath, write_json


def record_ignored_class(name,policy,tag,output):
    """Inventory the explicitly authorized ignored class; never launch a JVM."""
    start=time.monotonic()
    source=T/policy['source']
    tree=javalang.parse.parse(source.read_text())
    cls=next(c for c in tree.types if c.name==name.rsplit('.',1)[-1])
    assert any(a.name in {'Ignore','org.junit.Ignore'} for a in cls.annotations)
    methods=[m.name for m in cls.methods if any(a.name in {'Test','org.junit.Test'} for a in m.annotations)]
    assert methods==policy['methods'], 'Authorized ignored-method inventory changed'
    measured=[dict(method=m,status='ignored',exception=None,
                   message='Class-level @Ignore; recorded by orchestration without execution',
                   milliseconds=None,launch=0) for m in methods]
    output.write_text(''.join(json.dumps(r)+'\n' for r in measured))
    summary=dict(fqcn=name,classpath=classpath(tag,name),cwd=str(EXP/'checkouts'/tag),
                 timeout_ms=30000,listed_methods=methods,all_listed_methods=methods,
                 selected_methods=None,excluded_methods=[],missing_methods=[],
                 listing=None,list_error='',launches=[],methods=measured,
                 counts={'ignored':len(methods)},wall_seconds=round(time.monotonic()-start,3),
                 orchestration_class_ignore=True,inventory_source=str(source),
                 authorization='results/dev-run-policy.json')
    write_json(output.with_suffix('.summary.json'),summary)
    output.with_suffix('.wrapper.log').write_text('Class-level @Ignore: no wrapper or JVM executed.\n')
    return summary


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--point',required=True)
    parser.add_argument('--run',required=True,type=int,choices=[1,2,3])
    args=parser.parse_args()
    tag=args.point
    root=EXP/'dev'/tag
    compiled=json.loads((root/'compile-summary.json').read_text())
    run_dir=root/f'run{args.run}'
    if run_dir.exists():
        raise SystemExit('STOP: run output already exists; inspect before overwriting')
    if tag==BASELINE and args.run>1:
        previous=root/f'run{args.run-1}'/'run-summary.json'
        if not previous.exists() or not json.loads(previous.read_text())['complete']:
            raise SystemExit('STOP: baseline runs must be sequential and complete')
    run_dir.mkdir(parents=True)
    start=time.monotonic()
    counts=Counter()
    methods_run=0
    classes=[]
    ignored_classes=json.loads((PREV/'results/dev-run-policy.json').read_text())['class_level_ignored']
    for file in compiled['files']:
        name=file['test_class']
        output=run_dir/f'{name}.json'
        if name in ignored_classes:
            summary=record_ignored_class(name,ignored_classes[name],tag,output)
        elif file['exit_status']:
            continue
        else:
            command=[sys.executable,str(EXP/'scripts/run_class.py'),name,
                 '--classpath',classpath(tag,name),'--output',str(output),
                 '--timeout-ms','30000','--cwd',str(EXP/'checkouts'/tag)]
            p=subprocess.run(command,cwd=EXP,capture_output=True,text=True,errors='replace')
            (run_dir/f'{name}.wrapper.log').write_text(p.stdout+p.stderr)
            if p.returncode:
                write_json(EXP/'results/dev-run-failure.json',dict(tag=tag,run=args.run,
                    test_class=name,exit_status=p.returncode,output=p.stdout+p.stderr))
                raise SystemExit('STOP: runner wrapper failed: '+name)
            summary=json.loads(output.with_suffix('.summary.json').read_text())
        assert len(summary['methods'])==len(summary['listed_methods'])
        counts.update(summary['counts'])
        methods_run+=len(summary['methods'])
        classes.append(name)
        print(json.dumps(dict(tag=tag,run=args.run,test_class=name,counts=summary['counts'])),flush=True)
    result=dict(tag=tag,run=args.run,complete=True,classes_run=len(classes),methods_run=methods_run,
                counts=dict(counts),seconds=round(time.monotonic()-start,3))
    write_json(run_dir/'run-summary.json',result)
    if tag==BASELINE:
        write_json(EXP/'results'/f'dev-t-run{args.run}.json',result)
    print(json.dumps(result),flush=True)


if __name__=='__main__':
    main()
