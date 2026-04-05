# Test Weaver

A Claude Code Skill that automatically generates Java unit tests. Detects code changes via git diff, analyzes coverage gaps, generates TestNG + Mockito unit tests, and identifies potential business logic bugs.

## Features

- Auto-detect changes via `git diff master`
- JaCoCo coverage analysis with static code analysis fallback
- Generate tests covering code paths and branches
- TestNG + Mockito only, no PowerMock
- Reflection-based injection for static method dependencies
- Auto-detect whether test failures are test issues or business bugs
- Report suspected bugs without modifying business code

## Installation

### Script Install

```bash
# Project-local
bash <(curl -s https://gitee.com/ouyang-wenxuan/test_weaver/raw/master/scripts/install.sh)

# Global
bash <(curl -s https://gitee.com/ouyang-wenxuan/test_weaver/raw/master/scripts/install.sh) --global
```

### Manual Install

```bash
# Project-local
git clone https://gitee.com/ouyang-wenxuan/test_weaver.git .claude/skills/test-weaver

# Global
git clone https://gitee.com/ouyang-wenxuan/test_weaver.git ~/.claude/skills/test-weaver
```

## Upgrade

```bash
cd .claude/skills/test-weaver && git pull
```

## Usage

In Claude Code:

```
/test-weaver                              # Analyze all git diff master changes
/test-weaver src/main/java/com/example/FooService.java   # Analyze specific file
/test-weaver feature/login                # Diff against specific branch
```

## Constraints

| Constraint | Detail |
|------------|--------|
| Test framework | TestNG (not JUnit) |
| Mock framework | Mockito (not PowerMock) |
| Static methods | Reflection injection; skip if untestable |
| Test directory | `src/test/java/ut/` |
| Naming | camelCase `test{Method}{Scenario}` |
| Business code | Read-only; report bugs, never modify |

## License

MIT
