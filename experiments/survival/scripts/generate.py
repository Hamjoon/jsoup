#!/usr/bin/env python3
"""Generate the fixed call ledger with two workers and preserved HTTP retries."""
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, wait, FIRST_COMPLETED
import csv
import datetime as dt
import hashlib
import json
import os
import statistics
import time
import requests
from dev_common import EXP, write_json
from dev_later import write_csv
from model_probe import MODEL, ENDPOINT
from extract_v2 import TECHS


def distribution(values):
    return dict(count=len(values),min=min(values),median=statistics.median(values),max=max(values),total=sum(values)) if values else dict(count=0,total=0)


def generate_one(cut,tech,key,prompt_hash):
    name=cut['class']
    root=EXP/'runs'/name/tech
    if root.exists():
        raise RuntimeError('Refuse issuing another call for existing generation directory: '+str(root))
    prompt_path=EXP/'prompts/rendered'/name/f'{tech}.txt'
    prompt=prompt_path.read_bytes().decode('utf-8')
    assert hashlib.sha256(prompt.encode()).hexdigest()==prompt_hash
    body=dict(model=MODEL,temperature=0.7,max_tokens=16384,messages=[dict(role='user',content=prompt)])
    total_start=time.monotonic()
    attempts=[]
    for attempt in range(1,5):
        folder=root if attempt==1 else root/f'attempt-{attempt}'
        folder.mkdir(parents=True)
        write_json(folder/'request.json',body)
        timestamp=dt.datetime.now(dt.timezone.utc).isoformat()
        start=time.monotonic()
        http_status=None
        data={}
        raw=b''
        status='transport-error'
        error_type=None
        try:
            response=requests.post(ENDPOINT+'/chat/completions',json=body,
                headers={'Authorization':'Bearer '+key},timeout=(30,600))
            http_status=response.status_code
            raw=response.content
            if key.encode() in raw:
                raise RuntimeError('Credential echoed by server; response not saved')
            (folder/'raw-response.json').write_bytes(raw)
            try:
                data=response.json()
                status='ok' if http_status==200 else 'http-error'
            except ValueError:
                status='invalid-json' if http_status==200 else 'http-error'
        except requests.RequestException as exc:
            error_type=type(exc).__name__
            write_json(folder/'raw-response.json',dict(transport_error=error_type))
        choices=data.get('choices',[]) if isinstance(data,dict) else []
        choice=choices[0] if choices else {}
        message=choice.get('message',{})
        content=message.get('content')
        text=content if isinstance(content,str) else ''
        if status=='ok' and (not choices or not isinstance(content,str) or not text):
            status='empty-content'
        (folder/'response.md').write_bytes(text.encode('utf-8'))
        usage=data.get('usage',{}) if isinstance(data,dict) else {}
        write_json(folder/'usage.json',usage)
        record=dict(timestamp=timestamp,latency=round(time.monotonic()-start,3),status=status,
            http_status=http_status,finish_reason=choice.get('finish_reason'),provider=data.get('provider') if isinstance(data,dict) else None,
            prompt_tokens=usage.get('prompt_tokens'),completion_tokens=usage.get('completion_tokens'),
            reasoning_tokens=(usage.get('completion_tokens_details') or {}).get('reasoning_tokens'),attempt=attempt,
            model_requested=MODEL,model_reported=data.get('model') if isinstance(data,dict) else None,
            request_id=data.get('id') if isinstance(data,dict) else None,error_type=error_type,
            cut=name,technique=tech,prompt_sha256=prompt_hash,response_sha256=hashlib.sha256(text.encode()).hexdigest())
        write_json(folder/'run.json',record)
        attempts.append(record)
        if status=='http-error' and attempt<4:
            time.sleep(10)
            continue
        return dict(**{'class':name},technique=tech,status=status,finish_reason=record['finish_reason'],
            prompt_tokens=record['prompt_tokens'],completion_tokens=record['completion_tokens'],reasoning_tokens=record['reasoning_tokens'],
            latency=record['latency'],attempts=attempt,total_seconds=round(time.monotonic()-total_start,3),
            response_path=str((folder/'response.md').relative_to(EXP)),run_path=str((folder/'run.json').relative_to(EXP)),
            content_bytes=len(text.encode()),provider=record['provider'],
            all_attempt_prompt_tokens=sum(r['prompt_tokens'] or 0 for r in attempts),
            all_attempt_completion_tokens=sum(r['completion_tokens'] or 0 for r in attempts),
            all_attempt_reasoning_tokens=sum(r['reasoning_tokens'] or 0 for r in attempts))


