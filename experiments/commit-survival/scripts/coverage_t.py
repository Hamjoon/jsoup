#!/usr/bin/env python3
"""Measure denominator-only baseline coverage, folding class counters by CUT."""
from collections import Counter, defaultdict
import csv
import json
from pathlib import Path
import subprocess
import time
from csv_utils import write_csv
from dev_common import EXP, PREV, T, BASELINE, FROZEN_TAG, classpath as dev_cp, write_json
from llm_common import classpath as llm_cp, files, source, fqcn
from run_class import run_class

GROUPS=['dev','ZSL','FSL','CoT','ToT','GToT','pooled']


def read(name,root=EXP):
    return list(csv.DictReader((root/'results'/name).open()))


def cli(args,log):
    command=['java','-jar',str(PREV/'tools/jacococli.jar'),*map(str,args)]
    p=subprocess.run(command,cwd=EXP,capture_output=True,text=True)
    log.write_text(p.stdout+p.stderr)
    if p.returncode: raise RuntimeError('JaCoCo failed: '+str(log))


def main():
    start=time.monotonic(); cuts=[r for r in read('cut-classes.csv') if not r['excluded']]
    assert len(cuts)==52
    dev=[r for r in read('dev-denominator.csv') if r['in_denominator']=='1']
    llm=read('llm-methods.csv'); extracted={(r['class'],r['technique']):r for r in files()}
    result=[]; summaries=[]; discrepancies=[]; runs=[]
    for group in GROUPS:
        root=EXP/'coverage'/group
        if root.exists(): raise RuntimeError('Refusing to overwrite coverage: '+group)
        root.mkdir(parents=True)
        tasks=defaultdict(list)
        if group=='dev':
            members=dev
            for r in members: tasks[(r['test_class'],)].append(r['method'])
        else:
            members=[r for r in llm if r['in_denominator_pooled']=='1'] if group=='pooled' else [r for r in llm if r['technique']==group and r['in_denominator_technique']=='1']
            for r in members: tasks[(r['class'],r['technique'],r['test_class'])].append(r['method'])
        execs=[]; measured=0
        for key,methods in sorted(tasks.items()):
            if group=='dev':
                name=key[0]
                cp=dev_cp(BASELINE,name).replace(str(EXP/'dev'/BASELINE),str(PREV/'dev'/FROZEN_TAG))
            else:
                cut,tech,name=key; source(extracted[(cut,tech)])
                cp=llm_cp(BASELINE,cut,tech).replace(str(EXP/'llm'/BASELINE),str(PREV/'llm'/FROZEN_TAG))
            cp=cp.replace(str(EXP/'checkouts'/BASELINE/'target/classes'),str(T/'target/classes'))
            stem='.'.join(key); dest=root/(stem+'.exec')
            summary=run_class(name,cp,root/(stem+'.json'),timeout_ms=30000,
                java_options=[f'-javaagent:{PREV}/tools/jacocoagent.jar=destfile={dest},includes=org.jsoup.*,append=false'],
                working_directory=EXP/'checkouts'/BASELINE,include_methods=methods)
            assert set(summary['listed_methods'])==set(methods)
            measured+=len(summary['methods'])
            for r in summary['methods']:
                if r['status']!='pass': discrepancies.append(dict(group=group,file=stem,test_class=name,method=r['method'],expected='pass',actual=r['status']))
            assert dest.is_file(), 'Missing JaCoCo execution data: '+stem
            execs.append(dest)
            runs.append(dict(group=group,file=stem,methods=len(summary['methods']),counts=summary['counts'],seconds=summary['wall_seconds']))
        assert measured==len(members)
        cli(['merge',*execs,'--destfile',root/'merged.exec'],root/'merge.log')
        cli(['report',root/'merged.exec','--classfiles',T/'target/classes','--sourcefiles',T/'src/main/java',
             '--csv',root/'report.csv','--xml',root/'report.xml','--html',root/'html'],root/'report.log')
        raw=list(csv.DictReader((root/'report.csv').open()))
        for cut in cuts:
            # JaCoCo CSV renders inner/anonymous binary names as Outer.Inner / Outer.new ... .
            classes=[r for r in raw if r['PACKAGE']==cut['package'] and (r['CLASS']==cut['class'] or r['CLASS'].startswith(cut['class']+'.') or r['CLASS'].startswith(cut['class']+'$'))]
            assert classes, 'CUT absent from JaCoCo class report: '+cut['class']
            lines=sum(int(r['LINE_COVERED']) for r in classes); total=lines+sum(int(r['LINE_MISSED']) for r in classes)
            branches=sum(int(r['BRANCH_COVERED']) for r in classes); btotal=branches+sum(int(r['BRANCH_MISSED']) for r in classes)
            result.append(dict(**{'class':cut['class']},group=group,lines_covered=lines,lines_total=total,branches_covered=branches,branches_total=btotal))
        group_rows=[r for r in result if r['group']==group]
        lc=sum(r['lines_covered'] for r in group_rows); lt=sum(r['lines_total'] for r in group_rows)
        bc=sum(r['branches_covered'] for r in group_rows); bt=sum(r['branches_total'] for r in group_rows)
        summaries.append(dict(group=group,methods_executed=measured,lines_covered=lc,lines_total=lt,line_rate=f'{lc/lt:.6f}',branches_covered=bc,branches_total=bt,branch_rate=f'{bc/bt:.6f}',cuts_touched=sum(r['lines_covered']>0 for r in group_rows),cuts_above_half=sum(r['lines_total']>0 and r['lines_covered']/r['lines_total']>=.5 for r in group_rows)))
        print(json.dumps(summaries[-1]),flush=True)
    assert len({r['lines_total'] for r in summaries})==len({r['branches_total'] for r in summaries})==1
    write_csv(EXP/'results/coverage-t.csv',result,list(result[0]))
    write_csv(EXP/'results/coverage-t-summary.csv',summaries,list(summaries[0]))
    write_csv(EXP/'results/coverage-t-discrepancies.csv',discrepancies,['group','file','test_class','method','expected','actual'])
    md=['| Group | Methods | Lines covered / total | Line rate | Branches covered / total | Branch rate | CUTs touched | CUTs at least half |','|---|---:|---:|---:|---:|---:|---:|---:|']
    md += [f"| {r['group']} | {r['methods_executed']} | {r['lines_covered']}/{r['lines_total']} | {100*float(r['line_rate']):.1f}% | {r['branches_covered']}/{r['branches_total']} | {100*float(r['branch_rate']):.1f}% | {r['cuts_touched']} | {r['cuts_above_half']} |" for r in summaries]
    (EXP/'results/coverage-t-summary.md').write_text('\n'.join(md)+'\n')
    differences=[]
    if (PREV/'results/coverage-t.csv').exists():
        previous={r['class']:r for r in read('coverage-t.csv',PREV)}
        for r in result:
            if r['group'] not in {'dev','pooled'}: continue
            prefix='Dev' if r['group']=='dev' else 'pooled'; old=previous[r['class']]
            if any(int(old[prefix+s])!=r[n] for s,n in [('_covered','lines_covered'),('_total','lines_total')]):
                differences.append(dict(**{'class':r['class']},group=r['group'],previous_covered=old[prefix+'_covered'],new_covered=r['lines_covered'],previous_total=old[prefix+'_total'],new_total=r['lines_total']))
        lines=['# Baseline coverage compared with the previous experiment','',
               'New coverage executes denominator methods only and sums JaCoCo class counters including inner/anonymous classes. The previous report used source-file line counters and broader test execution; overlapping source lines in separate classes can also affect totals.','',
               '| CUT | Group | Previous covered | New covered | Previous total | New total |','|---|---|---:|---:|---:|---:|']
        lines += ['| '+' | '.join(map(str,r.values()))+' |' for r in differences]
        if not differences: lines.append('No differences.')
        (EXP/'results/coverage-t-vs-prev.md').write_text('\n'.join(lines)+'\n')
        write_json(EXP/'results/coverage-t-vs-prev.json',differences)
    write_json(EXP/'results/coverage-stage.json',dict(seconds=round(time.monotonic()-start,3),groups=GROUPS,
        discrepancies=discrepancies,previous_differences=len(differences),runs=runs,
        method='Existing run_class java_options API; denominator-only includes; class-level counters folded by outer CUT, append=false.'))


if __name__=='__main__': main()
