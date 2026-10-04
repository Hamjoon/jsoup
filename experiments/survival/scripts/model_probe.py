#!/usr/bin/env python3
"""Record the requested model entry and make exactly one prompt probe call."""
import csv
import datetime as dt
import hashlib
import json
import os
import time
import requests
from dev_common import EXP, write_json

MODEL='openai/gpt-oss-120b'
ENDPOINT='https://openrouter.ai/api/v1'


def main():
    start=time.monotonic()
    key=os.environ.get('OPENROUTER_API_KEY','')
    if not key:
        raise SystemExit('STOP: API key is absent; its value was not printed')
    folder=EXP/'runs/probe'
    if folder.exists():
        raise SystemExit('STOP: probe output already exists; inspect before any additional paid call')
    response=requests.get(ENDPOINT+'/models',timeout=(30,120))
    if response.status_code!=200:
        write_json(EXP/'results/model-probe-summary.json',dict(status='model-list-http-error',http_status=response.status_code))
        raise SystemExit('STOP: model listing request failed')
    entries=response.json()['data']
    entry=next((r for r in entries if r['id']==MODEL),None)
    model_record=dict(timestamp=dt.datetime.now(dt.timezone.utc).isoformat(),model=MODEL,
                      listed=entry is not None,entry=entry)
    write_json(EXP/'results/model-entry.json',model_record)
    report='# Model probe\n\nRequested model: `'+MODEL+'`.\n\n'
    report+='Model entry recorded from the OpenRouter model-list endpoint:\n\n```json\n'+json.dumps(model_record,indent=2)+'\n```\n'
    (EXP/'results/model-probe.md').write_text(report)
    if entry is None:
        raise SystemExit('STOP: requested model is not listed')
    cuts=[r for r in csv.DictReader((EXP/'results/cut-classes.csv').open()) if not r['excluded']]
    cut=min(cuts,key=lambda r:(int(r['tokens']),r['class']))
    name=cut['class']
    prompt_path=EXP/'prompts/rendered'/name/'ZSL.txt'
    prompt=prompt_path.read_bytes().decode('utf-8')
    body=dict(model=MODEL,temperature=0.7,max_tokens=16384,messages=[dict(role='user',content=prompt)])
    folder.mkdir(parents=True)
    write_json(folder/'request.json',body)
    timestamp=dt.datetime.now(dt.timezone.utc).isoformat()
    call_start=time.monotonic()
    try:
        response=requests.post(ENDPOINT+'/chat/completions',json=body,
            headers={'Authorization':'Bearer '+key},timeout=(30,600))
    except requests.RequestException as exc:
        record=dict(timestamp=timestamp,latency=round(time.monotonic()-call_start,3),
                    status='transport-error',error_type=type(exc).__name__,attempt=1)
        write_json(folder/'run.json',record)
        write_json(EXP/'results/model-probe-summary.json',record)
        raise SystemExit('STOP: model probe transport failed; no retry was made')
    latency=round(time.monotonic()-call_start,3)
    raw=response.content
    if key.encode() in raw:
        raise SystemExit('STOP: server echoed a credential; response was not saved')
    (folder/'raw-response.json').write_bytes(raw)
    try:
        data=response.json()
    except ValueError:
        write_json(folder/'run.json',dict(timestamp=timestamp,latency=latency,status='invalid-json',http_status=response.status_code,attempt=1))
        raise SystemExit('STOP: model probe did not return JSON; no retry was made')
    choices=data.get('choices',[])
    choice=choices[0] if choices else {}
    message=choice.get('message',{})
    content=message.get('content')
    text=content if isinstance(content,str) else ''
    (folder/'response.md').write_bytes(text.encode('utf-8'))
    usage=data.get('usage',{})
    write_json(folder/'usage.json',usage)
    status='ok' if response.status_code==200 and choices and isinstance(content,str) and text else 'invalid-response'
    record=dict(timestamp=timestamp,latency=latency,status=status,http_status=response.status_code,
        finish_reason=choice.get('finish_reason'),provider=data.get('provider'),
        prompt_tokens=usage.get('prompt_tokens'),completion_tokens=usage.get('completion_tokens'),
        reasoning_tokens=(usage.get('completion_tokens_details') or {}).get('reasoning_tokens'),attempt=1,
        model_requested=MODEL,model_reported=data.get('model'),request_id=data.get('id'),
        cut=name,prompt_path=str(prompt_path.relative_to(EXP)),prompt_sha256=hashlib.sha256(prompt.encode()).hexdigest())
    write_json(folder/'run.json',record)
    summary=dict(**record,source_tokens=int(cut['tokens']),
        start_marker_present='###Test START##' in text,end_marker_present='###Test END##' in text,
        first_ten_lines=text.splitlines()[:10],response_sha256=hashlib.sha256(text.encode()).hexdigest(),
        seconds=round(time.monotonic()-start,3),input_limit_applied=False,max_tokens_sent=body['max_tokens'])
    write_json(EXP/'results/model-probe-summary.json',summary)
    report+='\nProbe result:\n\n```json\n'+json.dumps(summary,indent=2)+'\n```\n'
    (EXP/'results/model-probe.md').write_text(report)
    print(json.dumps(summary,indent=2))
    if status!='ok':
        raise SystemExit('STOP: probe failed; no retry was made')


if __name__=='__main__':
    main()
