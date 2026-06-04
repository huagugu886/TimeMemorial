#!/bin/bash
# ============================================
# TimeMemorial 编译修复脚本
# 修复：mipmap 缺失 + Firebase 依赖 + SDK 版本
# ============================================

set -e
cd "$(dirname "$0")"

echo "🔧 开始修复 TimeMemorial 项目..."
echo ""

# ============================================
# 1. 创建 mipmap 图标目录和占位图标
# ============================================
echo "📦 [1/5] 创建 mipmap 图标资源..."

RES_DIR="app/src/main/res"

# 密度列表
DENSITIES=("mdpi" "hdpi" "xhdpi" "xxhdpi" "xxxhdpi")
SIZES=(48 72 96 144 192)

for i in "${!DENSITIES[@]}"; do
    DENSITY="${DENSITIES[$i]}"
    SIZE="${SIZES[$i]}"
    DIR="$RES_DIR/mipmap-${DENSITY}"
    mkdir -p "$DIR"

    # 用 Python 生成简单的纯色圆形 PNG 图标
    python3 -c "
import struct, zlib, os

size = $SIZE
# 创建一个紫色圆形图标 (和 app 主题色 #6C63FF 一致)
pixels = []
cx, cy = size // 2, size // 2
r = size // 2 - 2
for y in range(size):
    row = []
    for x in range(size):
        dx, dy = x - cx, y - cy
        if dx*dx + dy*dy <= r*r:
            # 紫色圆形
            row.extend([108, 99, 255, 255])
        else:
            # 透明
            row.extend([0, 0, 0, 0])
    pixels.append(bytes([0] + row))  # filter byte

raw = b''.join(pixels)

def make_png(w, h, raw):
    def chunk(ctype, data):
        c = ctype + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
    sig = b'\x89PNG\r\n\x1a\n'
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    idat = zlib.compress(raw)
    return sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')

png = make_png(size, size, raw)
with open('$DIR/ic_launcher.png', 'wb') as f:
    f.write(png)
with open('$DIR/ic_launcher_round.png', 'wb') as f:
    f.write(png)
"
    echo "   ✅ mipmap-${DENSITY} (${SIZE}x${SIZE})"
done

echo ""

# ============================================
# 2. 修复 build.gradle.kts - 去掉 Firebase
# ============================================
echo "📦 [2/5] 移除 Firebase 依赖..."

# 备份原文件
cp app/build.gradle.kts app/build.gradle.kts.bak

# 用 Python 精确替换
python3 << 'PYEOF'
import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

# 1. compileSdk 35 → 34
content = content.replace('compileSdk = 35', 'compileSdk = 34')
content = content.replace('targetSdk = 35', 'targetSdk = 34')

# 2. 删除 Firebase 相关依赖
lines = content.split('\n')
new_lines = []
in_firebase_block = False
for line in lines:
    stripped = line.strip()
    # 跳过 Firebase 注释和依赖行
    if '// Firebase' in stripped:
        in_firebase_block = True
        continue
    if in_firebase_block:
        if 'firebase' in stripped.lower():
            continue
        else:
            in_firebase_block = False
    # 也跳过遗漏的 firebase 行
    if 'firebase' in stripped.lower():
        continue
    new_lines.append(line)

content = '\n'.join(new_lines)

# 清理多余空行
content = re.sub(r'\n{3,}', '\n\n', content)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)

print("   ✅ build.gradle.kts 已修复")
PYEOF

echo ""

# ============================================
# 3. 修复 settings.gradle.kts
# ============================================
echo "📦 [3/5] 修复 settings.gradle.kts..."

cp settings.gradle.kts settings.gradle.kts.bak

python3 << 'PYEOF'
with open('settings.gradle.kts', 'r') as f:
    content = f.read()

# dependencyResolution → dependencyResolutionManagement
content = content.replace('dependencyResolution {', 'dependencyResolutionManagement {')

with open('settings.gradle.kts', 'w') as f:
    f.write(content)

print("   ✅ settings.gradle.kts 已修复")
PYEOF

echo ""

# ============================================
# 4. 修复 TimeMemorialApp.kt - 移除 Firebase 初始化
# ============================================
echo "📦 [4/5] 移除 Firebase 初始化代码..."

APP_KT="app/src/main/java/com/timememorial/app/TimeMemorialApp.kt"
cp "$APP_KT" "${APP_KT}.bak"

cat > "$APP_KT" << 'KOTLIN'
package com.timememorial.app

import android.app.Application

class TimeMemorialApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
KOTLIN

echo "   ✅ TimeMemorialApp.kt 已修复"

echo ""

# ============================================
# 5. 汇总
# ============================================
echo "=========================================="
echo "🎉 修复完成！修改汇总："
echo "=========================================="
echo ""
echo "  1. ✅ 创建 mipmap 图标（5个密度的占位图标）"
echo "  2. ✅ build.gradle.kts:"
echo "     - compileSdk 35 → 34"
echo "     - targetSdk 35 → 34"
echo "     - 移除 Firebase BOM + auth/firestore/storage"
echo "  3. ✅ settings.gradle.kts:"
echo "     - dependencyResolution → dependencyResolutionManagement"
echo "  4. ✅ TimeMemorialApp.kt:"
echo "     - 移除 FirebaseApp.initializeApp()"
echo ""
echo "⚠️  备份文件已保存为 .bak 后缀"
echo ""
echo "下一步：在 Termux 中运行以下命令编译："
echo ""
echo "  cd ~/TimeMemorial"
echo "  ./gradlew assembleDebug"
echo ""
echo "或使用 proot 方案（推荐，避免 AAPT2 架构问题）："
echo ""
echo "  pkg install proot-distro"
echo "  proot-distro install ubuntu"
echo "  proot-distro login ubuntu"
echo "  # 在 Ubuntu 中安装 JDK 17 + Android SDK 后编译"
echo ""
