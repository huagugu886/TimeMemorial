#!/bin/bash
# 诊断脚本：检查 Termux 里的 settings.gradle 到底是什么
echo "=== Termux settings.gradle 内容 ==="
cat "$HOME/TimeMemorial/settings.gradle" 2>/dev/null || echo "(文件不存在)"
echo ""
echo "=== 文件行号 ==="
cat -n "$HOME/TimeMemorial/settings.gradle" 2>/dev/null || echo "(无)"
echo ""
echo "=== gradlew 版本 ==="
cd "$HOME/TimeMemorial" 2>/dev/null && ./gradlew --version 2>&1 | head -5 || echo "(无法运行)"
echo ""
echo "=== 系统 gradle 版本 ==="
gradle --version 2>&1 | head -5 || echo "(无)"
