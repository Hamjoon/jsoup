# Script provenance

Frozen sources are read-only. No classpath order, method timeout, or status logic was changed. Shared dependency modules are copied so imports do not write bytecode under the previous experiment. extract_v2.py is imported only for lexical_masks and TECHS; no extraction or generation is executed.

| Script | Source | Source SHA-256 | Change |
|---|---|---|---|
| run_class.py | experiments/survival/scripts/run_class.py | `b40b267d791d48dad23bf861f40856f19939090c149aa4d38ef8f1014bc0de48` | Unchanged copy. |
| dev_compile.py | experiments/survival/scripts/dev_compile.py | `9b72e04525671736c4b56580a666f4de93e66dcca9986c8bc7f1a9a80ca6b13f` | Rename CLI to --point and validate commit builds; use the previous baseline method listings on compile failure. |
| dev_run.py | experiments/survival/scripts/dev_run.py | `443dcc449d0d60b8544f51143b6588cc63ff7ece27777756c24b9786c0918fdb` | Rename CLI to --point; read the unchanged ignored-class policy from PREV. |
| llm_compile.py | experiments/survival/scripts/llm_compile.py | `8547ee1a53965dc5881fe4295b3d64f6c47502588ee64273ef83a7238ecca790` | Rename CLI to --point; check CUT source presence in the point checkout; import the unchanged CSV writer from csv_utils. |
| llm_run.py | experiments/survival/scripts/llm_run.py | `a6419301ea24697fea27c5cdfe92879ea6f204e3821a9482a22eeb906614aa8c` | Rename CLI to --point; paths follow adapted shared modules. |
| survival.py | experiments/survival/scripts/survival.py | `9922cbd9bacfbc701a561b80cecdade17c2cd7b184bc2c50d837e23376fe30d0` | Import the unchanged CSV writer from csv_utils; cumulative commit analysis is deferred to Step 7 and this script is not executed before review. |
| dedup_lib.py | experiments/survival/scripts/dedup_lib.py | `ce4638c1598827522dd3d48652da1b5a3616b06e5fe7d7fb4a6e8b04035e0767` | Unchanged copy. |
| dev_common.py | experiments/survival/scripts/dev_common.py | `a1af1955bbe5afc34e5ca6b5e1562a2f22db25c50e497e030e55198b7ac820f5` | Use commit/build rows and point baseline; read frozen inventories, resources, runner and libraries from PREV; preserve classpath order. |
| llm_common.py | experiments/survival/scripts/llm_common.py | `536ee17cd1be4cd2ed5e8ecd545454c62141bb2e13d6bd50b0c54321fa50b4a4` | Read frozen extraction ledger/generated sources and runner/JUnit jars from PREV; validate point builds, preserving classpath order. |
| survival_common.py | experiments/survival/scripts/survival_common.py | `3b196e3c90cdd8fe912b4a0fae8e62add36c8b325530382c6a42d9df861ac718` | Read frozen denominator summary from PREV; release-analysis adaptation is deferred to Step 7. |
| extract_v2.py | experiments/survival/scripts/extract_v2.py | `15924c4bacca9c6f69ae928423b9bc4817a2d023757af3374a0a7dc60b481444` | Unchanged copy. |
| csv_utils.py | experiments/survival/scripts/dev_later.py | `48d3152fd55348cd3da8a8ef363eb35e1d05bf2f6c141aacee54e6258eba13f3` | Copy only write_csv unchanged; avoid importing the release orchestration module. |
