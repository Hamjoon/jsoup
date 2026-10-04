#!/usr/bin/env python3
"""Freeze the accepted review data before the authorized incremental revision."""
import hashlib
import json
import shutil
from dev_common import BASELINE, EXP, write_json


def main():
    dest=EXP/'results/part2-baseline-before-jsoup-extractor'
    if dest.exists():
        raise SystemExit('Review snapshot already exists; refuse replacing it')
    dest.mkdir()
    paths=[]
    for pattern in ['generation*','extraction*','llm-*','coverage-t*','part2-baseline-summary.json']:
        paths.extend((EXP/'results').glob(pattern))
    for path in sorted(set(paths)):
        if path.is_file():
            shutil.copyfile(path,dest/path.name)
    shutil.copyfile(EXP/'docs/handover-part2-b.md',dest/'handover-part2-b.md')
    protected=[]
    roots=[EXP/'generated',EXP/'runs',EXP/'llm'/BASELINE/'classes',EXP/'llm'/BASELINE/'javac']
    roots.extend(EXP/'llm'/BASELINE/f'run{k}' for k in [1,2,3])
    roots.append(EXP/'llm'/BASELINE/'coverage')
    for root in roots:
        for path in sorted(root.rglob('*')):
            if path.is_file():
                protected.append(dict(file=str(path.relative_to(EXP)),sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
    write_json(dest/'protected-files.json',protected)
    write_json(EXP/'results/jsoup-extractor-revision.json',dict(
        reason='User authorized HTML detection on comments-and-literals-masked code for jsoup test inputs.',
        reissue=dict(cut='Elements',technique='GToT',attempt=2,
                    reason='User explicitly authorized one reissue of the content-bearing finish_reason=error response.',
                    retry_policy='Exactly one authorized request; no automatic retries.'),
        snapshot=str(dest.relative_to(EXP)),previously_extracted=175,previously_html_rejected=84,
        previous_pooled_denominator=1224,existing_file_results_preserved=True))
    print(json.dumps(dict(snapshot=str(dest.relative_to(EXP)),protected_files=len(protected))))


if __name__=='__main__':
    main()
