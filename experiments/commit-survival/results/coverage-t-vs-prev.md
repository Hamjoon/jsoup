# Baseline coverage compared with the previous experiment

New coverage executes denominator methods only and sums JaCoCo class counters including inner/anonymous classes. The previous report used source-file line counters and broader test execution; overlapping source lines in separate classes can also affect totals.

| CUT | Group | Previous covered | New covered | Previous total | New total |
|---|---|---:|---:|---:|---:|
| HttpConnection | dev | 203 | 203 | 596 | 598 |
| StringUtil | dev | 81 | 80 | 84 | 85 |
| Attributes | dev | 157 | 158 | 166 | 167 |
| Element | dev | 373 | 375 | 403 | 405 |
| Node | dev | 212 | 213 | 229 | 230 |
| HtmlTreeBuilderState | dev | 828 | 851 | 1081 | 1104 |
| TokeniserState | dev | 698 | 765 | 1090 | 1157 |
| HttpConnection | pooled | 140 | 140 | 596 | 598 |
| StringUtil | pooled | 82 | 83 | 84 | 85 |
| W3CDom | pooled | 70 | 63 | 74 | 74 |
| Attribute | pooled | 61 | 57 | 64 | 64 |
| Attributes | pooled | 160 | 161 | 166 | 167 |
| Element | pooled | 372 | 335 | 403 | 405 |
| Entities | pooled | 135 | 134 | 141 | 141 |
| LeafNode | pooled | 32 | 30 | 33 | 33 |
| Node | pooled | 162 | 144 | 229 | 230 |
| CharacterReader | pooled | 227 | 222 | 230 | 230 |
| HtmlTreeBuilderState | pooled | 390 | 406 | 1081 | 1104 |
| Token | pooled | 151 | 144 | 156 | 156 |
| TokenQueue | pooled | 136 | 131 | 138 | 138 |
| TokeniserState | pooled | 300 | 359 | 1090 | 1157 |
| Whitelist | pooled | 211 | 207 | 222 | 222 |
| Elements | pooled | 177 | 153 | 182 | 182 |
| Evaluator | pooled | 209 | 208 | 211 | 211 |
| NodeTraversor | pooled | 62 | 55 | 66 | 66 |
| QueryParser | pooled | 120 | 118 | 233 | 233 |
