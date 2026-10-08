#!/usr/bin/env python3
"""Exercise cumulative death, build-failure carry-forward, and restoration."""
import csv
import json
from pathlib import Path
import tempfile
import unittest
import survival
from csv_utils import write_csv


class SurvivalSemantics(unittest.TestCase):
    def test_build_failure_does_not_break_and_restoration_does_not_revive(self):
        original=survival.EXP
        try:
            with tempfile.TemporaryDirectory() as directory:
                survival.EXP=Path(directory); results=survival.EXP/'results'; results.mkdir()
                (results/'setup.json').write_text(json.dumps({'denominators':{'dev':2}}))
                methods=[dict(test_class='Example',method='A',in_denominator='1'),dict(test_class='Example',method='B',in_denominator='1')]
                write_csv(results/'dev-denominator.csv',methods,list(methods[0]))
                matrix=[dict(test_class='Example',method='A',p1='pass',p2='not-evaluated',p3='fail'),
                        dict(test_class='Example',method='B',p1='compile-fail',p2='not-evaluated',p3='pass')]
                write_csv(results/'dev-matrix.csv',matrix,list(matrix[0]))
                commits=[dict(index=i,point='p'+str(i),sha=str(i)*40,date='2000-01-01',days_since_t=i,tag='') for i in range(4)]
                builds={c['point']:{'build_fail':str(int(c['index']==2))} for c in commits}
                summary=survival.analyze('dev',commits,builds)
                curve=survival.read('dev-survival.csv')
                self.assertEqual([int(r['survived']) for r in curve],[2,1,1,0])
                self.assertEqual([int(r['deaths']) for r in curve],[0,1,0,1])
                self.assertEqual(summary['final_point_pass'],1)
                breaks={r['method']:r for r in survival.read('dev-first-break.csv')}
                self.assertEqual(breaks['A']['first_break_index'],'3')
                self.assertEqual(breaks['B']['first_break_index'],'1')
                transient=survival.read('dev-transient.csv')
                self.assertEqual([(r['method'],r['i'],r['j']) for r in transient],[('B','1','3')])
        finally:
            survival.EXP=original


if __name__=='__main__': unittest.main()
