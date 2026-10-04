#!/usr/bin/env python3
"""Inventory baseline production types and their unchanged source paths."""
from collections import Counter
import csv
import json
import time
import javalang
import tiktoken
from dev_common import BASELINE, EXP, T, write_json
from dev_later import write_csv


def main():
    start=time.monotonic()
    tokenizer=tiktoken.get_encoding('cl100k_base')
    mapped={r['production_class'] for r in csv.DictReader((EXP/'results/dev-class-map.csv').open()) if r['production_class']}
    rows=[]
    for path in sorted((T/'src/main/java').rglob('*.java')):
        text=path.read_bytes().decode('utf-8')
        tree=javalang.parse.parse(text)
        package=tree.package.name if tree.package else ''
        if path.name=='package-info.java':
            name='package-info'
            kind='package-info'
        else:
            assert len(tree.types)==1, 'Unexpected multiple top-level declarations: '+str(path)
            top=tree.types[0]
            name=top.name
            assert name==path.stem
            if isinstance(top,javalang.tree.InterfaceDeclaration):
                kind='interface'
            elif isinstance(top,javalang.tree.EnumDeclaration):
                kind='enum'
            else:
                kind='abstract' if 'abstract' in top.modifiers else 'class'
        excluded=('package-info' if kind=='package-info' else 'examples' if package=='org.jsoup.examples' or
                  package.startswith('org.jsoup.examples.') else 'entities-data' if name=='EntitiesData' else
                  'interface' if kind=='interface' else '')
        rows.append(dict(package=package,**{'class':name},path=str(path.relative_to(T)),kind=kind,
                         lines=len(text.splitlines()),tokens=len(tokenizer.encode(text,disallowed_special=())),
                         excluded=excluded,has_same_name_test=int(package+'.'+name in mapped)))
    write_csv(EXP/'results/cut-classes.csv',rows,['package','class','path','kind','lines','tokens','excluded','has_same_name_test'])
    cuts=[r for r in rows if not r['excluded']]
    assert len({r['class'] for r in cuts})==len(cuts), 'Simple-name prompt directory collision'
    releases=[r for r in csv.DictReader((EXP/'results/releases.csv').open()) if int(r['index'])>0]
    tags=[r['tag'] for r in releases]
    presence=[]
    differences=[]
    missing=[]
    for cut in cuts:
        row={k:cut[k] for k in ['package','class','path']}
        for release in releases:
            tag=release['tag']
            value=int((EXP/'checkouts'/tag/cut['path']).is_file())
            row[tag]=value
            version=tuple(map(int,tag.removeprefix('jsoup-').split('.')))
            expected=int(not ((cut['class']=='StringUtil' and version>=(1,12,1)) or
                              (cut['class']=='UncheckedIOException' and version>=(1,19,1))))
            if value!=expected:
                differences.append(dict(package=cut['package'],**{'class':cut['class']},tag=tag,expected=expected,observed=value))
            if not value:
                missing.append(dict(package=cut['package'],**{'class':cut['class']},tag=tag))
        presence.append(row)
    write_csv(EXP/'results/cut-presence.csv',presence,['package','class','path',*tags])
    observed=dict(files=len(rows),cuts=len(cuts),abstract_cuts=sum(r['kind']=='abstract' for r in cuts))
    expected=dict(files=66,cuts=52,abstract_cuts=7)
    differences_inventory=[f'{k}: expected {v}, found {observed[k]}' for k,v in expected.items() if observed[k]!=v]
    errors=[] if observed['cuts']==expected['cuts'] else ['CUT count would change the fixed generation-call count']
    summary=dict(baseline=BASELINE,expected=expected,observed=observed,
        exclusions=dict(Counter(r['excluded'] for r in rows if r['excluded'])),
        kinds=dict(Counter(r['kind'] for r in cuts)),presence_missing=missing,
        presence_unexpected=differences,inventory_unexpected=differences_inventory,
        abstract_classes=[r['package']+'.'+r['class'] for r in cuts if r['kind']=='abstract'],
        errors=errors,seconds=round(time.monotonic()-start,3))
    write_json(EXP/'results/cut-inventory-summary.json',summary)
    print(json.dumps({k:v for k,v in summary.items() if k not in ['presence_missing','presence_unexpected']},indent=2))
    if errors:
        raise SystemExit('STOP: CUT inventory differs from the fixed design')
    if differences or differences_inventory:
        print('Unexpected inventory/presence findings are recorded; continuing under Step 10 report-and-continue instruction.')


if __name__=='__main__':
    main()
