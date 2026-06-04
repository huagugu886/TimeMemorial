#!/bin/bash
set -e
cd /sdcard/Download/TimeMemorial

PROPS_FILE="app/version.properties"

# ========== 推送模式检测 ==========
# 用法: bash push_release.sh [--ssh|--https]
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
        echo "❌ HTTPS 模式需要设置 GITHUB_TOKEN"
        echo "   export GITHUB_TOKEN=你的token"
        exit 1
    fi
    AUTH_URL=$(echo "$REMOTE_URL" | sed "s|https://|https://huagugu886:${GITHUB_TOKEN}@|")
    git remote set-url origin "$AUTH_URL"
    echo "🔐 推送模式: HTTPS (Token 认证)"
else
    echo "🔐 推送模式: SSH"
fi

# ========== 1. 版本自增 (version.properties) ==========
echo ""
echo "📦 TimeMemorial Release Push"
echo "===================================="

MAJOR=$(grep '^major=' "$PROPS_FILE" | cut -d'=' -f2)
MINOR=$(grep '^minor=' "$PROPS_FILE" | cut -d'=' -f2)
PATCH=$(grep '^patch=' "$PROPS_FILE" | cut -d'=' -f2)
BUILD=$(grep '^build=' "$PROPS_FILE" | cut -d'=' -f2)

CURRENT_NAME="${MAJOR}.${MINOR}.${PATCH}"
CURRENT_BUILD=$BUILD

# patch +1, build +1
NEW_PATCH=$((PATCH + 1))
NEW_BUILD=$((BUILD + 1))

echo "🔼 当前版本: v${CURRENT_NAME} (build ${CURRENT_BUILD})"
echo "🆕 新版本:   v${MAJOR}.${MINOR}.${NEW_PATCH} (build ${NEW_BUILD})"

# 写入 version.properties
sed -i "s/^patch=.*/patch=${NEW_PATCH}/" "$PROPS_FILE"
sed -i "s/^build=.*/build=${NEW_BUILD}/" "$PROPS_FILE"

echo "✅ version.properties 已更新:"
cat "$PROPS_FILE"

# 更新 settings_page.html 里的硬编码版本号 (如果还有)
SETTINGS_HTML="app/src/main/assets/settings_page.html"
if [ -f "$SETTINGS_HTML" ]; then
    sed -i "s|<span class=\"val\">v${CURRENT_NAME}</span>|<span class=\"val\">v${MAJOR}.${MINOR}.${NEW_PATCH}</span>|" "$SETTINGS_HTML"
fi

TAG="v${MAJOR}.${MINOR}.${NEW_PATCH}"

# ========== 2. Git 操作 ==========
echo ""
echo "[1/4] Git Status:"
git status --short

echo ""
echo "[2/4] Staging & Committing..."
git add -A
git commit -m "release: ${TAG} - $(date '+%Y-%m-%d %H:%M:%S')" || echo "⚠️  没有新改动需要提交"

echo ""
echo "[3/4] Pushing to main..."
git push origin main

echo ""
echo "[4/4] Pushing tag ${TAG}..."
git tag -d "${TAG}" 2>/dev/null || true
git tag "${TAG}"
git push origin "${TAG}" --force

# ========== 3. 清理 ==========
if [[ "$PUSH_MODE" == "https" ]]; then
    git remote set-url origin "$REMOTE_URL"
fi

echo ""
echo "✅ Done!"
echo "   版本: ${TAG} (build ${NEW_BUILD})"
echo "   检查: https://github.com/huagugu886/TimeMemorial/actions"
