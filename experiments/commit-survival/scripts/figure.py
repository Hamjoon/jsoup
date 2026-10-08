#!/usr/bin/env python3
"""Render the fixed commit-survival figure from the recorded curves."""
import json
import time
import re
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.ticker import PercentFormatter
from survival import GROUPS, read
from dev_common import EXP,write_json


def main():
    start=time.monotonic(); colors={'dev':'#172a3a','pooled':'#c43d3d','ZSL':'#0072b2','FSL':'#009e73','CoT':'#e69f00','ToT':'#8b63b5','GToT':'#4c9caa'}
    fig,ax=plt.subplots(figsize=(16,9),dpi=100,facecolor='white')
    for g in ['ZSL','FSL','CoT','ToT','GToT','dev','pooled']:
        rows=read(g+'-survival.csv')
        ax.step([int(r['index']) for r in rows],[100*int(r['survived'])/int(r['denominator']) for r in rows],
                where='post',color=colors[g],linewidth=3 if g in {'dev','pooled'} else 1.4,
                label={'dev':'Dev','pooled':'LLM pooled'}.get(g,g),zorder=4 if g in {'dev','pooled'} else 3)
    commits=read('commits.csv'); final=int(commits[-1]['index'])
    for r in commits[1:]:
        if r['tag']:
            i=int(r['index']);ax.axvline(i,color='#777777',linestyle='--',linewidth=1,alpha=.65,zorder=1)
            ax.text(i-.7 if i==final else i+.7,98,r['tag'],rotation=90,ha='right' if i==final else 'left',va='top',fontsize=10,color='#555555')
    failures=[int(r['index']) for r in read('builds.csv') if r['build_fail']=='1']
    if failures: ax.plot(failures,[0]*len(failures),'|',color='black',markersize=9,clip_on=False,label='Build failure')
    ax.set(xlim=(0,final),ylim=(0,100),xlabel='Commit index',ylabel='Cumulative survival')
    ax.yaxis.set_major_formatter(PercentFormatter(100)); ax.grid(axis='y',color='#e5e7eb',linewidth=.7)
    ax.spines[['top','right']].set_visible(False)
    handles,labels=ax.get_legend_handles_labels(); order=sorted(range(len(labels)),key=lambda i: ['Dev','LLM pooled','ZSL','FSL','CoT','ToT','GToT','Build failure'].index(labels[i]))
    ax.legend([handles[i] for i in order],[labels[i] for i in order],loc='lower left',frameon=True,framealpha=.95,ncol=2)
    fig.tight_layout(pad=2)
    for ext in ['png','svg']: fig.savefig(EXP/'results'/('figure1-survival-by-commit.'+ext),dpi=100,facecolor='white')
    svg=EXP/'results/figure1-survival-by-commit.svg'
    text,count=re.subn(r'(<svg\b[^>]*?)width="[^"]+" height="[^"]+"',
                       r'\1width="1600px" height="900px"',svg.read_text(),count=1)
    assert count==1
    svg.write_text(text)
    plt.close(fig)
    write_json(EXP/'results/figure-stage.json',dict(seconds=round(time.monotonic()-start,3),width=1600,height=900,build_fail_ticks=len(failures)))


if __name__=='__main__': main()
