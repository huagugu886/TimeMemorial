#!/bin/bash
# TimeMemorial 原生编译脚本 v4
set -e

echo "🔧 TimeMemorial 原生编译脚本 v4"
echo "============================================"

# ==================== 环境检查 ====================
echo ""
echo "🔍 环境检查："
echo "   Java: $(java -version 2>&1 | head -1)"
echo "   aapt2: $(aapt2 version 2>&1 | head -1)"
echo "   JAVA_HOME=$JAVA_HOME"

# ==================== Step 1: 检查编译依赖 ====================
echo ""
echo "📦 [Step 1] 检查编译依赖..."
pkg update -y
pkg install -y aapt2 2>/dev/null || true

if ! command -v java &>/dev/null; then
    pkg install -y openjdk-21
fi

echo "   ✅ 依赖检查完成"

# ==================== Step 2: 复制项目到 Termux ====================
echo ""
echo "📦 [Step 2] 复制项目到 Termux 内部存储..."
PROJECT_SRC="/sdcard/Download/TimeMemorial"
PROJECT_DIR="$HOME/TimeMemorial"

# 用 root 清理旧目录并复制
su -c "rm -rf '$PROJECT_DIR'" 2>/dev/null || true
su -c "cp -r '$PROJECT_SRC' '$PROJECT_DIR' && chmod -R 777 '$PROJECT_DIR'"

# 删除 settings.gradle.kts（Termux Gradle 不支持 Kotlin DSL）
rm -f "$PROJECT_DIR/settings.gradle.kts"
echo "   ✅ 已复制项目并删除 .kts 文件"

# ==================== Step 3: 复制 SDK 到 Termux ====================
echo ""
echo "📦 [Step 3] 复制 Android SDK 到 Termux..."
SDK_SRC="/sdcard/Download/android-sdk"
SDK_DIR="$HOME/android-sdk"

if [ -d "$SDK_DIR/build-tools/33.0.1" ] && [ -d "$SDK_DIR/platforms/android-34" ]; then
    echo "   ✅ SDK 已存在，跳过复制"
else
    echo "   复制 SDK..."
    su -c "rm -rf '$SDK_DIR'" 2>/dev/null || true
    su -c "mkdir -p '$SDK_DIR'"
    su -c "cp -r '$SDK_SRC/build-tools' '$SDK_DIR/' && chmod -R 777 '$SDK_DIR/build-tools'"
    echo "   ✅ build-tools 已复制"
    su -c "cp -r '$SDK_SRC/platforms' '$SDK_DIR/' && chmod -R 777 '$SDK_DIR/platforms'"
    echo "   ✅ platforms 已复制"
    su -c "cp -r '$SDK_SRC/platform-tools' '$SDK_DIR/' && chmod -R 777 '$SDK_DIR/platform-tools'" 2>/dev/null || true
    su -c "cp -r '$SDK_SRC/licenses' '$SDK_DIR/' 2>/dev/null && chmod -R 777 '$SDK_DIR/licenses'" 2>/dev/null || true
    echo "   ✅ SDK 复制完成"
fi

# 更新 local.properties
echo "sdk.dir=$HOME/android-sdk" > "$PROJECT_DIR/local.properties"
echo "   ✅ local.properties 已更新"

# ==================== Step 4: 编译 ====================
echo ""
echo "🔨 [Step 4] 开始编译..."
cd "$PROJECT_DIR"

# 确保 gradlew 可执行
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    echo "   使用 gradlew (Gradle 8.9) 编译..."
    ./gradlew assembleRelease --no-daemon --no-build-cache 2>&1 || {
        echo ""
        echo "⚠️ assembleRelease 失败，尝试 assembleDebug..."
        ./gradlew assembleDebug --no-daemon --no-build-cache 2>&1
    }
else
    echo "   ⚠️ 未找到 gradlew，使用系统 gradle..."
    gradle assembleRelease --no-daemon --no-build-cache 2>&1 || {
        echo ""
        echo "⚠️ assembleRelease 失败，尝试 assembleDebug..."
        gradle assembleDebug --no-daemon --no-build-cache 2>&1
    }
fi

# ==================== Step 5: 提取 APK ====================
echo ""
echo "📋 [Step 5] 查找编译产物..."
APK_PATH=$(find "$PROJECT_DIR" -name "*.apk" -type f 2>/dev/null | head -5)

if [ -n "$APK_PATH" ]; then
    echo "   找到 APK："
    echo "$APK_PATH" | while read apk; do
        SIZE=$(du -h "$apk" 2>/dev/null | cut -f1)
        echo "   📦 $apk ($SIZE)"
    done
    
    mkdir -p /sdcard/Download/TimeMemorial/output
    echo "$APK_PATH" | while read apk; do
        cp "$apk" /sdcard/Download/TimeMemorial/output/
    done
    echo ""
    echo "✅ APK 已复制到 /sdcard/Download/TimeMemorial/output/"
    echo ""
    echo "🎉 编译完成！"
else
    echo "   ❌ 未找到 APK 文件"
fi
