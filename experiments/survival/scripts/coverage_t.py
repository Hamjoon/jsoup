#!/usr/bin/env python3
"""Collect baseline source-file line coverage; failures never stop the track."""
from collections import Counter
import csv
import json
import subprocess
import time
import xml.etree.ElementTree as ET
from dev_common import BASELINE, EXP, T, classpath as dev_cp, write_json
from dev_later import write_csv
from extract_v2 import TECHS
from llm_common import files,source,fqcn,classpath
from run_class import run_class


def cli(args,log):
    p=subprocess.run(['java','-jar',str(EXP/'tools/jacococli.jar'),*map(str,args)],cwd=EXP,
                     capture_output=True,text=True,timeout=180)
    log.write_text(p.stdout+p.stderr)
    if p.returncode:
        raise RuntimeError('JaCoCo CLI failed: '+str(log.relative_to(EXP)))


def main():
    start=time.monotonic()
    root=EXP/'llm'/BASELINE/'coverage'
    root.mkdir(parents=True,exist_ok=True)
    failures=[]
    groups={tech:[] for tech in TECHS}
    compiled=json.loads((EXP/'llm'/BASELINE/'compile-summary.json').read_text())
    statuses={(r['class'],r['technique']):r['status'] for r in compiled['files_ledger']}
    for row in files():
        if statuses[(row['class'],row['technique'])]!='compiled':
            continue
        name=row['class']+'.'+row['technique']
        dest=root/(name+'.exec')
        try:
            source(row)
            summary=run_class(fqcn(row),classpath(BASELINE,row['class'],row['technique']),root/(name+'.json'),
                timeout_ms=30000,java_options=[f'-javaagent:{EXP}/tools/jacocoagent.jar=destfile={dest},includes=org.jsoup.*'],
                working_directory=T)
            if not dest.exists():
                raise RuntimeError('No JaCoCo execution file')
            groups[row['technique']].append(dest)
        except Exception as exc:
            failures.append(dict(scope='file',**{'class':row['class']},technique=row['technique'],
                                 error_type=type(exc).__name__,message=str(exc)))
            if dest.exists():
                groups[row['technique']].append(dest)
        print('Coverage '+name,flush=True)
    groups['pooled']=[p for tech in TECHS for p in groups[tech]]
    dev_output=root/'dev.exec'
    try:
        compile_dev=json.loads((EXP/'dev'/BASELINE/'compile-summary.json').read_text())
        names=[r['test_class'] for r in compile_dev['files'] if not r['exit_status']]
        parts=[str(EXP/'dev'/BASELINE/'helpers'),*[str(EXP/'dev'/BASELINE/'classes'/n) for n in names],dev_cp(BASELINE,helpers=False)]
        command=['java',f'-javaagent:{EXP}/tools/jacocoagent.jar=destfile={dev_output},includes=org.jsoup.*',
                 '-cp',':'.join(parts),'org.junit.runner.JUnitCore',*names]
        p=subprocess.run(command,cwd=T,capture_output=True,text=True,timeout=600)
        (root/'dev.log').write_text(p.stdout+p.stderr)
        write_json(root/'dev-run.json',dict(command=command,exit_status=p.returncode))
        if not dev_output.exists():
            raise RuntimeError('Developer run produced no execution file')
        if p.returncode:
            failures.append(dict(scope='dev-suite',error_type='JUnitExit',message='Coverage suite exit '+str(p.returncode)))
        groups['Dev']=[dev_output]
    except Exception as exc:
        failures.append(dict(scope='dev-suite',error_type=type(exc).__name__,message=str(exc)))
        groups['Dev']=[]
    coverage={}
    for group,execs in groups.items():
        try:
            if not execs:
                raise RuntimeError('No execution files for group')
            merged=root/(group+'.merged.exec')
            cli(['merge',*execs,'--destfile',merged],root/(group+'.merge.log'))
            xml=root/(group+'.xml')
            cli(['report',merged,'--classfiles',T/'target/classes','--csv',root/(group+'.csv'),
                 '--xml',xml],root/(group+'.report.log'))
            data={}
            for package in ET.parse(xml).getroot().findall('package'):
                for sourcefile in package.findall('sourcefile'):
                    line=next((c for c in sourcefile.findall('counter') if c.attrib['type']=='LINE'),None)
                    if line is not None:
                        data[(package.attrib['name'].replace('/','.'),sourcefile.attrib['name'])]=(
                            int(line.attrib['covered']),int(line.attrib['missed']))
            coverage[group]=data
        except Exception as exc:
            failures.append(dict(scope='report',group=group,error_type=type(exc).__name__,message=str(exc)))
    rows=[]
    cuts=[r for r in csv.DictReader((EXP/'results/cut-classes.csv').open()) if not r['excluded']]
    for cut in cuts:
        row={k:cut[k] for k in ['package','class']}
        for group in ['Dev','pooled',*TECHS]:
            value=coverage.get(group,{}).get((cut['package'],cut['class']+'.java'))
            if value is None:
                row[group+'_line_coverage']=''
                row[group+'_covered']=''
                row[group+'_total']=''
            else:
                covered,missed=value
                total=covered+missed
                row[group+'_line_coverage']=f'{covered/total:.6f}' if total else ''
                row[group+'_covered']=covered
                row[group+'_total']=total
        rows.append(row)
    fields=['package','class',*[g+s for g in ['Dev','pooled',*TECHS] for s in ['_line_coverage','_covered','_total']]]
    write_csv(EXP/'results/coverage-t.csv',rows,fields)
    write_json(EXP/'results/coverage-t-summary.json',dict(cuts=len(cuts),groups_reported=list(coverage),
        failures=failures,seconds=round(time.monotonic()-start,3),
        measurement='One full run per compiled LLM test file, pooled across those classes; one whole developer JUnit suite run. Source-file line counters from JaCoCo XML accompany the requested raw CSV reports.',
        group_totals={g:dict(covered=sum(int(r[g+'_covered'] or 0) for r in rows),
                            total=sum(int(r[g+'_total'] or 0) for r in rows)) for g in ['Dev','pooled',*TECHS]}))
    print('Coverage collection complete; failures were recorded without stopping.')


if __name__=='__main__':
    main()
