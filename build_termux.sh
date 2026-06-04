#!/bin/bash
# ============================================
# TimeMemorial Termux 原生编译 v5
# 核心：替换 AGP Maven 下载的 x86_64 aapt2 → Termux ARM64 原生版
# ============================================
set -e

echo "🚀 TimeMemorial 原生编译 v5"
echo "============================"
echo ""

PROJECT_DIR="$HOME/TimeMemorial"
ANDROID_SDK="$HOME/android-sdk"

# ============================================
# Step 1: 安装依赖
# ============================================
echo "📦 [Step 1] 安装编译依赖..."
pkg update -y 2>/dev/null || true
pkg install -y aapt2 2>/dev/null || true

# 确认 aapt2 可用
TERMUX_AAPT2=$(which aapt2 2>/dev/null)
if [ -z "$TERMUX_AAPT2" ]; then
    echo "❌ aapt2 安装失败！"
    exit 1
fi
echo "   ✅ aapt2: $TERMUX_AAPT2 ($($TERMUX_AAPT2 version 2>&1 | head -1))"

# 安装 JDK 17
if ! java -version 2>&1 | grep -q "17"; then
    echo "   安装 OpenJDK 17..."
    pkg install -y openjdk-17 2>/dev/null || pkg install -y openjdk-17-jdk 2>/dev/null
fi
echo "   ✅ Java: $(java -version 2>&1 | head -1)"
echo ""

# ============================================
# Step 2: 复制项目
# ============================================
echo "📁 [Step 2] 复制项目到 Termux..."
rm -rf "$PROJECT_DIR"
cp -r /sdcard/Download/TimeMemorial "$PROJECT_DIR"
rm -f "$PROJECT_DIR/settings.gradle.kts"  # Termux Gradle 不支持 Kotlin DSL settings
echo "   ✅ 项目就绪"
echo ""

# ============================================
# Step 3: 复制 Android SDK
# ============================================
echo "📱 [Step 3] 检查 Android SDK..."
if [ ! -d "$ANDROID_SDK/platforms/android-34" ]; then
    echo "   复制 SDK..."
    mkdir -p "$ANDROID_SDK"
    su -c "cp -r /sdcard/Download/android-sdk/build-tools $ANDROID_SDK/"
    su -c "cp -r /sdcard/Download/android-sdk/platforms $ANDROID_SDK/"
    su -c "cp -r /sdcard/Download/android-sdk/platform-tools $ANDROID_SDK/"
    su -c "cp -r /sdcard/Download/android-sdk/licenses $ANDROID_SDK/"
    su -c "chown -R \$(whoami):\$(whoami) $ANDROID_SDK"
    echo "   ✅ SDK 复制完成"
else
    echo "   ✅ SDK 已存在"
fi
echo "sdk.dir=$ANDROID_SDK" > "$PROJECT_DIR/local.properties"
echo ""

# ============================================
# Step 4: 配置 Gradle
# ============================================
echo "⚙️ [Step 4] 配置 Gradle..."
cat > "$PROJECT_DIR/gradle.properties" << 'EOF'
org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
EOF
echo "   ✅ gradle.properties 已写入"
echo ""

# ============================================
# Step 5: 首次编译（让 Gradle 下载依赖和 aapt2）
# ============================================
echo "🔨 [Step 5] 首次编译（下载依赖）..."
cd "$PROJECT_DIR"
chmod +x gradlew

export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH="$JAVA_HOME/bin:$PATH"

# 先执行一次，让 Gradle 下载所有依赖（包括 x86_64 aapt2）
# 这次会因 aapt2 架构不兼容而失败，但依赖已下载好
echo "   下载依赖中（会失败，属正常）..."
./gradlew assembleDebug --no-daemon 2>&1 | tail -20 || true
echo ""

# ============================================
# Step 6: 替换 x86_64 aapt2 → Termux ARM64 原生版
# ============================================
echo "🔧 [Step 6] 替换 aapt2 为 Termux 原生版..."

