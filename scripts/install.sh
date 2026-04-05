#!/bin/bash
#
# test-weaver 安装/升级脚本
#
# 用法:
#   ./install.sh              安装到当前项目 .claude/skills/test-weaver/
#   ./install.sh --global     安装到 ~/.claude/skills/test-weaver/
#   ./install.sh --upgrade    升级已有安装
#   ./install.sh --uninstall  卸载

set -e

REPO_URL="https://github.com/cabbage-zhu/test_weaver.git"
SKILL_NAME="test-weaver"

print_usage() {
    echo "用法: ./install.sh [选项]"
    echo ""
    echo "选项:"
    echo "  (无)          安装到当前项目 .claude/skills/$SKILL_NAME/"
    echo "  --global      安装到 ~/.claude/skills/$SKILL_NAME/"
    echo "  --upgrade     升级已有安装"
    echo "  --uninstall   卸载"
    echo "  --help        显示帮助"
}

find_existing() {
    if [ -d ".claude/skills/$SKILL_NAME" ]; then
        echo ".claude/skills/$SKILL_NAME"
    elif [ -d "$HOME/.claude/skills/$SKILL_NAME" ]; then
        echo "$HOME/.claude/skills/$SKILL_NAME"
    else
        echo ""
    fi
}

do_install() {
    local target="$1"

    if [ -d "$target" ]; then
        echo "已安装在 $target，使用 --upgrade 升级。"
        exit 1
    fi

    mkdir -p "$(dirname "$target")"
    git clone "$REPO_URL" "$target"
    echo "安装完成: $target"
    echo "在 Claude Code 中使用 /test-weaver 即可。"
}

do_upgrade() {
    local existing
    existing=$(find_existing)

    if [ -z "$existing" ]; then
        echo "未找到已有安装。请先运行 ./install.sh 安装。"
        exit 1
    fi

    echo "升级: $existing"
    cd "$existing"
    local old_version
    old_version=$(grep '^version:' SKILL.md 2>/dev/null | head -1 | awk '{print $2}' || echo "unknown")
    git pull origin master
    local new_version
    new_version=$(grep '^version:' SKILL.md 2>/dev/null | head -1 | awk '{print $2}' || echo "unknown")
    echo "升级完成: $old_version -> $new_version"
}

do_uninstall() {
    local existing
    existing=$(find_existing)

    if [ -z "$existing" ]; then
        echo "未找到已有安装。"
        exit 0
    fi

    echo "即将删除: $existing"
    read -r -p "确认? [y/N] " confirm
    if [[ "$confirm" =~ ^[yY]$ ]]; then
        rm -rf "$existing"
        echo "已卸载。"
    else
        echo "取消。"
    fi
}

case "${1:-}" in
    --global)
        do_install "$HOME/.claude/skills/$SKILL_NAME"
        ;;
    --upgrade)
        do_upgrade
        ;;
    --uninstall)
        do_uninstall
        ;;
    --help|-h)
        print_usage
        ;;
    "")
        # 项目级安装
        project_root=$(git rev-parse --show-toplevel 2>/dev/null || true)
        if [ -z "$project_root" ]; then
            echo "当前不在 git 仓库中。使用 --global 进行全局安装。"
            exit 1
        fi
        do_install "$project_root/.claude/skills/$SKILL_NAME"
        ;;
    *)
        echo "未知选项: $1"
        print_usage
        exit 1
        ;;
esac
