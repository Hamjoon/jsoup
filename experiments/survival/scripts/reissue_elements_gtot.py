#!/usr/bin/env python3
"""Issue exactly the single reissue explicitly authorized at baseline review."""
import csv
import json
import os
from dev_common import EXP, write_json
from generate import generate_one, save


def main():
    rows=list(csv.DictReader((EXP/'results/generation.csv').open()))
    index=next(i for i,r in enumerate(rows) if (r['class'],r['technique'])==('Elements','GToT'))
    old=rows[index]
    assert old['attempts']=='1' and old['finish_reason']=='error'
    assert not (EXP/'runs/Elements/GToT/attempt-2').exists(), 'Refuse another paid reissue'
    key=os.environ.get('OPENROUTER_API_KEY','')
    if not key:
        raise SystemExit('API key absent; its value was not printed')
    record=json.loads((EXP/old['run_path']).read_text())
    row=generate_one({'class':'Elements'},'GToT',key,record['prompt_sha256'],authorized_attempt=2)
    revision=json.loads((EXP/'results/jsoup-extractor-revision.json').read_text())
    revision['reissue'].update(status=row['status'],finish_reason=row['finish_reason'],
        seconds=row['total_seconds'],response_path=row['response_path'])
    write_json(EXP/'results/jsoup-extractor-revision.json',revision)
    if row['status']!='ok':
        raise SystemExit('STOP: authorized reissue failed; response retained and no retry made')
    new_run=EXP/row['run_path']
    run=json.loads(new_run.read_text())
    run['authorization']=revision['reissue']['reason']
    write_json(new_run,run)
    for field in ['prompt_tokens','completion_tokens','reasoning_tokens']:
        row['all_attempt_'+field]+=int(old['all_attempt_'+field] or 0)
    row['total_seconds']+=float(old['total_seconds'])
    for r in rows:
        for field in ['prompt_tokens','completion_tokens','reasoning_tokens','attempts','content_bytes',
                      'all_attempt_prompt_tokens','all_attempt_completion_tokens','all_attempt_reasoning_tokens']:
            r[field]=int(r[field]) if r[field] else None
        for field in ['latency','total_seconds']:
            r[field]=float(r[field])
    rows[index]=row
    summary=json.loads((EXP/'results/generation-summary.json').read_text())
    save(rows,260,summary['seconds']+revision['reissue']['seconds'])
    print(json.dumps(revision['reissue'],indent=2))


if __name__=='__main__':
    main()
