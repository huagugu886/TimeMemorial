#!/bin/bash
# TimeMemorial - 一键推送到 GitHub 并发布 Release
# 用法: 在 Termux 中运行 bash /sdcard/Download/TimeMemorial/do_push.sh

set -e
cd /sdcard/Download/TimeMemorial

echo "🚀 TimeMemorial Release Push"
echo "============================"

echo ""
echo "[1/5] Git Status:"
git status --short

echo ""
echo "[2/5] Staging all changes..."
git add -A

echo ""
echo "[3/5] Committing..."
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
git commit -m "feat: v1.1.0 - $TIMESTAMP" || echo "没有新改动需要提交"

echo ""
echo "[4/5] Pushing to GitHub main..."
git push origin main

echo ""
echo "[5/5] Creating and pushing tag v1.1.0..."
git tag -d v1.1.0 2>/dev/null || true
git tag v1.1.0
git push origin v1.1.0 --force

echo ""
echo "✅ 推送完成！"
echo "📦 GitHub Actions 将自动编译并发布 Release"
echo "🔗 查看进度: https://github.com/huagugu886/TimeMemorial/actions"
echo "🔗 Release 页面: https://github.com/huagugu886/TimeMemorial/releases"
