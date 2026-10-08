"""Unchanged CSV writer extracted from the previous dev_later.py."""
import csv


def write_csv(path, rows, fields):
    with path.open('w',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=fields,lineterminator='\n')
        writer.writeheader()
        writer.writerows(rows)

