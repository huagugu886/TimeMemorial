#!/bin/bash
# TimeMemorial - 一键推送到 GitHub 并自动发布 Release
# 用法: 在 Termux 中运行 bash /sdcard/Download/TimeMemorial/do_push.sh

set -e
cd /sdcard/Download/TimeMemorial

echo "🚀 TimeMemorial Release Push"
echo "============================"

echo ""
echo "[1/4] Git Status:"
git status --short

echo ""
echo "[2/4] Staging all changes..."
git add -A

echo ""
echo "[3/4] Committing..."
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
git commit -m "feat: v1.2.0 fix signing & auto-release - $TIMESTAMP" || echo "没有新改动需要提交"

echo ""
echo "[4/4] Pushing to GitHub main..."
git push origin main

echo ""
echo "✅ 推送完成！"
echo "📦 GitHub Actions 将自动编译 Debug + Release APK 并发布 Release"
echo "🔗 查看进度: https://github.com/huagugu886/TimeMemorial/actions"
echo "🔗 Release 页面: https://github.com/huagugu886/TimeMemorial/releases"
