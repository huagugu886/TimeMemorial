#!/bin/bash
set -e
cd /sdcard/Download/TimeMemorial

BUILD_FILE="app/build.gradle.kts"

# ========== 推送模式检测 ==========
# 用法: bash push_release.sh [--ssh|--https]
# 自动检测: 远程地址是 git@ 开头用 SSH，https:// 开头用 HTTPS
# HTTPS 模式需要环境变量 GITHUB_TOKEN

REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "")

# 手动指定模式
if [[ "$1" == "--ssh" ]]; then
    PUSH_MODE="ssh"
elif [[ "$1" == "--https" ]]; then
    PUSH_MODE="https"
elif [[ "$REMOTE_URL" == git@github.com:* ]]; then
    PUSH_MODE="ssh"
elif [[ "$REMOTE_URL" == https://github.com/* ]]; then
    PUSH_MODE="https"
else
    PUSH_MODE="ssh"  # 默认 SSH
fi

# HTTPS 模式下配置 token 认证
if [[ "$PUSH_MODE" == "https" ]]; then
    if [[ -z "$GITHUB_TOKEN" ]]; then
        echo "❌ HTTPS 模式需要设置 GITHUB_TOKEN 环境变量"
        echo "   export GITHUB_TOKEN=你的token"
        echo "   或切换到 SSH: git remote set-url origin git@github.com:huagugu886/TimeMemorial.git"
        exit 1
    fi
    # 用 token 替换 URL 中的认证信息
    AUTH_URL=$(echo "$REMOTE_URL" | sed "s|https://|https://huagugu886:${GITHUB_TOKEN}@|")
    git remote set-url origin "$AUTH_URL"
    echo "🔐 推送模式: HTTPS (Token 认证)"
else
    echo "🔐 推送模式: SSH"
fi

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

# HTTPS 模式下恢复原始远程地址（不留 token）
if [[ "$PUSH_MODE" == "https" ]]; then
    git remote set-url origin "$REMOTE_URL"
fi

echo ""
echo "✅ Done! GitHub Actions will now build and create a release."
echo "   推送模式: ${PUSH_MODE}"
echo "   Version: ${TAG} (code ${NEW_CODE})"
echo "   Check: https://github.com/huagugu886/TimeMemorial/actions"
