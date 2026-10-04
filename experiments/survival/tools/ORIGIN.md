# Tool provenance

Copied unchanged from the LLM4TS replication repository at commit `eb5088094ee51a7abb945f8cfabc5558803134e1`.

| File | Source SHA-256 | Copied SHA-256 |
|---|---|---|
| tools/junit-4.13.2.jar | 8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3 | 8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3 |
| tools/hamcrest-core-1.3.jar | 66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9 | 66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9 |
| tools/jacocoagent.jar | 67de51e9ca1db044f3a3d10613518befb02e8eee1015f2ff6d56cfb9d4506546 | 67de51e9ca1db044f3a3d10613518befb02e8eee1015f2ff6d56cfb9d4506546 |
| tools/jacococli.jar | c449591174982bbc003d1290003fcbc7b939215436922d2f0f25239d110d531a | c449591174982bbc003d1290003fcbc7b939215436922d2f0f25239d110d531a |
| tools/VERSIONS.md | 519ad230af3cbfa0029a1a7944f0caef80960eb33b63d5e63483c41e74e8c943 | 519ad230af3cbfa0029a1a7944f0caef80960eb33b63d5e63483c41e74e8c943 |
| tools/runner/JsonRunner.java | a327f70c735214a5cb83b8c0e327db18c030abd65e2c128c1f6385f4d01d8bde | a327f70c735214a5cb83b8c0e327db18c030abd65e2c128c1f6385f4d01d8bde |
| scripts/run_class.py | b40b267d791d48dad23bf861f40856f19939090c149aa4d38ef8f1014bc0de48 | b40b267d791d48dad23bf861f40856f19939090c149aa4d38ef8f1014bc0de48 |

## Interface (five lines)

JsonRunner takes FQCN, --list, --timeout-ms, and --exclude; run_class.py takes FQCN, required --classpath/--output, optional --timeout-ms/--cwd/--include.
JsonRunner emits JSON lines; the wrapper writes method/status/message/exception/milliseconds/launch JSON lines and a sibling .summary.json containing listing, launch logs, counts and timing.
Runner statuses are pass, fail, error, ignored, timeout; the wrapper adds not-run, but does not implement compile-fail.
At method timeout JsonRunner emits timeout and halts the JVM; the wrapper starts another JVM excluding completed and unselected methods.
Listing uses a separate JVM; --cwd resolves classpath entries to absolute paths; --include selects named methods and marks missing selections not-run.

## Status-contract clarification

The initial exact status-set check triggered an early stop because run_class.py does not emit compile-fail. Gary confirmed that compile-fail is assigned by dev_compile.py at the file level. The Step 3 check is satisfied by pass/fail/error/ignored/timeout from JsonRunner and not-run from run_class.py. Both copied files remain unchanged. Release builds may proceed; the denominator review stop remains required.

## LLM prompt and library provenance

Copied byte for byte from the LLM4TS replication repository at commit `eb5088094ee51a7abb945f8cfabc5558803134e1`. Only source_tests from dedup_lib.py and extract from extract_v2.py are imported; their entry points are not called.

| Source file | Experiment file | Source SHA-256 | Copied SHA-256 |
|---|---|---|---|
| prompts/templates/ZSL.txt | prompts/templates/ZSL.txt | 76f684228964c772d401a8cb44e8ccb0f3c2fb525caaf195f3ceb8bd21f7f4ef | 76f684228964c772d401a8cb44e8ccb0f3c2fb525caaf195f3ceb8bd21f7f4ef |
| prompts/templates/FSL.txt | prompts/templates/FSL.txt | 8ec872a158a61d4a0ea63591990a3cca35fcf9a8d32129a71ac63a1fe68fc99a | 8ec872a158a61d4a0ea63591990a3cca35fcf9a8d32129a71ac63a1fe68fc99a |
| prompts/templates/CoT.txt | prompts/templates/CoT.txt | d176f4276fb22664e80e318d6f2118181ee34b153a14ad548eb553e0c1efd50b | d176f4276fb22664e80e318d6f2118181ee34b153a14ad548eb553e0c1efd50b |
| prompts/templates/ToT.txt | prompts/templates/ToT.txt | 6cabd0f4020d024e933e1ea7a01ba833e12e47d6f4a90450dde0add5876d329b | 6cabd0f4020d024e933e1ea7a01ba833e12e47d6f4a90450dde0add5876d329b |
| prompts/templates/GToT.txt | prompts/templates/GToT.txt | cd305c96c26e4497b65597b352fe36b84020cfe7e245269bb37008bbe7caba61 | cd305c96c26e4497b65597b352fe36b84020cfe7e245269bb37008bbe7caba61 |
| prompts/templates/fewshot_example.txt | prompts/templates/fewshot_example.txt | e85ca2d0f5041b8029a7319562c81df2c46882418f110b6f47d0c8b562317ae4 | e85ca2d0f5041b8029a7319562c81df2c46882418f110b6f47d0c8b562317ae4 |
| scripts/extract_v2.py | scripts/extract_v2.py | 15924c4bacca9c6f69ae928423b9bc4817a2d023757af3374a0a7dc60b481444 | 15924c4bacca9c6f69ae928423b9bc4817a2d023757af3374a0a7dc60b481444 |
| scripts/dedup.py | scripts/dedup_lib.py | ce4638c1598827522dd3d48652da1b5a3616b06e5fe7d7fb4a6e8b04035e0767 | ce4638c1598827522dd3d48652da1b5a3616b06e5fe7d7fb4a6e8b04035e0767 |
