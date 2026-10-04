#!/usr/bin/env python3
"""Render comparison tables and seeded matched-size survival from result CSVs."""
from collections import Counter
import json
import random
import statistics
import time
from dev_common import EXP, write_json
from dev_later import write_csv
from handover_a import table
from survival_common import GROUPS, read_csv, releases, matrix, key

LABELS=dict(dev='Dev',pooled='LLM pooled',ZSL='ZSL',FSL='FSL',CoT='CoT',ToT='ToT',GToT='GToT')


def render_table2():
    """Render Table 2 solely from existing first-break and attribution records."""
    attribution={r['group']:r for r in read_csv('compile-attribution-summary.csv') if r['scope']=='group'}
    statuses=['survived','compile-fail','fail','error','timeout','absent']
    rows=[]
    split=[]
    for group in [g for g in GROUPS if g!='pooled']:
        counts=Counter(r['first_break_status'] or 'survived' for r in read_csv(f'{group}-first-break.csv'))
        denominator=int(read_csv(f'{group}-survival.csv')[-1]['denominator'])
        assert sum(counts[s] for s in statuses)==denominator, 'Table 2 omits a nonzero status: '+group
        removed=len({key(r,group) for r in read_csv(f'{group}-compile-fail-cut-removed.csv')})
        assert removed<=counts['compile-fail']
        rows.append(dict(removed=removed,group=LABELS[group],denominator=denominator,**{s:counts[s] for s in statuses}))
        split.append(dict(group=LABELS[group],compile_fail=counts['compile-fail'],own=attribution[group]['own'],sibling=attribution[group]['sibling']))
    text=table(rows,[('Group','group'),('Denominator','denominator'),*[(s,s) for s in statuses[:2]],('compile-fail (removed CUT)','removed'),*[(s,s) for s in statuses[2:]]])
    text+='\nCompile-fail attribution (subset of the compile-fail column):\n\n'+table(split,[('Group','group'),('Compile fail','compile_fail'),('Own','own'),('Sibling','sibling')])
    (EXP/'results/table2-first-break.md').write_text(text)
    write_csv(EXP/'results/table2-first-break.csv',rows,['group','denominator',*statuses[:2],'removed',*statuses[2:]])


def main():
    start=time.monotonic()
    versions=releases()
    minor=[r for r in versions if r['last_of_minor']=='1']
    assert len(minor)==13
    curves={g:{r['tag']:r for r in read_csv(f'{g}-survival.csv')} for g in GROUPS}
    lenient={g:{r['tag']:r for r in read_csv(f'{g}-survival-lenient.csv')} for g in GROUPS}
    for filename,data in [('table1-survival-by-minor.md',curves),('table1-lenient-by-minor.md',lenient)]:
        rows=[]
        for release in minor:
            tag=release['tag']
            rows.append(dict(tag=tag,days=release['days_since_t'],**{g:data[g][tag]['survived']+'/'+data[g][tag]['denominator'] for g in GROUPS}))
        (EXP/'results'/filename).write_text(table(rows,[('Release','tag'),('Days','days'),*[(LABELS[g],g) for g in GROUPS]]))
    render_table2()
    pool=matrix('pooled')
    size=int(curves['dev'][versions[-1]['tag']]['denominator'])
    assert len(pool)>=size
    seed=20261004
    rng=random.Random(seed)
    samples=100
    matched={r['tag']:[] for r in minor}
    for _ in range(samples):
        selected=rng.sample(range(len(pool)),size)
        live=set(selected)
        for release in versions:
            tag=release['tag']
            live={i for i in live if pool[i][tag]=='pass'}
            if tag in matched:
                matched[tag].append(len(live))
    matched_rows=[dict(tag=r['tag'],days_since_t=r['days_since_t'],denominator=size,samples=samples,seed=seed,
        mean_survived=f'{statistics.mean(matched[r["tag"]]):.3f}',min_survived=min(matched[r['tag']]),max_survived=max(matched[r['tag']]),
        mean_survival_rate=f'{statistics.mean(matched[r["tag"]])/size:.6f}') for r in minor]
    write_csv(EXP/'results/llm-matched-594.csv',matched_rows,list(matched_rows[0]))
    dev=read_csv('dev-by-class.csv')
    cuts=[r for r in read_csv('cut-classes.csv') if not r['excluded']]
    class_rows=[]
    for cut in cuts:
        members=[r for r in pool if r['class']==cut['class']]
        dev_members=[r for r in dev if r['production_class']==cut['package']+'.'+cut['class']]
        record=dict(**{'class':cut['class']},package=cut['package'],llm_denominator=len(members),
            dev_test_classes='|'.join(r['test_class'] for r in dev_members),dev_denominator=sum(int(r['denominator']) for r in dev_members))
        live=list(members)
        for release in versions:
            tag=release['tag']
            live=[r for r in live if r[tag]=='pass']
            if release['last_of_minor']=='1':
                record[tag+'_llm']=len(live)
                record[tag+'_dev']=sum(int(r[tag]) for r in dev_members)
        class_rows.append(record)
    write_csv(EXP/'results/llm-by-class.csv',class_rows,list(class_rows[0]))
    display=[dict(CUT=r['package']+'.'+r['class'],LLM=r['llm_denominator'],Dev=r['dev_denominator'],
        **{v['tag']:str(r[v['tag']+'_llm'])+'/'+str(r[v['tag']+'_dev']) for v in minor}) for r in class_rows]
    (EXP/'results/llm-by-class.md').write_text('Release cells are LLM pooled survivors / same-name developer survivors.\n\n'+
        table(display,[('CUT','CUT'),('LLM denominator','LLM'),('Dev denominator','Dev'),*[(r['tag'],r['tag']) for r in minor]]))
    assert sum(r['llm_denominator'] for r in class_rows)==len(pool)
    for release in minor:
        tag=release['tag']
        assert sum(r[tag+'_llm'] for r in class_rows)==int(curves['pooled'][tag]['survived'])
    write_json(EXP/'results/comparison-summary.json',dict(groups=GROUPS,minor_rows=len(minor),matched_samples=samples,matched_seed=seed,
        matched_size=size,matched_final=matched_rows[-1],seconds=round(time.monotonic()-start,3),all_table_assertions_passed=True))
    print('Comparison tables, class figures, and seeded matched-size samples written.')


if __name__=='__main__':
    main()
