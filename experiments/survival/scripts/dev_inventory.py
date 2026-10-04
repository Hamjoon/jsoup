#!/usr/bin/env python3
"""Inventory frozen baseline sources and report mismatches without editing data."""
from collections import Counter
import csv
import json
from pathlib import Path
import re

EXP = Path(__file__).resolve().parents[1]
T = EXP/'checkouts/jsoup-1.11.2'


def write_csv(path, rows, fields):
    with path.open('w', newline='') as f:
        writer=csv.DictWriter(f,fieldnames=fields,lineterminator='\n')
        writer.writeheader()
        writer.writerows(rows)


def main():
    rows=[]
    for file in sorted((T/'src/test/java').rglob('*.java')):
        text=file.read_text()
        match=re.search(r'^\s*package\s+([\w.]+)\s*;',text,re.M)
        package=match.group(1) if match else ''
        has_test='@Test' in text
        integration=package.startswith('org.jsoup.integration')
        role='helper' if not has_test else ('integration' if integration else 'test')
        rows.append(dict(path=str(file.relative_to(T)),package=package,
                         **{'class':file.stem},has_test_annotation=int(has_test),
                         integration=int(integration),role=role))
    write_csv(EXP/'results/dev-files.csv',rows,
              ['path','package','class','has_test_annotation','integration','role'])
    production={}
    for file in sorted((T/'src/main/java').rglob('*.java')):
        if file.name=='package-info.java':
            continue
        match=re.search(r'^\s*package\s+([\w.]+)\s*;',file.read_text(),re.M)
        package=match.group(1) if match else ''
        production[(package,file.stem)]=f'{package}.{file.stem}'
    mapping=[]
    for row in rows:
        if row['role']=='test':
            simple=row['class'][:-4] if row['class'].endswith('Test') else row['class']
            mapping.append(dict(test_class=f"{row['package']}.{row['class']}",
                                production_class=production.get((row['package'],simple),'')))
    write_csv(EXP/'results/dev-class-map.csv',mapping,['test_class','production_class'])
    roles=dict(Counter(r['role'] for r in rows))
    expected={'files':43,'test':34,'helper':3,'integration':6}
    observed={'files':len(rows),**roles}
    errors=[f'{key}: expected {value}, found {observed.get(key,0)}'
            for key,value in expected.items() if observed.get(key,0)!=value]
    summary=dict(expected=expected,observed=observed,
                 helpers=[r['path'] for r in rows if r['role']=='helper'],
                 integration_files=[r['path'] for r in rows if r['integration']],
                 integration_with_test_annotation=sum(r['integration'] and r['has_test_annotation'] for r in rows),
                 integration_package_first_roles=dict(Counter('integration' if r['integration'] else
                    'test' if r['has_test_annotation'] else 'helper' for r in rows)),
                 production_classes=len(production),mapped_production_classes=len({r['production_class'] for r in mapping if r['production_class']}),
                 test_annotations_outside_integration=sum((T/r['path']).read_text().count('@Test') for r in rows if not r['integration']),
                 errors=errors)
    (EXP/'results/dev-inventory-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    print(json.dumps(summary,indent=2))
    if errors:
        raise SystemExit('STOP: baseline inventory contradicts the expected counts; no test execution started')


if __name__=='__main__':
    main()
