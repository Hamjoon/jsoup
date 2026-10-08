#!/usr/bin/env python3
"""Enumerate and validate the complete first-parent commit experiment."""
import csv
from datetime import datetime
import json
from pathlib import Path
import re
import subprocess
import time
from csv_utils import write_csv

EXP = Path(__file__).resolve().parents[1]
REPO = EXP.parents[1]
START, END = 'jsoup-1.11.2', 'jsoup-1.13.1'


def git(*args):
    return subprocess.check_output(['git', '-c', f'safe.directory={REPO}', '-C', str(REPO), *args], text=True).strip()


def main():
    started = time.monotonic()
    baseline = git('rev-parse', START+'^{commit}')
    shas = [baseline, *git('rev-list', '--first-parent', '--reverse', START+'..'+END).splitlines()]
    t_date = datetime.fromisoformat(git('show', '-s', '--format=%cI', baseline)).date()
    rows = []
    prior_tree = None
    for index, sha in enumerate(shas):
        date = datetime.fromisoformat(git('show', '-s', '--format=%cI', sha)).date()
        parents = git('show', '-s', '--format=%P', sha).split()
        paths = git('diff-tree', '--no-commit-id', '--name-only', '-r', parents[0], sha).splitlines()
        # An explicit parent diff verifies that merges count only first-parent changes.
        assert paths == git('diff', '--no-renames', '--name-only', parents[0], sha).splitlines()
        tags = sorted(t for t in git('tag', '--points-at', sha).splitlines() if re.fullmatch(r'jsoup-1\.\d+\.\d+', t))
        assert len(tags) <= 1, (sha, tags)
        tree = (git('rev-parse', sha+':src/main'), git('rev-parse', sha+':pom.xml'))
        main = [p for p in paths if p.startswith('src/main/')]
        test = [p for p in paths if p.startswith('src/test/')]
        pom = [p for p in paths if p == 'pom.xml']
        row = dict(index=index, point=f'c{index:03d}-{sha[:7]}', sha=sha, date=str(date),
                   days_since_t=(date-t_date).days, is_merge=int(len(parents)>1), parent1=parents[0],
                   subject=git('show', '-s', '--format=%s', sha)[:80], tag=tags[0] if tags else '',
                   files_main=len(main), files_test=len(test), files_pom=len(pom),
                   files_other=len(paths)-len(main)-len(test)-len(pom),
                   main_files='|'.join(sorted({Path(p).stem for p in main if p.startswith('src/main/java/') and p.endswith('.java')})),
                   same_main_as_prev=int(prior_tree is not None and tree == prior_tree))
        rows.append(row)
        prior_tree = tree
    write_csv(EXP/'results/commits.csv', rows, list(rows[0]))
    expected = {23:'jsoup-1.11.3', 68:'jsoup-1.12.1', 142:'jsoup-1.12.2', 185:END}
    actual = {r['index']:r['tag'] for r in rows[1:] if r['tag']}
    release = next(r for r in csv.DictReader((EXP/'results/releases.csv').open()) if r['tag']==END)
    errors = []
    if len(rows)-1 != 185:
        errors.append(f'Expected 185 later commits, found {len(rows)-1}')
    if actual != expected:
        errors.append(f'Tag positions differ: {actual}')
    if rows[-1]['sha'] != release['commit']:
        errors.append('Final SHA differs from the copied release ledger')
    if any(r['parent1'] != rows[i-1]['sha'] for i,r in enumerate(rows) if i):
        errors.append('First-parent chain is not consecutive')
    summary = dict(rows=len(rows), evaluated_points=len(rows)-1, tag_positions=actual,
                   merges=sum(r['is_merge'] for r in rows[1:]),
                   rows_with_main_changes=sum(r['files_main']>0 for r in rows[1:]),
                   rows_same_main_as_prev=sum(r['same_main_as_prev'] for r in rows[1:]),
                   counts_scope='Indices after baseline only', baseline=rows[0],
                   notable_rows=[r for r in rows if {'StringUtil','BooleanAttribute'} & set(r['main_files'].split('|'))],
                   endpoint_matches_release=rows[-1]['sha']==release['commit'],
                   seconds=round(time.monotonic()-started,3), errors=errors)
    (EXP/'results/commits-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    print(json.dumps({k:v for k,v in summary.items() if k not in {'baseline','notable_rows'}},indent=2))
    if errors:
        raise SystemExit('STOP: commit validation failed; see results/commits-summary.json')


if __name__ == '__main__':
    main()
