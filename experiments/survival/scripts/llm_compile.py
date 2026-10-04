#!/usr/bin/env python3
"""Compile each unchanged extracted file separately with the LLM classpath."""
from collections import Counter
import argparse
import csv
import re
import subprocess
import time
from dev_common import BASELINE, EXP, write_json
from dev_later import write_csv
from llm_common import files,source,classpath,error_category


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--release',required=True)
    args=parser.parse_args()
    tag=args.release
    root=EXP/'llm'/tag
    if (root/'compile-summary.json').exists():
        raise SystemExit('STOP: compile summary already exists; inspect before overwriting')
    start=time.monotonic()
    rows=[]
    diagnostics=[]
    presence={r['class']:r for r in csv.DictReader((EXP/'results/cut-presence.csv').open())}
    for row in files():
        name,tech=row['class'],row['technique']
        if tag!=BASELINE and presence[name][tag]=='0':
            rows.append(dict(**{'class':name},technique=tech,exit='',error_count=0,status='absent',file=row['file']))
            continue
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
        diagnostics.extend(errors)
        record=dict(**{'class':name},technique=tech,exit=p.returncode,error_count=len(errors),
                    status='compiled' if p.returncode==0 else 'compile-fail',file=row['file'])
        rows.append(record)
        print(str(name)+'.'+tech+': '+record['status'],flush=True)
    summary=dict(tag=tag,files=len(rows),compiled=sum(r['status']=='compiled' for r in rows),
        compile_fail=sum(r['status']=='compile-fail' for r in rows),absent=sum(r['status']=='absent' for r in rows),
        error_categories=dict(Counter(r['category'] for r in diagnostics)),seconds=round(time.monotonic()-start,3),files_ledger=rows)
    write_json(root/'compile-summary.json',summary)
    if tag==BASELINE:
        write_csv(EXP/'results/llm-compile-t.csv',rows,['class','technique','exit','error_count','status','file'])
        write_csv(EXP/'results/llm-compile-t-errors.csv',diagnostics,['class','technique','file','line','category','message','error_line'])
        write_json(EXP/'results/llm-compile-t-summary.json',summary)
        if not rows or summary['compiled']<len(rows)/3:
            raise SystemExit('STOP: fewer than one third of extracted files compile at baseline')


if __name__=='__main__':
    main()