def save(rows,expected,seconds):
    ordered=sorted(rows,key=lambda r:(r['class'],TECHS.index(r['technique'])))
    if ordered:
        write_csv(EXP/'results/generation.csv',ordered,list(ordered[0]))
    summary=dict(expected_calls=expected,completed_calls=len(rows),responses_with_content=sum(r['content_bytes']>0 for r in rows),
        finish_reasons=dict(Counter(str(r['finish_reason']) for r in rows)),statuses=dict(Counter(r['status'] for r in rows)),
        empty_contents=sum(r['content_bytes']==0 for r in rows),length_finishes=sum(r['finish_reason']=='length' for r in rows),
        attempts=sum(r['attempts'] for r in rows),maximum_concurrency=2,seconds=round(seconds,3),
        token_distributions={k:distribution([r[k] for r in rows if r[k] is not None]) for k in ['prompt_tokens','completion_tokens','reasoning_tokens']},
        total_tokens={k:sum(r['all_attempt_'+k] for r in rows) for k in ['prompt_tokens','completion_tokens','reasoning_tokens']})
    write_json(EXP/'results/generation-summary.json',summary)


def main():
    key=os.environ.get('OPENROUTER_API_KEY','')
    if not key:
        raise SystemExit('STOP: API key is absent; its value was not printed')
    cuts=[r for r in csv.DictReader((EXP/'results/cut-classes.csv').open()) if not r['excluded']]
    tasks=[(cut,tech) for cut in cuts for tech in TECHS]
    assert len(tasks)==260
    manifest=json.loads((EXP/'prompts/manifest.json').read_text())
    hashes={(r['class'],r['technique']):r['sha256'] for r in manifest['files']}
    if (EXP/'results/generation.csv').exists():
        raise SystemExit('STOP: generation ledger already exists; inspect before issuing more paid calls')
    for cut,tech in tasks:
        if (EXP/'runs'/cut['class']/tech).exists():
            raise SystemExit('STOP: existing call directory without generation ledger')
    iterator=iter(tasks)
    rows=[]
    start=time.monotonic()
    with ThreadPoolExecutor(max_workers=2) as executor:
        pending={}
        def submit():
            task=next(iterator,None)
            if task:
                cut,tech=task
                future=executor.submit(generate_one,cut,tech,key,hashes[(cut['class'],tech)])
                pending[future]=(cut['class'],tech)
        submit()
        submit()
        failed=False
        while pending:
            done,_=wait(pending,return_when=FIRST_COMPLETED)
            for future in done:
                name,tech=pending.pop(future)
                try:
                    row=future.result()
                except Exception as exc:
                    write_json(EXP/'results/generation-failure.json',dict(cut=name,technique=tech,error_type=type(exc).__name__))
                    failed=True
                    continue
                rows.append(row)
                save(rows,len(tasks),time.monotonic()-start)
                print(json.dumps({k:row[k] for k in ['class','technique','status','finish_reason','completion_tokens','attempts']}),flush=True)
                if row['status']!='ok':
                    failed=True
            if not failed:
                for _ in done:
                    submit()
        save(rows,len(tasks),time.monotonic()-start)
    if failed or len(rows)!=len(tasks) or any(not r['content_bytes'] for r in rows):
        raise SystemExit('STOP: generation has an unrecoverable/empty response; no well-formed response was retried')
    print('Generation complete; all responses with content are preserved.')


if __name__=='__main__':
    main()
