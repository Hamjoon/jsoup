#!/usr/bin/env python3
"""Render original template placeholders without rescanning inserted text."""
import csv
import hashlib
import importlib.metadata
import json
import re
import statistics
import time
import tiktoken
from dev_common import EXP, T, write_json
from dev_later import write_csv

TECHS=['ZSL','FSL','CoT','ToT','GToT']
PLACEHOLDERS=re.compile(r'(\{class_name\}|\{source_code\}|\{fewshot_example\})')


def substitute(template,values):
    return ''.join(values.get(piece,piece) for piece in PLACEHOLDERS.split(template))


def main():
    start=time.monotonic()
    templates={tech:(EXP/'prompts/templates'/f'{tech}.txt').read_bytes().decode('utf-8') for tech in TECHS}
    example=(EXP/'prompts/templates/fewshot_example.txt').read_bytes().decode('utf-8')
    tokenizer=tiktoken.get_encoding('cl100k_base')
    cuts=[r for r in csv.DictReader((EXP/'results/cut-classes.csv').open()) if not r['excluded']]
    tokens=[]
    prompts=[]
    source_hashes={}
    # Preserve placeholder-looking source/example content as data.
    assert substitute('{fewshot_example}:{class_name}:{source_code}',{
        '{fewshot_example}':'example {class_name}', '{class_name}':'CUT',
        '{source_code}':'source {fewshot_example}'})=='example {class_name}:CUT:source {fewshot_example}'
    for cut in cuts:
        name=cut['class']
        source=(T/cut['path']).read_bytes().decode('utf-8')
        source_hashes[name]=hashlib.sha256(source.encode('utf-8')).hexdigest()
        values={'{fewshot_example}':example,'{class_name}':name,'{source_code}':source}
        for tech in TECHS:
            rendered=substitute(templates[tech],values)
            path=EXP/'prompts/rendered'/name/f'{tech}.txt'
            data=rendered.encode('utf-8')
            path.parent.mkdir(parents=True,exist_ok=True)
            if path.exists():
                assert path.read_bytes()==data, 'Refuse overwriting a different prompt: '+str(path)
            else:
                path.write_bytes(data)
            count=len(tokenizer.encode(rendered,disallowed_special=()))
            tokens.append(dict(**{'class':name},technique=tech,tokens=count))
            prompts.append(dict(**{'class':name},technique=tech,path=str(path.relative_to(EXP)),
                                sha256=hashlib.sha256(data).hexdigest(),tokens=count))
    assert len(prompts)==len(cuts)*len(TECHS)==260
    write_csv(EXP/'results/prompt-tokens.csv',tokens,['class','technique','tokens'])
    summary=[]
    for tech in TECHS:
        counts=[r['tokens'] for r in tokens if r['technique']==tech]
        summary.append(dict(technique=tech,prompts=len(counts),min=min(counts),median=statistics.median(counts),
                            max=max(counts),exceed_4096=sum(n>4096 for n in counts)))
    write_csv(EXP/'results/prompt-token-summary.csv',summary,['technique','prompts','min','median','max','exceed_4096'])
    write_json(EXP/'prompts/manifest.json',dict(techniques=TECHS,classes=len(cuts),prompts=len(prompts),
        tokenizer='cl100k_base',tokenizer_package='tiktoken',tokenizer_version=importlib.metadata.version('tiktoken'),
        template_sha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((EXP/'prompts/templates').glob('*.txt'))},
        source_sha256=source_hashes,rendering='Split only the original template on three placeholder tokens; join once; inserted text is never rescanned.',
        input_limit_applied=False,files=prompts))
    write_json(EXP/'results/prompt-render-summary.json',dict(classes=len(cuts),techniques=len(TECHS),
        prompts=len(prompts),tokenizer_version=importlib.metadata.version('tiktoken'),
        placeholder_preservation_checked=True,seconds=round(time.monotonic()-start,3)))
    print(json.dumps(summary,indent=2))


if __name__=='__main__':
    main()
