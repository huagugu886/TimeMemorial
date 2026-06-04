#!/bin/bash
# TimeMemorial - 推送到 GitHub 并触发自动编译
# 用法: bash push_and_build.sh [--ssh|--https]
set -e

cd /sdcard/Download/TimeMemorial

# ========== 推送模式检测 ==========
REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "")

if [[ "$1" == "--ssh" ]]; then
    PUSH_MODE="ssh"
elif [[ "$1" == "--https" ]]; then
    PUSH_MODE="https"
elif [[ "$REMOTE_URL" == git@github.com:* ]]; then
    PUSH_MODE="ssh"
elif [[ "$REMOTE_URL" == https://github.com/* ]]; then
    PUSH_MODE="https"
else
    PUSH_MODE="ssh"
fi

if [[ "$PUSH_MODE" == "https" ]]; then
    if [[ -z "$GITHUB_TOKEN" ]]; then
        echo "❌ HTTPS 模式需要设置 GITHUB_TOKEN 环境变量"
        echo "   export GITHUB_TOKEN=你的token"
        echo "   或切换到 SSH: git remote set-url origin git@github.com:huagugu886/TimeMemorial.git"
        exit 1
    fi
    AUTH_URL=$(echo "$REMOTE_URL" | sed "s|https://|https://huagugu886:${GITHUB_TOKEN}@|")
    git remote set-url origin "$AUTH_URL"
    echo "🔐 推送模式: HTTPS (Token 认证)"
else
    echo "🔐 推送模式: SSH"
fi

echo "=== 1. 检查 Git 状态 ==="
git status --short

echo ""
echo "=== 2. 添加所有文件 ==="
git add -A

echo ""
echo "=== 3. 提交 ==="
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
git commit -m "feat: update TimeMemorial - $TIMESTAMP" || echo "没有新改动需要提交"

echo ""
echo "=== 4. 推送到 GitHub main 分支 ==="
git push origin main

# HTTPS 模式下恢复原始远程地址
if [[ "$PUSH_MODE" == "https" ]]; then
    git remote set-url origin "$REMOTE_URL"
fi

echo ""
echo "✅ 推送完成！(模式: ${PUSH_MODE})"
echo "📦 GitHub Actions 将自动开始编译..."
echo "🔗 查看编译进度: https://github.com/huagugu886/TimeMemorial/actions"
echo ""
echo "编译完成后，在 Actions 页面下载 'TimeMemorial-debug' artifact 即可获取 APK"
