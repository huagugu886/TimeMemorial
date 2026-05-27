#!/bin/bash
set -e
cd /sdcard/Download/TimeMemorial

echo "📦 TimeMemorial Release Push Script"
echo "===================================="

echo ""
echo "[1/5] Git Status:"
git status --short

echo ""
echo "[2/5] Staging all changes..."
git add -A

echo ""
echo "[3/5] Committing..."
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
git commit -m "release: v1.3.0 - $TIMESTAMP" || echo "没有新改动需要提交"

echo ""
echo "[4/5] Pushing to GitHub..."
git push origin main

echo ""
echo "[5/5] Creating and pushing tag v1.3.0..."
git tag -d v1.3.0 2>/dev/null || true
git tag v1.3.0
git push origin v1.3.0 --force

echo ""
echo "✅ Done! GitHub Actions will now build and create a release."
echo "   Check: https://github.com/huagugu886/TimeMemorial/actions"
