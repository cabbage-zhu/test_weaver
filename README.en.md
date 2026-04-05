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

**Linux / macOS / Git Bash:**
```bash
# Project-local
bash <(curl -s https://gitee.com/ouyang-wenxuan/test_weaver/raw/master/scripts/install.sh)

# Global
bash <(curl -s https://gitee.com/ouyang-wenxuan/test_weaver/raw/master/scripts/install.sh) --global
```

**Windows CMD / PowerShell:**
```cmd
REM Download and run installer
git clone https://github.com/cabbage-zhu/test_weaver.git %TEMP%\test-weaver-installer
%TEMP%\test-weaver-installer\scripts\install.bat
REM Or global install
%TEMP%\test-weaver-installer\scripts\install.bat --global
```

### Manual Install

```bash
# Project-local
git clone https://github.com/cabbage-zhu/test_weaver.git .claude/skills/test-weaver

# Global (Linux / macOS)
git clone https://github.com/cabbage-zhu/test_weaver.git ~/.claude/skills/test-weaver

# Global (Windows)
git clone https://github.com/cabbage-zhu/test_weaver.git %USERPROFILE%\.claude\skills\test-weaver
```

## Upgrade

```bash
# Linux / macOS
cd .claude/skills/test-weaver && bash scripts/install.sh --upgrade

# Windows
cd .claude\skills\test-weaver && scripts\install.bat --upgrade

# Manual (all platforms)
cd .claude/skills/test-weaver && git pull
```

## Usage

In Claude Code:

```
/test-weaver                              # Analyze all git diff master changes
/test-weaver src/main/java/com/example/FooService.java   # Analyze specific file
/test-weaver feature/login                # Diff against specific branch
```

## Model Switching (Cost Optimization)

test-weaver is a relatively simple code generation task. It's recommended to use Haiku model to reduce costs.

### Manual Model Switching

In Claude Code, type:

```
/model haiku
```

After test-weaver completes, switch back to your original model:

```
/model opus
```

Or use the following to complete the task with a single command:

**Linux / macOS / Git Bash:**
```bash
/model haiku && /test-weaver
```

**Windows PowerShell:**
```powershell
/model haiku; /test-weaver
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
