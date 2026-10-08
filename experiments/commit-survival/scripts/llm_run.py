#!/usr/bin/env python3
"""Run each compiled generated class using the unchanged per-method runner."""
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
import argparse
import json
import time
from dev_common import BASELINE, EXP, write_json
from dedup_lib import source_tests
from llm_common import files,source,fqcn,classpath
from run_class import run_class



def run_file(tag,row,output,include=None):
    source(row)
    name=fqcn(row)
    try:
        summary=run_class(name,classpath(tag,row['class'],row['technique']),output,
                          timeout_ms=30000,working_directory=EXP/'checkouts'/tag,include_methods=include)
    except RuntimeError:
        path=output.with_suffix('.summary.json')
        if not path.exists():
            raise
        summary=json.loads(path.read_text())
        if not summary['list_error']:
            raise
        tests=source_tests(source(row).read_bytes().decode('utf-8'),row['package'])
        names=sorted({m['method'] for m in tests if m['test_class']==name})
        records=[dict(method=m,status='not-run',exception='JUnitListingFailure',
                      message=summary['list_error']) for m in names]
        write_json(output.with_suffix('.listing-failure.json'),dict(test_class=name,methods=records,
                   raw_wrapper_summary=str(path.relative_to(EXP))))
        return summary,dict(**{'class':row['class']},technique=row['technique'],test_class=name,source_methods=len(names),reason=summary['list_error'])
    return summary,None

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--point',required=True)
    parser.add_argument('--run',required=True,type=int,choices=[1,2,3])
    parser.add_argument('--add-new',action='store_true',help='Run only new compiled files; preserve prior per-file results')
    parser.add_argument('--workers',type=int,choices=range(1,5),default=1)
    args=parser.parse_args()
    tag=args.point
    root=EXP/'llm'/tag
    folder=root/f'run{args.run}'
    if folder.exists() and not args.add_new:
        raise SystemExit('STOP: run output exists; inspect before overwriting')
    if tag==BASELINE and args.run>1:
        assert json.loads((root/f'run{args.run-1}'/'run-summary.json').read_text())['complete']
    folder.mkdir(parents=True,exist_ok=args.add_new)
    start=time.monotonic()
    compiled=json.loads((root/'compile-summary.json').read_text())
    extracted={(r['class'],r['technique']):r for r in files()}
    counts=Counter()
    listing_failures=[]
    measured=0
    listed=0
    classes=0
    previous=None
    if args.add_new:
        assert tag==BASELINE
        previous=json.loads((folder/'run-summary.json').read_text())
        assert previous['complete']
        counts.update(previous['counts'])
        listing_failures.extend(previous['listing_failures'])
        measured=previous['methods_measured']
        listed=previous['methods_listed']
        classes=previous['files_run']
    pending=[]
    for status in compiled['files_ledger']:
        if status['status']!='compiled':
            continue
        row=extracted[(status['class'],status['technique'])]
        source(row)
        output=folder/f"{row['class']}.{row['technique']}.json"
        if args.add_new and output.with_suffix('.summary.json').exists():
            continue
        pending.append((row,output))
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        for (row,output),(summary,failure) in zip(pending,executor.map(lambda task:run_file(tag,*task),pending)):
            if failure:
                listing_failures.append(failure)
                counts['not-run']+=failure['source_methods']
                measured+=failure['source_methods']
            else:
                counts.update(summary['counts'])
                measured+=len(summary['methods'])
                listed+=len(summary['all_listed_methods'])
            classes+=1
            print(json.dumps(dict(**{'class':row['class']},technique=row['technique'],run=args.run,
                counts=summary['counts'],listing_failed=bool(summary['list_error']))),flush=True)
    result=dict(tag=tag,workers=args.workers,run=args.run,complete=True,files_run=classes,methods_measured=measured,
                methods_listed=listed,counts=dict(counts),listing_failures=listing_failures,
                seconds=round(time.monotonic()-start,3))
    if previous:
        result['incremental_seconds']=result['seconds']
        result['seconds']+=previous['seconds']
        result['new_files_run']=classes-previous['files_run']
    write_json(folder/'run-summary.json',result)
    if tag==BASELINE:
        write_json(EXP/'results'/f'llm-t-run{args.run}.json',result)
    print(json.dumps({k:v for k,v in result.items() if k!='listing_failures'},indent=2))


if __name__=='__main__':
    main()
