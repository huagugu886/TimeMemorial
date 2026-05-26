#!/bin/bash
# TimeMemorial - 推送到 GitHub 并触发自动编译
set -e

cd /sdcard/Download/TimeMemorial

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

echo ""
echo "✅ 推送完成！"
echo "📦 GitHub Actions 将自动开始编译..."
echo "🔗 查看编译进度: https://github.com/huagugu886/TimeMemorial/actions"
echo ""
echo "编译完成后，在 Actions 页面下载 'TimeMemorial-debug' artifact 即可获取 APK"
