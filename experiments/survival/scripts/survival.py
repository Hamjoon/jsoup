#!/usr/bin/env python3
"""Calculate cumulative survival, point status, and first-break diagnostics."""
from collections import Counter, defaultdict
import csv
import json
import re
import time
from dev_common import EXP, write_json
from dev_later import write_csv


def read_csv(name):
    return list(csv.DictReader((EXP/'results'/name).open()))


def category(message):
    patterns=[('CFS',r'cannot find symbol'),('PDNE',r'package .* does not exist'),
              ('PAI',r'has private access'),('IT',r'incompatible types'),
              ('CAM',r'constructor .* cannot be applied'),('MAM',r'method .* cannot be applied'),
              ('SCI',r'non-static .* cannot be referenced from a static context'),
              ('UE',r'unreported exception')]
    return next((c for c,p in patterns if re.search(p,message)),'other')


def main():
    start=time.monotonic()
    releases=[r for r in read_csv('releases.csv') if int(r['index'])>0]
    tags=[r['tag'] for r in releases]
    matrix=read_csv('dev-matrix.csv')
    baseline=[r for r in read_csv('dev-denominator.csv') if r['in_denominator']=='1']
    keys={(r['test_class'],r['method']) for r in baseline}
    assert len(keys)==len(baseline)==len(matrix)
    assert {(r['test_class'],r['method']) for r in matrix}==keys
    with (EXP/'results/dev-matrix.csv').open() as stream:
        assert csv.DictReader(stream).fieldnames==['test_class','method',*tags]
    allowed={'pass','fail','error','timeout','compile-fail','not-run','not-evaluated'}
    assert all(row[tag] in allowed for row in matrix for tag in tags)
    live=set(keys)
    first={}
    survivors={}
    rows=[]
    for release in releases:
        tag=release['tag']
        counts=Counter(row[tag] for row in matrix)
        assert sum(counts.values())==len(keys)
        for row in matrix:
            key=(row['test_class'],row['method'])
            if row[tag] not in {'pass','not-evaluated'}:
                live.discard(key)
                first.setdefault(key,dict(first_break_tag=tag,first_break_index=release['index'],
                                          first_break_status=row[tag],days_since_t=release['days_since_t']))
        survivors[tag]=live.copy()
        row=dict(tag=tag,date=release['date'],days_since_t=release['days_since_t'],minor=release['minor'],
                 last_of_minor=release['last_of_minor'],denominator=len(keys),point_pass=counts['pass'],
                 survived=len(live),survival_rate=f'{len(live)/len(keys):.4f}',fail=counts['fail'],
                 error=counts['error'],timeout=counts['timeout'],compile_fail=counts['compile-fail'],
                 not_run=counts['not-run'])
        if counts['not-evaluated']:
            raise SystemExit('STOP: not-evaluated status despite accepted successful builds')
        assert row['point_pass']+sum(row[k] for k in ['fail','error','timeout','compile_fail','not_run'])==len(keys)
        assert not rows or row['survived']<=rows[-1]['survived']
        rows.append(row)
    write_csv(EXP/'results/dev-survival.csv',rows,list(rows[0]))
    breaks=[dict(test_class=r['test_class'],method=r['method'],**first.get((r['test_class'],r['method']),
             dict(first_break_tag='',first_break_index='',first_break_status='',days_since_t=''))) for r in matrix]
    write_csv(EXP/'results/dev-first-break.csv',breaks,
              ['test_class','method','first_break_tag','first_break_index','first_break_status','days_since_t'])
    status_counts=Counter(r['first_break_status'] or 'survived' for r in breaks)
    status_order=['fail','error','timeout','compile-fail','not-run','survived']
    summaries=[dict(scope='status',first_break_status=s,first_break_tag='',count=status_counts[s]) for s in status_order]
    for tag in tags:
        counts=Counter(r['first_break_status'] for r in breaks if r['first_break_tag']==tag)
        summaries.extend(dict(scope='release',first_break_status=s,first_break_tag=tag,count=counts[s]) for s in status_order if s!='survived')
    assert sum(status_counts.values())==len(keys)
    write_csv(EXP/'results/dev-first-break-summary.csv',summaries,['scope','first_break_status','first_break_tag','count'])
    compile_files={(r['first_break_tag'],r['test_class']) for r in breaks if r['first_break_status']=='compile-fail'}
    diagnostics=[]
    examples={}
    message_keys=[]
    for tag,name in sorted(compile_files,key=lambda pair:(tags.index(pair[0]),pair[1])):
        path=EXP/'dev'/tag/'javac'/f'{name}.err'
        lines=path.read_text().splitlines()
        found=0
        for i,line in enumerate(lines):
            match=re.match(r'^(.+\.java):(\d+): error: (.*)$',line)
            if not match:
                continue
            found+=1
            message=match.group(3)
            diagnostics.append(dict(tag=tag,test_class=name,source_file=match.group(1),
                                    line=match.group(2),category=category(message),message=message,error_line=line))
            end=i+1
            while end<len(lines) and not re.match(r'^.+\.java:\d+: error:|^\d+ errors?$|^Note:',lines[end]):
                end+=1
            # Include javac's verbatim symbol detail so different missing APIs
            # are counted separately from the generic "cannot find symbol" header.
            symbol=next((s for s in lines[i+1:end] if re.match(r'^\s*symbol:',s)),None)
            message_key=message+('\n'+symbol if symbol else '')
            message_keys.append(message_key)
            examples.setdefault(message_key,dict(first_tag=tag,test_class=name,diagnostic='\n'.join(lines[i:end])))
        assert found, 'No javac error lines at first compile break: '+str(path)
    write_csv(EXP/'results/dev-compile-errors.csv',diagnostics,
              ['tag','test_class','source_file','line','category','message','error_line'])
    categories=Counter(r['category'] for r in diagnostics)
    write_csv(EXP/'results/dev-compile-error-summary.csv',[dict(category=c,count=categories[c]) for c in
              ['CFS','PDNE','PAI','IT','CAM','MAM','SCI','UE','other']],['category','count'])
    messages=Counter(message_keys)
    common=[dict(message=m,count=n,**examples[m]) for m,n in messages.most_common(4)]
    write_csv(EXP/'results/dev-javac-common.csv',common,['message','count','first_tag','test_class','diagnostic'])
    minor_tags=[r['tag'] for r in releases if r['last_of_minor']=='1']
    mappings=read_csv('dev-class-map.csv')
    byclass=[]
    for mapping in mappings:
        name=mapping['test_class']
        class_keys={key for key in keys if key[0]==name}
        byclass.append(dict(test_class=name,production_class=mapping['production_class'],denominator=len(class_keys),
                            **{tag:len(class_keys & survivors[tag]) for tag in minor_tags}))
    assert sum(r['denominator'] for r in byclass)==len(keys)
    for tag in minor_tags:
        assert sum(r[tag] for r in byclass)==len(survivors[tag])
    write_csv(EXP/'results/dev-by-class.csv',byclass,['test_class','production_class','denominator',*minor_tags])
    write_json(EXP/'results/dev-survival-summary.json',dict(releases=len(tags),minor_lines=len(minor_tags),
        denominator=len(keys),final_survived=rows[-1]['survived'],final_survival_rate=rows[-1]['survival_rate'],
        final_point_pass=rows[-1]['point_pass'],first_break_counts=dict(status_counts),
        compile_failure_files_at_first_break=len(compile_files),javac_error_lines=len(diagnostics),
        seconds=round(time.monotonic()-start,3),assertions_passed=True))
    print(json.dumps(rows[-1],indent=2))


if __name__=='__main__':
    main()
