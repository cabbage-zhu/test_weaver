# Test Weaver — 测试编织者

自动生成 Java 单元测试的 Claude Code Skill。根据 git diff 检测代码变更，分析覆盖率缺口，生成 TestNG + Mockito 单元测试，并能发现业务代码中的潜在 bug。

## 功能特性

- 自动通过 `git diff master` 获取代码改动点
- JaCoCo 覆盖率分析优先，静态代码分析兜底
- 生成覆盖代码路径和分支的单元测试
- 使用 TestNG + Mockito，不使用 PowerMock
- 静态方法依赖通过反射注入 mock 对象
- 测试失败时自动分析是测试问题还是业务 bug
- 发现业务 bug 只报告不修改

## 安装

### 方式一：脚本安装

```bash
# 安装到当前项目
bash <(curl -s https://gitee.com/ouyang-wenxuan/test_weaver/raw/master/scripts/install.sh)

# 全局安装
bash <(curl -s https://gitee.com/ouyang-wenxuan/test_weaver/raw/master/scripts/install.sh) --global
```

### 方式二：手动安装

```bash
# 项目级安装
git clone https://gitee.com/ouyang-wenxuan/test_weaver.git .claude/skills/test-weaver

# 全局安装
git clone https://gitee.com/ouyang-wenxuan/test_weaver.git ~/.claude/skills/test-weaver
```

## 升级

```bash
# 脚本升级
cd .claude/skills/test-weaver && bash scripts/install.sh --upgrade

# 手动升级
cd .claude/skills/test-weaver && git pull
```

## 卸载

```bash
cd .claude/skills/test-weaver && bash scripts/install.sh --uninstall
```

## 使用

在 Claude Code 中输入：

```
/test-weaver                              # 分析所有 git diff master 的变更
/test-weaver src/main/java/com/example/FooService.java   # 只分析指定文件
/test-weaver feature/login                # 对比指定分支
```

## 约束

| 约束 | 说明 |
|------|------|
| 测试框架 | TestNG（不用 JUnit） |
| Mock 框架 | Mockito（不用 PowerMock） |
| 静态方法 | 反射注入，不可测则跳过 |
| 测试目录 | `src/test/java/ut/` |
| 命名规范 | 驼峰式 `test{方法名}{测试内容}` |
| 业务代码 | 只读不写，发现 bug 只报告 |

## 目录结构

```
test_weaver/
├── SKILL.md                 # 核心 Skill 提示词
├── examples/                # 测试模板示例
│   ├── testng-basic.java
│   ├── testng-mockito.java
│   ├── testng-reflection.java
│   └── testng-branch-coverage.java
├── references/              # 规则文档
│   ├── testing-rules.md
│   └── bug-report-template.md
└── scripts/
    └── install.sh           # 安装/升级脚本
```

## 许可

MIT
