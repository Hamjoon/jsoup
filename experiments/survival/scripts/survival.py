#!/usr/bin/env python3
"""Calculate fixed-denominator strict survival and first-break diagnostics."""
from collections import Counter
import argparse
import json
import re
import time
from dev_common import EXP, write_json
from dev_later import write_csv
from survival_common import GROUPS, STATUSES, REMOVED, read_csv, releases, matrix, key, identity, javac_path, diagnostics


def analyze(group):
    start=time.monotonic()
    versions=releases()
    tags=[r['tag'] for r in versions]
    methods=matrix(group)
    ids=identity(group)
    live={key(r,group) for r in methods}
    first={}
    survivors={}
    rows=[]
    for release in versions:
        tag=release['tag']
        counts=Counter(r[tag] for r in methods)
        assert set(counts)<=set(STATUSES) and sum(counts.values())==len(methods)
        for method in methods:
            k=key(method,group)
            if method[tag]!='pass':
                live.discard(k)
                first.setdefault(k,dict(first_break_tag=tag,first_break_index=release['index'],
                    first_break_status=method[tag],days_since_t=release['days_since_t']))
        survivors[tag]=live.copy()
        record=dict(tag=tag,date=release['date'],days_since_t=release['days_since_t'],minor=release['minor'],
            last_of_minor=release['last_of_minor'],denominator=len(methods),point_pass=counts['pass'],survived=len(live),
            survival_rate=f'{len(live)/len(methods):.4f}',fail=counts['fail'],error=counts['error'],timeout=counts['timeout'],
            compile_fail=counts['compile-fail'],not_run=counts['not-run'],absent=counts['absent'],ignored=counts['ignored'])
        assert record['point_pass']+sum(record[f] for f in ['fail','error','timeout','compile_fail','not_run','absent','ignored'])==len(methods)
        assert not rows or record['survived']<=rows[-1]['survived']
        rows.append(record)
    write_csv(EXP/'results'/f'{group}-survival.csv',rows,list(rows[0]))
    breaks=[dict(**{k:m[k] for k in ids},**first.get(key(m,group),dict(first_break_tag='',first_break_index='',
        first_break_status='',days_since_t=''))) for m in methods]
    write_csv(EXP/'results'/f'{group}-first-break.csv',breaks,[*ids,'first_break_tag','first_break_index','first_break_status','days_since_t'])
    status_counts=Counter(r['first_break_status'] or 'survived' for r in breaks)
    order=['fail','error','timeout','compile-fail','absent','not-run','ignored','survived']
    summaries=[dict(scope='status',first_break_status=s,first_break_tag='',count=status_counts[s]) for s in order]
    for tag in tags:
        counts=Counter(r['first_break_status'] for r in breaks if r['first_break_tag']==tag)
        summaries.extend(dict(scope='release',first_break_status=s,first_break_tag=tag,count=counts[s]) for s in order if s!='survived')
    assert sum(status_counts.values())==len(methods)
    files={}
    for row in breaks:
        if row['first_break_status']=='compile-fail':
            path=javac_path(group,row['first_break_tag'],row)
            files[(row['first_break_tag'],path)]=row
    errors=[]
    examples={}
    symbols=[]
    for (tag,path),row in sorted(files.items(),key=lambda item:(tags.index(item[0][0]),str(item[0][1]))):
        for error in diagnostics(path):
            errors.append(dict(tag=tag,**{k:row[k] for k in ids if k!='method'},
                **{k:error[k] for k in ['source_file','line','category','message','symbol','error_line']}))
            symbol=error['symbol']
            symbols.append(symbol)
            examples.setdefault(symbol,dict(first_tag=tag,test_class=row['test_class'],diagnostic=error['diagnostic']))
    write_csv(EXP/'results'/f'{group}-compile-errors.csv',errors,
        ['tag',*[k for k in ids if k!='method'],'source_file','line','category','message','symbol','error_line'])
    categories=Counter(r['category'] for r in errors)
    write_csv(EXP/'results'/f'{group}-compile-error-summary.csv',[dict(category=c,count=categories[c]) for c in
        ['CFS','PDNE','PAI','IT','CAM','MAM','DCD','SCI','UE','VNI','other']],['category','count'])
    common=[dict(symbol=s,count=n,**examples[s]) for s,n in Counter(symbols).most_common(4)]
    write_csv(EXP/'results'/f'{group}-javac-common.csv',common,['symbol','count','first_tag','test_class','diagnostic'])
    removed=[]
    if group=='dev':
        for row in breaks:
            if row['first_break_status']!='compile-fail':
                continue
            named='\n'.join(e['named_text'] for e in diagnostics(javac_path(group,row['first_break_tag'],row)))
            for symbol in REMOVED:
                if re.search(r'\b'+re.escape(symbol)+r'\b',named):
                    removed.append(dict(test_class=row['test_class'],method=row['method'],first_break_tag=row['first_break_tag'],symbol=symbol))
        write_csv(EXP/'results/dev-compile-fail-cut-removed.csv',removed,['test_class','method','first_break_tag','symbol'])
        count=len({(r['test_class'],r['method']) for r in removed})
        summaries.append(dict(scope='cut-removed-subcount',first_break_status='compile-fail',first_break_tag='',count=count))
        mapping=read_csv('dev-class-map.csv')
        byclass=[]
        minor=[r['tag'] for r in versions if r['last_of_minor']=='1']
        for row in mapping:
            members={key(m,group) for m in methods if m['test_class']==row['test_class']}
            byclass.append(dict(**row,denominator=len(members),**{tag:len(members&survivors[tag]) for tag in minor}))
        write_csv(EXP/'results/dev-by-class.csv',byclass,['test_class','production_class','denominator',*minor])
    write_csv(EXP/'results'/f'{group}-first-break-summary.csv',summaries,['scope','first_break_status','first_break_tag','count'])
    result=dict(releases=len(tags),minor_lines=sum(r['last_of_minor']=='1' for r in versions),denominator=len(methods),
        final_survived=rows[-1]['survived'],final_survival_rate=rows[-1]['survival_rate'],final_point_pass=rows[-1]['point_pass'],
        first_break_counts=dict(status_counts),compile_failure_files_at_first_break=len(files),javac_error_lines=len(errors),
        seconds=round(time.monotonic()-start,3),assertions_passed=True)
    if group=='dev':
        result['compile_fail_cut_removed_methods']=count
        result['removed_symbol_rows']=len(removed)
    write_json(EXP/'results'/f'{group}-survival-summary.json',result)
    print(json.dumps(dict(group=group,**result)),flush=True)


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--group',choices=[*GROUPS,'all'],default='dev')
    args=parser.parse_args()
    for group in GROUPS if args.group=='all' else [args.group]:
        analyze(group)


if __name__=='__main__':
    main()
