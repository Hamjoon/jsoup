#!/usr/bin/env python3
"""Render event tables and denominator funnels from measured result files."""
from collections import Counter
import json
import time
from csv_utils import write_csv
from dev_common import EXP, write_json
from survival import GROUPS, read

ORDER=['dev','ZSL','FSL','CoT','ToT','GToT','pooled']
LABEL={'dev':'Dev','pooled':'LLM all','ZSL':'ZSL','FSL':'FSL','CoT':'CoT','ToT':'ToT','GToT':'GToT'}


def mdtable(headers,rows):
    def escape(x): return str(x).replace('|',' / ').replace('\n',' ')
    return '\n'.join(['| '+' | '.join(headers)+' |','| '+' | '.join('---' for _ in headers)+' |',
                      *['| '+' | '.join(escape(x) for x in r)+' |' for r in rows]])+'\n'


def main():
    start=time.monotonic(); commits=read('commits.csv'); builds={r['point']:r for r in read('builds.csv')}
    series={g:{int(r['index']):r for r in read(g+'-survival.csv')} for g in GROUPS}
    breaks=read('pooled-first-break.csv'); events=[]
    status_order=['compile-fail','absent','fail','error','timeout']
    for c in commits[1:]:
        i=int(c['index']); failed=int(builds[c['point']]['build_fail'])
        if not (failed or c['tag'] or any(int(series[g][i]['deaths']) for g in GROUPS)): continue
        row={k:c[k] for k in ['index','point','date','tag','subject','main_files']}
        row['sha7']=c['sha'][:7]; row['build_fail']=failed
        for g in GROUPS:
            row[g+'_survived']=series[g][i]['survived']; row[g+'_deaths']=series[g][i]['deaths']
        statuses=Counter(r['first_break_status'] for r in breaks if r['first_break_index']==str(i))
        row.update({s.replace('-','_'):statuses[s] for s in status_order})
        events.append(row)
    fields=['index','point','sha7','date','tag','subject','main_files','build_fail',*[g+s for g in GROUPS for s in ['_survived','_deaths']],*[s.replace('-','_') for s in status_order]]
    write_csv(EXP/'results/events.csv',events,fields)
    indices=[0,*[int(r['index']) for r in events]]
    headers=['#','commit','date','change',*[f"{LABEL[g]} ({int(series[g][0]['denominator']):,})" for g in ORDER]]
    rows=[]
    for i in indices:
        c=commits[i]; commit=c['sha'][:7]+(' ('+c['tag']+')' if c['tag'] else '')
        rows.append([i,commit,c['date'],c['subject'][:60],*[f"{series[g][i]['survived']} ({100*int(series[g][i]['survived'])/int(series[g][i]['denominator']):.1f}%)" for g in ORDER]])
    (EXP/'results/table1-survival-by-event.md').write_text(mdtable(headers,rows))
    ranked=sorted([r for r in events if int(r['dev_deaths'])+int(r['pooled_deaths'])],key=lambda r:(-(int(r['dev_deaths'])+int(r['pooled_deaths'])),int(r['index'])))[:10]
    rows=[]
    for r in ranked:
        desc=', '.join(s+' '+str(r[s.replace('-','_')]) for s in status_order)
        rows.append([r['index'],r['sha7'],r['date'],r['subject'][:60],r['main_files'],r['dev_deaths'],r['pooled_deaths'],desc])
    (EXP/'results/table2-breaking-commits.md').write_text(mdtable(['#','commit','date','change','files in src/main','Dev deaths','LLM deaths','LLM death statuses'],rows))
    cov={r['group']:r for r in read('coverage-t-summary.csv')}; cuts=len([r for r in read('cut-classes.csv') if not r['excluded']])
    rows=[]
    for g in ORDER:
        r=cov[g]; rows.append([LABEL[g],r['methods_executed'],f"{r['lines_covered']}/{r['lines_total']} ({100*float(r['line_rate']):.1f}%)",f"{r['branches_covered']}/{r['branches_total']} ({100*float(r['branch_rate']):.1f}%)",f"{r['cuts_touched']}/{cuts}"])
    (EXP/'results/table3-coverage-t.md').write_text(mdtable(['Group','methods','line coverage (covered/total, rate)','branch coverage (covered/total, rate)',f'CUTs touched (of {cuts})'],rows))
    dev=read('dev-denominator.csv'); llm=read('llm-methods.csv'); funnel=[]
    for g in ORDER:
        members=dev if g=='dev' else [r for r in llm if g=='pooled' or r['technique']==g]
        passed=sum(all(r['run'+str(i)]=='pass' for i in [1,2,3]) for r in members)
        denominator=int(series[g][0]['denominator'])
        funnel.append(dict(group=g,listed=len(members),passed_three_of_three=passed,after_deduplication=denominator))
    write_csv(EXP/'results/t-funnel.csv',funnel,list(funnel[0]))
    (EXP/'results/t-funnel.md').write_text(mdtable(['Group','Listed','Passed three of three at t','After de-duplication (denominator)'],[[LABEL[r['group']],r['listed'],r['passed_three_of_three'],r['after_deduplication']] for r in funnel]))
    write_json(EXP/'results/events-stage.json',dict(seconds=round(time.monotonic()-start,3),events=len(events),breaking_commits=len([r for r in events if int(r['dev_deaths'])+int(r['pooled_deaths'])]),top_breaking_commits=len(ranked)))


if __name__=='__main__': main()
