#!/bin/bash
# TimeMemorial - 上传到 GitHub 并触发自动编译
# 用法：bash upload.sh [版本号]
# 例如：bash upload.sh v1.0.0

cd /sdcard/Download/TimeMemorial

echo "📦 准备上传到 GitHub..."

# 配置 Git（如果还没配置）
git config user.email "bot@timememorial.app" 2>/dev/null
git config user.name "TimeMemorial Bot" 2>/dev/null

# 添加所有文件
echo "📁 添加文件..."
git add -A

# 检查是否有改动
if git diff --cached --quiet; then
    echo "⚠️  没有新的改动需要提交"
else
    # 提交
    MSG="🚀 更新: $(date '+%Y-%m-%d %H:%M')"
    echo "💾 提交: $MSG"
    git commit -m "$MSG"
fi

# 推送到 GitHub
echo "☁️  推送到 GitHub..."
git push origin main

if [ $? -eq 0 ]; then
    echo "✅ 代码已上传！"
else
    echo "❌ 上传失败，请检查网络或 Token"
    exit 1
fi

# 如果指定了版本号，创建 Release 标签触发自动编译打包
if [ -n "$1" ]; then
    echo "🏷️  创建版本标签: $1"
    git tag -a "$1" -m "Release $1"
    git push origin "$1"
    echo "✅ Release $1 已创建，GitHub Actions 将自动编译打包 APK"
    echo "📱 查看进度: https://github.com/huaguugu886/TimeMemorial/actions"
else
    echo ""
    echo "💡 提示：运行 bash upload.sh v1.0.0 可创建 Release 并自动打包 APK"
    echo "📱 查看编译状态: https://github.com/huaguugu886/TimeMemorial/actions"
fi
