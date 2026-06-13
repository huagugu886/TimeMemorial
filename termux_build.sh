#!/bin/bash
# ============================================
# TimeMemorial Termux 编译环境搭建 + 一键编译
# 使用 proot-distro 方案避免 AAPT2 架构问题
# ============================================

set -e

echo "🚀 TimeMemorial 编译环境搭建脚本"
echo "=================================="
echo ""

# ============================================
# Step 1: 安装 proot-distro 和 Ubuntu
# ============================================
echo "📦 [Step 1] 安装 proot-distro..."
pkg update -y
pkg install proot-distro -y

echo "📦 [Step 1.5] 安装 Ubuntu..."
proot-distro install ubuntu || echo "Ubuntu 已安装，跳过"

echo ""

# ============================================
# Step 2: 在 Ubuntu 内配置编译环境
# ============================================
echo "📦 [Step 2] 配置 Ubuntu 编译环境..."

# 复制项目到 Ubuntu 可访问的位置
PROOT_UBUNTU="$PREFIX/var/lib/proot-distro/installed-rootfs/ubuntu"
PROJECT_IN_PROOT="$PROOT_UBUNTU/root/TimeMemorial"

if [ -d "/sdcard/Download/TimeMemorial" ]; then
    echo "   复制项目到 proot Ubuntu..."
    mkdir -p "$PROOT_UBUNTU/root"
    cp -r /sdcard/Download/TimeMemorial "$PROJECT_IN_PROOT"
fi

# 在 Ubuntu 内执行环境配置
proot-distro login ubuntu --bind /sdcard/Download/TimeMemorial:/root/TimeMemorial -- bash -c '
set -e
echo "📦 安装基础工具..."
apt update -y
apt install -y wget unzip openjdk-17-jdk-headless

echo ""
echo "📦 配置 Java 17..."
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export PATH=$JAVA_HOME/bin:$PATH

echo "   Java 版本："
java -version 2>&1 | head -1

echo ""
echo "📦 下载 Android SDK command-line tools..."
export ANDROID_HOME=/root/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools

# 下载最新 command-line tools
cd /tmp
if [ ! -f "commandlinetools-linux.zip" ]; then
    wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O commandlinetools-linux.zip
fi
unzip -qo commandlinetools-linux.zip -d $ANDROID_HOME/cmdline-tools/
mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest 2>/dev/null || true

export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "📦 接受 SDK 许可证..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true

echo "📦 安装 Android SDK 组件..."
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

echo ""
echo "✅ 环境配置完成！"
echo ""

# ============================================
# Step 3: 编译项目
# ============================================
echo "🔨 [Step 3] 开始编译..."
cd /root/TimeMemorial

# 编译
bash gradlew assembleDebug --no-daemon 2>&1

echo ""
echo "=================================="
echo "🎉 编译完成！"
echo "=================================="
'

# ============================================
# Step 4: 复制 APK 出来
# ============================================
APK_PATH="$PROJECT_IN_PROOT/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" /sdcard/Download/TimeMemorial-v1.0.0-debug.apk
    echo ""
    echo "✅ APK 已复制到：/sdcard/Download/TimeMemorial-v1.0.0-debug.apk"
    echo "   直接安装即可！"
else
    echo ""
    echo "❌ 编译失败，请查看上方日志"
fi
