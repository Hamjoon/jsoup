#!/usr/bin/env python3
"""Fixed-denominator cumulative commit survival, first breaks, and restorations."""
from collections import Counter
import csv
import json
import time
from dev_common import EXP, write_json
from csv_utils import write_csv

GROUPS=['dev','pooled','ZSL','FSL','CoT','ToT','GToT']
STATUSES=['pass','fail','error','timeout','compile-fail','absent','not-run','not-evaluated']


def read(name):
    with (EXP/'results'/name).open() as stream:
        return list(csv.DictReader(stream))


def identity(group): return ['test_class','method'] if group=='dev' else ['class','technique','test_class','method']


def key(row,group): return tuple(row[k] for k in identity(group))


def members(group,rows):
    if group=='dev': return rows
    field='in_denominator_pooled' if group=='pooled' else 'in_denominator_technique'
    return [r for r in rows if r[field]=='1' and (group=='pooled' or r['technique']==group)]


def analyze(group,commits,builds):
    methods=members(group,read('dev-matrix.csv' if group=='dev' else 'llm-matrix.csv'))
    denominator_rows=[r for r in read('dev-denominator.csv') if r['in_denominator']=='1'] if group=='dev' else members(group,read('llm-methods.csv'))
    assert {key(r,group) for r in methods}=={key(r,group) for r in denominator_rows}
    assert len(methods)==len({key(r,group) for r in methods})
    expected=json.loads((EXP/'results/setup.json').read_text())['denominators'][group]
    assert len(methods)==expected
    live={key(r,group) for r in methods}; first={}; rows=[]
    for c in commits:
        index=int(c['index']); point=c['point']; failed=int(builds[point]['build_fail'])
        counts=Counter(r[point] for r in methods) if index else Counter({'pass':expected})
        assert set(counts)<=set(STATUSES) and sum(counts.values())==expected
        before=len(live)
        if index and failed:
            assert counts['not-evaluated']==expected
        elif index:
            assert not counts['not-evaluated']
            for method in methods:
                k=key(method,group); status=method[point]
                if status!='pass':
                    live.discard(k)
                    first.setdefault(k,dict(first_break_index=index,first_break_point=point,first_break_status=status))
        record=dict(index=index,point=point,sha7=c['sha'][:7],date=c['date'],days_since_t=c['days_since_t'],tag=c['tag'],
                    build_fail=failed,denominator=expected,point_pass=counts['pass'],survived=len(live),
                    survival_rate=f'{len(live)/expected:.4f}',deaths=before-len(live),
                    **{s.replace('-','_'):counts[s] for s in STATUSES if s!='pass'})
        assert record['point_pass']+sum(record[s.replace('-','_')] for s in STATUSES if s!='pass')==expected
        assert not rows or record['survived']<=rows[-1]['survived']
        rows.append(record)
    breaks=[dict(**{k:m[k] for k in identity(group)},**first.get(key(m,group),dict(first_break_index='',first_break_point='',first_break_status=''))) for m in methods]
    totals=Counter(r['first_break_status'] or 'survived' for r in breaks)
    summary=[dict(scope='status',first_break_index='',first_break_status=s,count=totals[s]) for s in [*STATUSES[1:],'survived']]
    per_index=Counter((r['first_break_index'],r['first_break_status']) for r in breaks if r['first_break_index']!='')
    summary += [dict(scope='index',first_break_index=i,first_break_status=s,count=n) for (i,s),n in sorted(per_index.items())]
    transient=[]
    evaluated=[c for c in commits[1:] if builds[c['point']]['build_fail']=='0']
    for method in methods:
        broken=None
        for c in evaluated:
            status=method[c['point']]
            if broken is None and status!='pass': broken=(c,status)
            elif broken is not None and status=='pass':
                b,s=broken
                transient.append(dict(**{k:method[k] for k in identity(group)},i=int(b['index']),point_i=b['point'],status_i=s,j=int(c['index']),point_j=c['point']))
                break
    assert sum(totals.values())==expected
    assert sum(r['deaths'] for r in rows)==len(first)==expected-rows[-1]['survived']
    write_csv(EXP/'results'/f'{group}-survival.csv',rows,list(rows[0]))
    write_csv(EXP/'results'/f'{group}-first-break.csv',breaks,[*identity(group),'first_break_index','first_break_point','first_break_status'])
    write_csv(EXP/'results'/f'{group}-first-break-summary.csv',summary,['scope','first_break_index','first_break_status','count'])
    write_csv(EXP/'results'/f'{group}-transient.csv',transient,[*identity(group),'i','point_i','status_i','j','point_j'])
    result=dict(group=group,denominator=expected,final_survived=rows[-1]['survived'],final_point_pass=rows[-1]['point_pass'],
                first_break_counts=dict(totals),transient_methods=len(transient),not_run_cells=sum(r['not_run'] for r in rows))
    write_json(EXP/'results'/f'{group}-survival-summary.json',result)
    return result


