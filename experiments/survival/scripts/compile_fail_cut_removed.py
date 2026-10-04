#!/usr/bin/env python3
"""Repository-only removed-CUT sub-counts; never compile or execute tests."""
from collections import defaultdict
import csv
import hashlib
import re

from comparison_tables import render_table2
from dev_common import EXP, write_json
from dev_later import write_csv
from survival_common import GROUPS, REMOVED, identity, key, read_csv


def named_symbols(symbol):
    # Java identifier boundaries accept simple or qualified class names, but
    # exclude similarly named classes such as WhitelistTest. Only this CSV
    # column is examined, including its javac-message fallback when present.
    return [name for name in REMOVED
            if re.search(r'(?<![\w$])'+re.escape(name)+r'(?![\w$])', symbol)]


def main():
    results = EXP/'results'
    groups = [g for g in GROUPS if g != 'dev']
    allowed = {'table2-first-break.md', 'table2-first-break.csv',
               'compile-fail-cut-removed-addendum.json'}
    allowed.update(f'{g}-{suffix}.csv' for g in groups
                   for suffix in ['compile-fail-cut-removed', 'first-break-summary'])
    protected = {p: hashlib.sha256(p.read_bytes()).hexdigest()
                 for p in results.rglob('*') if p.is_file()
                 and p.relative_to(results).as_posix() not in allowed}
    counts = {}
    for group in groups:
        symbols = defaultdict(set)
        for row in read_csv(f'{group}-compile-errors.csv'):
            file_key = tuple(row[field] for field in
                             ['tag', 'class', 'technique', 'test_class'])
            symbols[file_key].update(named_symbols(row['symbol']))
        matches = []
        for row in read_csv(f'{group}-first-break.csv'):
            if row['first_break_status'] != 'compile-fail':
                continue
            file_key = (row['first_break_tag'], row['class'],
                        row['technique'], row['test_class'])
            for symbol in REMOVED:
                if symbol in symbols[file_key]:
                    matches.append({**{f: row[f] for f in identity(group)},
                                    'first_break_tag': row['first_break_tag'],
                                    'symbol': symbol})
        write_csv(results/f'{group}-compile-fail-cut-removed.csv', matches,
                  [*identity(group), 'first_break_tag', 'symbol'])
        count = len({key(row, group) for row in matches})
        counts[group] = {'methods': count, 'symbol_rows': len(matches)}
        path = results/f'{group}-first-break-summary.csv'
        # Preserve all existing summary records byte-for-byte; replace only
        # this optional sub-count record on subsequent invocations.
        lines = path.read_bytes().splitlines(keepends=True)
        kept = [line for line in lines if next(csv.reader(
            [line.decode()]))[0] != 'cut-removed-subcount']
        assert not kept or kept[-1].endswith(b'\n')
        path.write_bytes(b''.join(kept) +
                         f'cut-removed-subcount,compile-fail,,{count}\n'.encode())
    dev = read_csv('dev-compile-fail-cut-removed.csv')
    counts['dev'] = {'methods': len({key(r, 'dev') for r in dev}),
                     'symbol_rows': len(dev)}
    render_table2()
    for path, digest in protected.items():
        assert hashlib.sha256(path.read_bytes()).hexdigest() == digest, path
    write_json(results/'compile-fail-cut-removed-addendum.json', {
        'groups': counts,
        'matching': 'LLM first-break compile-fail file joined by tag, class, '
                    'technique, test_class; match only compile-errors.csv symbol '
                    'column using Java identifier boundaries; simple and '
                    'package-qualified names accepted; canonical simple symbols '
                    'written; count distinct methods, not symbol rows',
        'developer_subcount': 'Existing accepted developer CSV unchanged',
        'protected_result_files': len(protected),
        'protected_result_files_byte_identical': True,
        'compilation_or_execution_performed': False,
    })
    print(counts)


if __name__ == '__main__':
    main()
