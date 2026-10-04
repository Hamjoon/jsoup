#!/usr/bin/env python3
"""Freeze the release list from upstream tag metadata, enforcing the design."""
import csv
import datetime as dt
import json
from pathlib import Path
import re
import subprocess

EXP = Path(__file__).resolve().parents[1]
REPO = EXP.parents[1]


def git(*args):
    return subprocess.check_output(['git', '-C', str(REPO), *args], text=True).strip()


def main():
    metadata = git('for-each-ref', '--sort=creatordate',
                   '--format=%(refname:short)%09%(creatordate:iso-strict)%09%(objecttype)', 'refs/tags')
    tags = [line.split('\t') for line in metadata.splitlines()
            if re.fullmatch(r'jsoup-1\.\d+\.\d+', line.split('\t')[0])]
    start = next(i for i, row in enumerate(tags) if row[0] == 'jsoup-1.11.2')
    tags = tags[start:]
    base = dt.date.fromisoformat(tags[0][1][:10])
    rows = []
    for index, (tag, timestamp, kind) in enumerate(tags):
        date = dt.date.fromisoformat(timestamp[:10])
        minor = '.'.join(tag.removeprefix('jsoup-').split('.')[:2])
        rows.append(dict(index=index, tag=tag, date=date.isoformat(), tag_type=kind,
                         commit=git('rev-list', '-n', '1', tag),
                         days_since_t=(date-base).days, minor=minor, last_of_minor=0))
    for row in rows:
        row['last_of_minor'] = int(row is next(r for r in reversed(rows) if r['minor'] == row['minor']))
    results = EXP / 'results'
    results.mkdir(exist_ok=True)
    with (results / 'releases.csv').open('w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    checks = dict(rows=len(rows), first_tag=rows[0]['tag'], last_tag=rows[-1]['tag'],
                  last_date=rows[-1]['date'], distinct_minors=len({r['minor'] for r in rows}),
                  lightweight_tags=sum(r['tag_type'] == 'commit' for r in rows))
    errors = []
    for key, expected in dict(rows=27, first_tag='jsoup-1.11.2', last_tag='jsoup-1.23.2',
                              last_date='2026-08-26', distinct_minors=13).items():
        if checks[key] != expected:
            errors.append(f'{key}: expected {expected!r}, found {checks[key]!r}')
    checks['errors'] = errors
    (results / 'release-validation.json').write_text(json.dumps(checks, indent=2) + '\n')
    print(json.dumps(checks, indent=2))
    if errors:
        raise SystemExit('STOP: release metadata contradicts the fixed design')


if __name__ == '__main__':
    main()