def consistency(commits,builds):
    old={t:{key(r,t):r for r in read('prev-'+t+'-matrix.csv')} for t in ['dev','llm']}
    current={t:read(t+'-matrix.csv') for t in ['dev','llm']}
    details=[]; comparisons=[]; unique={'tag':set(),'same-tree':set()}
    for group in GROUPS:
        track='dev' if group=='dev' else 'llm'; methods=members(group,current[track])
        for i,c in enumerate(commits[1:],1):
            for kind,reference in [('tag',c['tag']),('same-tree',commits[i-1]['point'])]:
                if kind=='tag' and not c['tag']: continue
                if kind=='same-tree' and (c['same_main_as_prev']!='1' or builds[reference]['build_fail']=='1'): continue
                differences=[]
                for m in methods:
                    k=key(m,group); actual=m[c['point']]
                    expected=old[track].get(k,{}).get(reference,'MISSING') if kind=='tag' else ('pass' if i==1 else m[reference])
                    if actual!=expected:
                        row=dict(kind=kind,group=group,index=c['index'],point=c['point'],reference=reference,
                                 **{f:m[f] for f in identity(group)},expected=expected,actual=actual)
                        differences.append(row); unique[kind].add((track,c['point'],k))
                comparisons.append(dict(kind=kind,group=group,index=c['index'],point=c['point'],reference=reference,differing_cells=len(differences)))
                details.extend(differences[:20])
    write_csv(EXP/'results/consistency-counts.csv',comparisons,list(comparisons[0]))
    write_json(EXP/'results/consistency-examples.json',details)
    summary=dict(tag_differing_cells=len(unique['tag']),same_tree_differing_cells=len(unique['same-tree']),
                 definition='Unique track/point/method cells; technique and pooled overlaps counted once.',comparisons=comparisons)
    write_json(EXP/'results/consistency-summary.json',summary)
    md=['# Consistency checks','',f"Unique differing cells: tags {summary['tag_differing_cells']}; same-tree points {summary['same_tree_differing_cells']}.",
        '', '| Check | Group | Index | Point | Reference | Differing cells |','|---|---|---:|---|---|---:|']
    md += ['| '+' | '.join(str(r[k]) for k in ['kind','group','index','point','reference','differing_cells'])+' |' for r in comparisons]
    md += ['', 'Examples (up to twenty per comparison):','', '```json',json.dumps(details,indent=2),'```']
    (EXP/'results/consistency.md').write_text('\n'.join(md)+'\n')
    return summary


def main():
    start=time.monotonic(); commits=read('commits.csv'); builds={r['point']:r for r in read('builds.csv')}
    summaries=[analyze(g,commits,builds) for g in GROUPS]
    checks=consistency(commits,builds)
    write_json(EXP/'results/analysis-stage.json',dict(seconds=round(time.monotonic()-start,3),groups=summaries,
               tag_differing_cells=checks['tag_differing_cells'],same_tree_differing_cells=checks['same_tree_differing_cells']))
    print(json.dumps(summaries,indent=2))


if __name__=='__main__': main()