# 查找 AGP 缓存的 aapt2 二进制文件
AAPT2_CACHED=$(find "$HOME/.gradle" -path "*/aapt2*" -name "aapt2" -type f 2>/dev/null | head -5)

if [ -z "$AAPT2_CACHED" ]; then
    echo "   ⚠️ 未在 .gradle 中找到 aapt2，扩大搜索..."
    AAPT2_CACHED=$(find "$HOME/.gradle" -name "aapt2" -type f 2>/dev/null | head -5)
fi

if [ -n "$AAPT2_CACHED" ]; then
    echo "   找到以下 aapt2 文件："
    echo "$AAPT2_CACHED" | while read f; do
        echo "   → $f"
    done
    
    # 逐个替换
    echo "$AAPT2_CACHED" | while read f; do
        if file "$f" 2>/dev/null | grep -q "ELF"; then
            # 备份原文件
            mv "$f" "${f}.bak_x86_64" 2>/dev/null || true
            # 创建指向 Termux 原生 aapt2 的 wrapper
            cat > "$f" << AAPT2WRAP
#!/bin/bash
exec $TERMUX_AAPT2 "\$@"
AAPT2WRAP
            chmod +x "$f"
            echo "   ✅ 已替换: $f"
        fi
    done
else
    echo "   ❌ 未找到任何 aapt2 缓存文件"
    echo "   尝试直接在 build-tools 目录查找..."
    find "$ANDROID_SDK" -name "aapt2" -type f 2>/dev/null | while read f; do
        echo "   → $f"
    done
fi
echo ""

# ============================================
# Step 7: 正式编译
# ============================================
echo "🔨 [Step 7] 正式编译..."
echo ""

# 尝试 Release
echo "📦 assembleRelease..."
if ./gradlew assembleRelease --no-daemon --stacktrace 2>&1 | tail -30; then
    echo "✅ Release 编译成功！"
    BUILD_TYPE="release"
else
    echo ""
    echo "⚠️ Release 失败，尝试 Debug..."
    echo ""
    if ./gradlew assembleDebug --no-daemon --stacktrace 2>&1 | tail -30; then
        echo "✅ Debug 编译成功！"
        BUILD_TYPE="debug"
    else
        echo ""
        echo "❌ 编译失败"
        echo ""
        echo "=== 诊断信息 ==="
        echo "Java: $(java -version 2>&1 | head -1)"
        echo "AAPT2: $($TERMUX_AAPT2 version 2>&1)"
        echo "Gradle version:"
        ./gradlew --version 2>&1 | head -5
        echo ""
        echo "已缓存的 aapt2 文件:"
        find "$HOME/.gradle" -name "aapt2" -type f 2>/dev/null | head -10
        exit 1
    fi
fi

# ============================================
# Step 8: 提取 APK
# ============================================
echo ""
echo "📦 [Step 8] 提取 APK..."
OUTPUT_DIR="/sdcard/Download/TimeMemorial/output"
mkdir -p "$OUTPUT_DIR"

APK_FOUND=0
for apk in $(find "$PROJECT_DIR/app/build/outputs/apk" -name "*.apk" 2>/dev/null); do
    SIZE=$(du -h "$apk" | cut -f1)
    echo "   ✅ $(basename $apk) ($SIZE)"
    cp "$apk" "$OUTPUT_DIR/"
    APK_FOUND=1
done

if [ "$APK_FOUND" -eq 1 ]; then
    echo ""
    echo "=================================="
    echo "🎉 编译完成！APK 在："
    ls -lh "$OUTPUT_DIR/"*.apk 2>/dev/null
    echo "=================================="
    echo ""
    echo "安装："
    echo "  termux-open $OUTPUT_DIR/*.apk"
else
    echo "❌ 未找到 APK"
    exit 1
fi
