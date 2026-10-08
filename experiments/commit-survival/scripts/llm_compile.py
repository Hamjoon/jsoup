#!/usr/bin/env python3
"""Compile each unchanged extracted file separately with the LLM classpath."""
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
import argparse
import csv
import json
import re
import subprocess
import time
from dev_common import BASELINE, EXP, write_json
from csv_utils import write_csv
from llm_common import files,source,classpath,error_category



def compile_file(tag,row,presence):
    name,tech=row['class'],row['technique']
    root=EXP/'llm'/tag
    if tag!=BASELINE and presence[name][tag]=='0':
        return dict(**{'class':name},technique=tech,exit='',error_count=0,status='absent',file=row['file']),[]
    output=root/'classes'/name/tech
    output.mkdir(parents=True,exist_ok=True)
    command=['javac','-d',str(output),'-cp',classpath(tag,name,tech),str(source(row))]
    p=subprocess.run(command,cwd=EXP,capture_output=True,text=True,errors='replace')
    err=root/'javac'/f'{name}.{tech}.err'
    err.parent.mkdir(exist_ok=True)
    err.write_text(p.stderr)
    write_json(err.with_suffix('.command.json'),dict(command=command,exit_status=p.returncode,stdout=p.stdout,stderr=p.stderr))
    errors=[]
    for line in p.stderr.splitlines():
        match=re.match(r'^(.+\.java):(\d+): error: (.*)$',line)
        if match:
            errors.append(dict(**{'class':name},technique=tech,file=row['file'],line=match.group(2),
                category=error_category(match.group(3)),message=match.group(3),error_line=line))
    record=dict(**{'class':name},technique=tech,exit=p.returncode,error_count=len(errors),
                status='compiled' if p.returncode==0 else 'compile-fail',file=row['file'])
    return record,errors

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--point',required=True)
    parser.add_argument('--add-new',action='store_true',help='Retain existing per-file results and compile only new extracted files')
    parser.add_argument('--workers',type=int,choices=range(1,5),default=1)
    args=parser.parse_args()
    tag=args.point
    root=EXP/'llm'/tag
    if (root/'compile-summary.json').exists() and not args.add_new:
        raise SystemExit('STOP: compile summary already exists; inspect before overwriting')
    start=time.monotonic()
    rows=[]
    diagnostics=[]
    previous=None
    if args.add_new:
        assert tag==BASELINE
        previous=json.loads((root/'compile-summary.json').read_text())
        rows.extend(previous['files_ledger'])
        diagnostics.extend(csv.DictReader((EXP/'results/llm-compile-t-errors.csv').open()))
    known={(r['class'],r['technique']) for r in rows}
    presence={r['class']:{tag:'1' if (EXP/'checkouts'/tag/r['path']).is_file() else '0'}
              for r in csv.DictReader((EXP/'results/cut-classes.csv').open()) if not r['excluded']}
    pending=[]
    for row in files():
        if (row['class'],row['technique']) in known:
            source(row)
        else:
            pending.append(row)
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        for record,errors in executor.map(lambda row:compile_file(tag,row,presence),pending):
            rows.append(record)
            diagnostics.extend(errors)
            print(record['class']+'.'+record['technique']+': '+record['status'],flush=True)
    summary=dict(tag=tag,workers=args.workers,files=len(rows),compiled=sum(r['status']=='compiled' for r in rows),
        compile_fail=sum(r['status']=='compile-fail' for r in rows),absent=sum(r['status']=='absent' for r in rows),
        error_categories=dict(Counter(r['category'] for r in diagnostics)),seconds=round(time.monotonic()-start,3),files_ledger=rows)
    if previous:
        summary['incremental_seconds']=summary['seconds']
        summary['seconds']+=previous['seconds']
        summary['new_files']=len(rows)-previous['files']
    write_json(root/'compile-summary.json',summary)
    if tag==BASELINE:
        write_csv(EXP/'results/llm-compile-t.csv',rows,['class','technique','exit','error_count','status','file'])
        write_csv(EXP/'results/llm-compile-t-errors.csv',diagnostics,['class','technique','file','line','category','message','error_line'])
        write_json(EXP/'results/llm-compile-t-summary.json',summary)
        if not rows or summary['compiled']<len(rows)/3:
            raise SystemExit('STOP: fewer than one third of extracted files compile at baseline')


if __name__=='__main__':
    main()
