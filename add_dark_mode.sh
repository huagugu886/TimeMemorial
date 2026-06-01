#!/bin/bash

# 时光纪念 App - 深色模式 CSS 注入脚本
# 为所有 HTML 页面添加 @media (prefers-color-scheme: dark) 支持

ASSETS_DIR="/sdcard/Download/TimeMemorial/app/src/main/assets"

echo "🌙 开始为 HTML 文件添加深色模式支持..."

# 定义深色模式 CSS
read -r -d '' DARK_CSS << 'EOF'
/* 🌙 深色模式适配 */
@media (prefers-color-scheme: dark) {
  :root {
    --bg: #1A1A2E;
    --surface: #16213E;
    --card: #0F3460;
    --card-hover: #1A4B8C;
    --text-primary: #EAEAEA;
    --text-secondary: #A0A0B0;
    --text-hint: #6C6C80;
    --border: #2A2A40;
    --divider: #2A2A40;
    --shadow: rgba(0, 0, 0, 0.3);
    --overlay: rgba(0, 0, 0, 0.7);
    --ripple: rgba(255, 255, 255, 0.1);
    
    /* 状态栏 */
    --status-bar-color: #1A1A2E;
  }
  
  /* 深色模式下的特定元素调整 */
  .header {
    background: linear-gradient(135deg, #2D1B69, #1A1A2E);
  }
  
  .modal {
    background: #16213E;
  }
  
  .modal-handle {
    background: #3A3A50;
  }
  
  /* 卡片在深色模式下的效果 */
  .memorial-card, .featured-card {
    background: #0F3460;
    border: 1px solid rgba(124, 92, 252, 0.1);
  }
  
  .memorial-card:hover, .featured-card:hover {
    background: #1A4B8C;
    border-color: rgba(124, 92, 252, 0.3);
  }
  
  /* 输入框深色适配 */
  input, textarea, select {
    background: #1A1A2E;
    color: #EAEAEA;
    border-color: #3A3A50;
  }
  
  input:focus, textarea:focus, select:focus {
    border-color: var(--primary);
    background: #16213E;
  }
  
  /* 标签深色适配 */
  .tag {
    background: rgba(124, 92, 252, 0.15);
    color: #B8A9FF;
  }
  
  .tag.active {
    background: var(--primary);
    color: #FFFFFF;
  }
  
  /* 按钮深色适配 */
  .btn-primary {
    background: var(--primary);
    box-shadow: 0 4px 15px rgba(124, 92, 252, 0.4);
  }
  
  /* 滚动条深色适配 */
  ::-webkit-scrollbar {
    background: #1A1A2E;
  }
  
  ::-webkit-scrollbar-thumb {
    background: #3A3A50;
    border-radius: 4px;
  }
  
  /* 空状态图标深色适配 */
  .empty-state svg {
    opacity: 0.6;
  }
  
  /* FAB 按钮深色适配 */
  .fab {
    box-shadow: 0 6px 20px rgba(124, 92, 252, 0.5);
  }
}
EOF

# 备份原文件
echo "📦 备份原始 HTML 文件..."
for file in "$ASSETS_DIR"/*.html; do
  if [[ -f "$file" && ! "$file" == *.bak ]]; then
    cp "$file" "${file}.bak"
  fi
done

# 函数：注入深色模式 CSS
inject_dark_css() {
  local file="$1"
  local filename=$(basename "$file")
  
  # 检查是否已注入
  if grep -q "prefers-color-scheme: dark" "$file"; then
    echo "  ⏭️  $filename 已包含深色模式，跳过"
    return
  fi
  
  # 找到 </style> 标签，在其前面插入深色模式 CSS
  if grep -q "</style>" "$file"; then
    sed -i "s|</style>|$DARK_CSS\n</style>|" "$file"
    echo "  ✅ $filename 已添加深色模式 CSS"
  else
    echo "  ⚠️  $filename 未找到 </style> 标签，需要手动处理"
  fi
}

# 处理所有 HTML 文件
echo ""
echo "🎨 注入深色模式 CSS..."
for file in "$ASSETS_DIR"/*.html; do
  if [[ -f "$file" && ! "$file" == *.bak ]]; then
    inject_dark_css "$file"
  fi
done

echo ""
echo "✨ 深色模式支持已添加完成！"
echo ""
echo "📋 后续步骤："
echo "   1. 重新编译 App：cd /sdcard/Download/TimeMemorial && ./gradlew assembleDebug"
echo "   2. 安装 APK 到手机测试"
echo "   3. 在设置中切换深色模式查看效果"
echo ""
echo "💡 提示：深色模式颜色会跟随系统设置自动切换"
EOF
