#!/bin/bash
set -e
cd /sdcard/Download/TimeMemorial

# Auto-detect version from build.gradle.kts
VERSION=$(grep 'versionName' app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
VERSION_CODE=$(grep 'versionCode' app/build.gradle.kts | sed 's/.*= //')
TAG="v${VERSION}"

echo "📦 TimeMemorial Release Push Script"
echo "===================================="
echo "🔖 Version: ${TAG} (versionCode: ${VERSION_CODE})"

echo ""
echo "[1/5] Git Status:"
git status --short

echo ""
echo "[2/5] Staging all changes..."
git add -A

echo ""
echo "[3/5] Committing..."
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
git commit -m "release: ${TAG} - $TIMESTAMP" || echo "没有新改动需要提交"

echo ""
echo "[4/5] Pushing to GitHub..."
git push origin main

echo ""
echo "[5/5] Creating and pushing tag ${TAG}..."
git tag -d "${TAG}" 2>/dev/null || true
git tag "${TAG}"
git push origin "${TAG}" --force

echo ""
echo "✅ Done! GitHub Actions will now build and create a release."
echo "   Version: ${TAG}"
echo "   Check: https://github.com/huagugu886/TimeMemorial/actions"
