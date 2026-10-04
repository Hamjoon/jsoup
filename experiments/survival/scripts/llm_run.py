#!/usr/bin/env python3
"""Run each compiled generated class using the unchanged per-method runner."""
from collections import Counter
import argparse
import json
import time
from dev_common import BASELINE, EXP, write_json
from dedup_lib import source_tests
from llm_common import files,source,fqcn,classpath
from run_class import run_class


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--release',required=True)
    parser.add_argument('--run',required=True,type=int,choices=[1,2,3])
    args=parser.parse_args()
    tag=args.release
    root=EXP/'llm'/tag
    folder=root/f'run{args.run}'
    if folder.exists():
        raise SystemExit('STOP: run output exists; inspect before overwriting')
    if tag==BASELINE and args.run>1:
        assert json.loads((root/f'run{args.run-1}'/'run-summary.json').read_text())['complete']
    folder.mkdir(parents=True)
    start=time.monotonic()
    compiled=json.loads((root/'compile-summary.json').read_text())
    extracted={(r['class'],r['technique']):r for r in files()}
    counts=Counter()
    listing_failures=[]
    measured=0
    listed=0
    classes=0
    for status in compiled['files_ledger']:
        if status['status']!='compiled':
            continue
        row=extracted[(status['class'],status['technique'])]
        source(row)
        name=fqcn(row)
        output=folder/f"{row['class']}.{row['technique']}.json"
        try:
            summary=run_class(name,classpath(tag,row['class'],row['technique']),output,
                              timeout_ms=30000,working_directory=EXP/'checkouts'/tag)
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
            listing_failures.append(dict(**{'class':row['class']},technique=row['technique'],test_class=name,
                                         source_methods=len(names),reason=summary['list_error']))
            counts['not-run']+=len(names)
            measured+=len(names)
        else:
            counts.update(summary['counts'])
            measured+=len(summary['methods'])
            listed+=len(summary['all_listed_methods'])
        classes+=1
        print(json.dumps(dict(**{'class':row['class']},technique=row['technique'],run=args.run,
                              counts=summary['counts'],listing_failed=bool(summary['list_error']))),flush=True)
    result=dict(tag=tag,run=args.run,complete=True,files_run=classes,methods_measured=measured,
                methods_listed=listed,counts=dict(counts),listing_failures=listing_failures,
                seconds=round(time.monotonic()-start,3))
    write_json(folder/'run-summary.json',result)
    if tag==BASELINE:
        write_json(EXP/'results'/f'llm-t-run{args.run}.json',result)
    print(json.dumps({k:v for k,v in result.items() if k!='listing_failures'},indent=2))


if __name__=='__main__':
    main()
