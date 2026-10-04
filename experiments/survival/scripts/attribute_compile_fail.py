#!/usr/bin/env python3
"""Attribute file-level compilation breaks to method bodies and derive lenient curves."""
from collections import Counter
import json
import time
from dev_common import BASELINE, EXP, write_json
from dev_later import write_csv
from dedup_lib import source_tests
from survival_common import GROUPS, read_csv, releases, matrix, key, identity, javac_path, diagnostics


def body_ranges():
    llm={key(r,'pooled'):r for r in read_csv('llm-methods.csv')}
    dev={}
    for file in read_csv('dev-files.csv'):
        if file['role']!='test':
            continue
        path=EXP/'checkouts'/BASELINE/file['path']
        text=path.read_bytes().decode('utf-8')
        for method in source_tests(text,file['package']):
            k=(method['test_class'],method['method'])
            assert k not in dev
            dev[k]=dict(opening_line=method['opening_line'],closing_line=text.count('\n',0,method['closing_offset'])+1,
                file=file['path'],body_hash=method['body_hash'])
    return dev,llm


def main():
    start=time.monotonic()
    dev,llm=body_ranges()
    summary=[]
    versions=releases()
    for group in GROUPS:
        methods=matrix(group)
        lookup=dev if group=='dev' else llm
        ids=identity(group)
        first={key(r,group):r for r in read_csv(f'{group}-first-break.csv') if r['first_break_status']=='compile-fail'}
        strict={r['tag']:r for r in read_csv(f'{group}-survival.csv')}
        events=[]
        first_events=[]
        live={key(r,group) for r in methods}
        curve=[]
        for release in versions:
            tag=release['tag']
            point=Counter()
            sibling=0
            for row in methods:
                k=key(row,group)
                status=row[tag]
                if status=='compile-fail':
                    body=lookup[k]
                    low,high=int(body['opening_line']),int(body['closing_line'])
                    lines=[int(e['line']) for e in diagnostics(javac_path(group,tag,row))]
                    own=[line for line in lines if low<=line<=high]
                    attribution='own' if own else 'sibling'
                    event=dict(**{f:row[f] for f in ids},tag=tag,opening_line=low,closing_line=high,
                        attribution=attribution,error_lines='|'.join(map(str,lines)),own_error_lines='|'.join(map(str,own)))
                    events.append(event)
                    if k in first and first[k]['first_break_tag']==tag:
                        first_events.append(dict(**event,first_break_tag=tag))
                    if attribution=='sibling':
                        status='pass'
                        sibling+=1
                point[status]+=1
                if status!='pass':
                    live.discard(k)
            record=dict(strict[tag])
            record.update(survived=len(live),survival_rate=f'{len(live)/len(methods):.4f}',point_pass=point['pass'],
                fail=point['fail'],error=point['error'],timeout=point['timeout'],compile_fail=point['compile-fail'],
                absent=point['absent'],not_run=point['not-run'],ignored=point['ignored'],sibling_treated_pass=sibling)
            assert sum(point.values())==len(methods)
            assert len(live)>=int(strict[tag]['survived'])
            assert not curve or len(live)<=curve[-1]['survived']
            curve.append(record)
        assert len(first_events)==len(first)
        fields=[*ids,'tag','opening_line','closing_line','attribution','error_lines','own_error_lines']
        write_csv(EXP/'results'/f'{group}-compile-attribution.csv',first_events,[*fields,'first_break_tag'])
        write_csv(EXP/'results'/f'{group}-compile-attribution-all.csv',events,fields)
        write_csv(EXP/'results'/f'{group}-survival-lenient.csv',curve,list(curve[0]))
        counts=Counter(r['attribution'] for r in first_events)
        group_summary=[dict(scope='group',group=group,first_break_tag='',own=counts['own'],sibling=counts['sibling'])]
        for release in versions:
            tag=release['tag']
            counts=Counter(r['attribution'] for r in first_events if r['first_break_tag']==tag)
            group_summary.append(dict(scope='release',group=group,first_break_tag=tag,own=counts['own'],sibling=counts['sibling']))
        summary.extend(group_summary)
        write_csv(EXP/'results'/f'{group}-compile-attribution-summary.csv',group_summary,['scope','group','first_break_tag','own','sibling'])
    write_csv(EXP/'results/compile-attribution-summary.csv',summary,['scope','group','first_break_tag','own','sibling'])
    write_json(EXP/'results/compile-attribution-timing.json',dict(seconds=round(time.monotonic()-start,3),
        all_release_compile_fail_events_attributed=True,lenient_nonincreasing=True,lenient_never_below_strict=True))
    print('Attributed first breaks and all release compile-fail events; lenient assertions passed.')


if __name__=='__main__':
    main()
