#!/bin/bash
set -e
cd /sdcard/Download/TimeMemorial

BUILD_FILE="app/build.gradle.kts"

# ========== 1. 自动升级版本号 ==========
echo "📦 TimeMemorial Release Push Script"
echo "===================================="

# 提取当前版本
CURRENT_CODE=$(grep 'versionCode' "$BUILD_FILE" | sed 's/.*= //' | tr -d ' ')
CURRENT_NAME=$(grep 'versionName' "$BUILD_FILE" | sed 's/.*"\(.*\)".*/\1/')

echo "🔼 当前版本: v${CURRENT_NAME} (code ${CURRENT_CODE})"

# 计算新版本：补丁号 +1
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_NAME"
NEW_PATCH=$((PATCH + 1))
NEW_NAME="${MAJOR}.${MINOR}.${NEW_PATCH}"
NEW_CODE=$((CURRENT_CODE + 1))

echo "🆕 新版本: v${NEW_NAME} (code ${NEW_CODE})"

# 替换版本号
sed -i "s/versionCode = ${CURRENT_CODE}/versionCode = ${NEW_CODE}/" "$BUILD_FILE"
sed -i "s/versionName = \"${CURRENT_NAME}\"/versionName = \"${NEW_NAME}\"/" "$BUILD_FILE"

echo "✅ 版本号已更新"
grep -E "versionCode|versionName" "$BUILD_FILE" | head -2

TAG="v${NEW_NAME}"

echo ""
echo "[2/5] Git Status:"
git status --short

echo ""
echo "[3/5] Staging all changes..."
git add -A

echo ""
echo "[4/5] Committing..."
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
git commit -m "release: ${TAG} - $TIMESTAMP" || echo "没有新改动需要提交"

echo ""
echo "[5/5] Pushing to GitHub..."
git push origin main

echo ""
echo "[6/6] Creating and pushing tag ${TAG}..."
git tag -d "${TAG}" 2>/dev/null || true
git tag "${TAG}"
git push origin "${TAG}" --force

echo ""
echo "✅ Done! GitHub Actions will now build and create a release."
echo "   Version: ${TAG} (code ${NEW_CODE})"
echo "   Check: https://github.com/huagugu886/TimeMemorial/actions"
