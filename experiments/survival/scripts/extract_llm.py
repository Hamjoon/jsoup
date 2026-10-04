#!/usr/bin/env python3
"""Apply unchanged extract_v2; materialize its accepted source bytes only."""
from collections import Counter
import csv
import hashlib
import json
import re
import time
from dev_common import EXP, write_json
from dev_later import write_csv
from extract_v2 import extract, TECHS
from handover_a import table


def main():
    start=time.monotonic()
    calls=list(csv.DictReader((EXP/'results/generation.csv').open()))
    assert len(calls)==260 and all(r['status']=='ok' for r in calls)
    rows=[]
    for call in calls:
        text=(EXP/call['response_path']).read_bytes().decode('utf-8')
        result=extract(text)
        selected=result['combine_v2']
        match=re.search(r'public\s+class\s+(\w+)',selected) or re.search(r'class\s+(\w+)',selected)
        name=match.group(1) if match else ''
        status='extracted' if result['csr_v2'] and name else 'no-class' if not name else 'csr-rejected'
        data=selected.encode('utf-8')
        path=''
        if status=='extracted':
            output=EXP/'generated'/call['class']/call['technique']/f'{name}.java'
            output.parent.mkdir(parents=True,exist_ok=True)
            if output.exists():
                assert output.read_bytes()==data, 'Refuse changing a generated test'
            else:
                output.write_bytes(data)
            path=str(output.relative_to(EXP))
        rows.append(dict(**{'class':call['class']},technique=call['technique'],
            msr_detected=int(result['msr_detected']),csr_v2=int(result['csr_v2']),
            source='markers' if result['selection']=='markers' else 'fence' if result['selection']=='fence-fallback' else '',
            test_class=name,package=result['package'],sha256=hashlib.sha256(data).hexdigest(),
            test_annotations=selected.count('@Test'),status=status,file=path,response_path=call['response_path'],
            marker_pair=int(result['marker_pair']),braces_balanced=int(result['braces_balanced']),html_detected=int(result['html_detected'])))
    write_csv(EXP/'results/extraction.csv',rows,list(rows[0]))
    summary=[]
    for tech in TECHS:
        group=[r for r in rows if r['technique']==tech]
        summary.append(dict(technique=tech,responses=len(group),msr=sum(r['msr_detected'] for r in group),
            csr=sum(r['csr_v2'] for r in group),extracted=sum(r['status']=='extracted' for r in group),
            no_class=sum(r['status']=='no-class' for r in group),html_flagged=sum(r['html_detected'] for r in group)))
    write_csv(EXP/'results/extraction-summary.csv',summary,list(summary[0]))
    (EXP/'results/extraction-summary.md').write_text(table(summary,[('Technique','technique'),('Responses','responses'),('MSR','msr'),('CSR v2','csr'),('Extracted','extracted'),('No class','no_class'),('HTML flagged','html_flagged')]))
    write_json(EXP/'results/extraction-timing.json',dict(seconds=round(time.monotonic()-start,3),
        files_materialized=sum(r['status']=='extracted' for r in rows),rejection_statuses=dict(Counter(r['status'] for r in rows)),
        note='Materialization follows unchanged extract_v2 csr_v2 acceptance, including its HTML-detection flag; no source repair.'))
    print(json.dumps(summary,indent=2))


if __name__=='__main__':
    main()
