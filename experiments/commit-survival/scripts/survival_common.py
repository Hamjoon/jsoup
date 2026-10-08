"""Shared method identities and verbatim javac diagnostics for both tracks."""
from functools import lru_cache
import csv
import json
import re
from dev_common import EXP, PREV
from llm_common import error_category

GROUPS=['dev','pooled','ZSL','FSL','CoT','ToT','GToT']
STATUSES=['pass','compile-fail','fail','error','timeout','absent','not-run','ignored']
REMOVED=['StringUtil','BooleanAttribute','Whitelist','ConstrainableInputStream','UncheckedIOException','ChangeNotifyingArrayList']

def read_csv(name):
    return list(csv.DictReader((EXP/'results'/name).open()))

def releases():
    return [r for r in read_csv('releases.csv') if int(r['index'])>0]

def key(row,group):
    return (row['test_class'],row['method']) if group=='dev' else (row['class'],row['technique'],row['test_class'],row['method'])

def identity(group):
    return ['test_class','method'] if group=='dev' else ['class','technique','test_class','method']

def matrix(group):
    rows=read_csv('dev-matrix.csv' if group=='dev' else 'llm-matrix.csv')
    if group=='pooled':
        rows=[r for r in rows if r['in_denominator_pooled']=='1']
    elif group!='dev':
        rows=[r for r in rows if r['technique']==group and r['in_denominator_technique']=='1']
    expected=594 if group=='dev' else json.loads((PREV/'results/llm-t-summary.json').read_text())['groups'][group]['denominator']
    assert len(rows)==len({key(r,group) for r in rows})==expected
    return rows

def javac_path(group,tag,row):
    stem=row['test_class'] if group=='dev' else row['class']+'.'+row['technique']
    return EXP/('dev' if group=='dev' else 'llm')/tag/'javac'/(stem+'.err')

@lru_cache(None)
def diagnostics(path):
    lines=path.read_text().splitlines()
    result=[]
    for i,line in enumerate(lines):
        match=re.match(r'^(.+\.java):(\d+): error: (.*)$',line)
        if not match:
            continue
        end=i+1
        while end<len(lines) and not re.match(r'^.+\.java:\d+: error:|^\d+ errors?$|^Note:',lines[end]):
            end+=1
        details=[s.strip() for s in lines[i+1:end] if re.match(r'^\s*(symbol|location):',s)]
        symbol=next((s.split(':',1)[1].strip() for s in details if s.startswith('symbol:')),match[3])
        result.append(dict(source_file=match[1],line=int(match[2]),category=error_category(match[3]),
            message=match[3],symbol=symbol,error_line=line,diagnostic='\n'.join(lines[i:end]),
            named_text='\n'.join([match[3],*details])))
    assert result, 'No diagnostic lines in '+str(path)
    return result
