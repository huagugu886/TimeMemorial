#!/usr/bin/env python3
"""TimeMemorial 详情弹窗增强补丁 - 替换 CSS + HTML + JS"""
import re

FILE = '/sdcard/Download/TimeMemorial/app/src/main/assets/home_page.html'

with open(FILE, 'r', encoding='utf-8') as f:
    html = f.read()

# ===== 1. 替换 CSS：详情弹窗样式 =====
old_css = re.search(
    r'/\*[\s=]*详情弹窗.*?\*/\s*\.detail-overlay\s*\{.*?\.btn-delete:active\s*\{\s*background:\s*#FEE2E2;\s*\}',
    html, re.DOTALL
)
if not old_css:
    # fallback: 按 .detail-overlay { 开始到 .btn-delete:active 结束
    old_css = re.search(
        r'\.detail-overlay\s*\{.*?\.btn-delete:active\s*\{\s*background:\s*#FEE2E2;\s*\}',
        html, re.DOTALL
    )

new_css = r'''/* ========== 详情弹窗（增强版） ========== */
.detail-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.5);
  display: flex; align-items: flex-end; justify-content: center;
  opacity: 0; pointer-events: none;
  transition: opacity 0.3s;
}
.detail-overlay.show { opacity: 1; pointer-events: auto; }
.detail-sheet {
  background: var(--card-bg); width: 100%; max-height: 90vh;
  border-radius: 24px 24px 0 0;
  overflow-y: auto; -webkit-overflow-scrolling: touch;
  transform: translateY(100%);
  transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1);
  padding-bottom: calc(24px + var(--safe-bottom));
}
.detail-overlay.show .detail-sheet { transform: translateY(0); }

/* 封面 */
.detail-cover {
  width: 100%; height: 240px; object-fit: cover; display: block;
  border-radius: 24px 24px 0 0;
  cursor: pointer; transition: filter 0.2s;
}
.detail-cover:active { filter: brightness(0.92); }
.detail-cover-placeholder {
  width: 100%; height: 240px; display: flex; align-items: center; justify-content: center;
  border-radius: 24px 24px 0 0; font-size: 72px; color: rgba(255,255,255,0.6);
}

/* 头部 */
.detail-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 20px 0; gap: 12px;
}
.detail-title {
  font-size: 22px; font-weight: 700; color: var(--text); flex: 1; line-height: 1.3;
}
.detail-close {
  width: 32px; height: 32px; border-radius: 50%; background: #F3F4F6;
  border: none; font-size: 18px; cursor: pointer; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary); transition: background 0.2s;
}
.detail-close:active { background: #E5E7EB; }

/* 信息区 */
.detail-info { padding: 16px 20px; }

/* 标签行 */
.detail-tag-row { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.detail-tag { padding: 5px 14px; border-radius: 12px; font-size: 12px; color: #fff; font-weight: 600; letter-spacing: 0.3px; }

/* 倒计时区域 */
.detail-countdown {
  text-align: center; padding: 20px 0 16px; position: relative;
}
.detail-countdown .big-num {
  font-size: 64px; font-weight: 900; line-height: 1;
  background: linear-gradient(135deg, var(--primary), #A78BFA);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.detail-countdown .big-unit {
  font-size: 18px; color: var(--text-secondary); margin-left: 4px; font-weight: 600;
}
.detail-countdown .countdown-sub {
  font-size: 13px; color: var(--text-secondary); margin-top: 6px;
}
.detail-countdown.is-today .big-num {
  font-size: 52px;
  background: linear-gradient(135deg, #FF6B9D, #FF9F43);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.detail-countdown.is-today .celebration {
  font-size: 28px; margin-top: 4px; display: block;
  animation: celebBounce 1.5s ease-in-out infinite;
}
@keyframes celebBounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
.detail-countdown.is-past .big-num {
  background: linear-gradient(135deg, #9CA3AF, #D1D5DB);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 信息卡片网格 */
.detail-cards {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-bottom: 16px;
}
.detail-card {
  background: #F8F9FA; border-radius: 14px; padding: 12px 10px; text-align: center;
}
.detail-card .card-icon { font-size: 20px; margin-bottom: 4px; }
.detail-card .card-label { font-size: 11px; color: var(--text-secondary); margin-bottom: 2px; }
.detail-card .card-value { font-size: 13px; color: var(--text); font-weight: 600; }

/* 信息行 */
.detail-row {
  display: flex; align-items: center; gap: 10px; padding: 12px 0;
  border-bottom: 1px solid #F3F4F6; font-size: 14px;
}
.detail-row:last-child { border-bottom: none; }
.detail-row .label { width: 70px; flex-shrink: 0; color: var(--text-secondary); }
.detail-row .value { color: var(--text); font-weight: 500; flex: 1; }

/* 描述框 */
.detail-desc-box {
  background: #F8F9FA; border-radius: 14px; padding: 16px;
  font-size: 14px; color: var(--text-secondary); line-height: 1.7;
  margin-top: 16px; border-left: 3px solid var(--primary);
}

/* 分割线 */
.detail-divider { height: 1px; background: #F3F4F6; margin: 4px 0; }

/* 操作按钮 */
.detail-actions { display: flex; gap: 12px; padding: 12px 20px 0; }
.btn-edit, .btn-delete {
  flex: 1; padding: 14px; border-radius: 14px; border: none;
  font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.btn-edit { background: rgba(124, 92, 252, 0.1); color: var(--primary); }
.btn-edit:active { background: rgba(124, 92, 252, 0.2); }
.btn-delete { background: #FFF0F0; color: #EF4444; }
.btn-delete:active { background: #FEE2E2; }'''

if old_css:
    html = html[:old_css.start()] + new_css + html[old_css.end():]
    print('✅ CSS 替换成功')
else:
    print('❌ CSS 未匹配到，跳过')

# ===== 2. 替换 HTML：详情弹窗结构 =====
old_html = re.search(
    r'<div class="detail-overlay" id="detailModal">.*?</div>\s*</div>\s*</div>',
    html, re.DOTALL
)

new_html = '''<div class="detail-overlay" id="detailModal">
  <div class="detail-sheet">
    <div id="detailCover"></div>
    <div class="detail-header">
      <div class="detail-title" id="detailTitle"></div>
      <button class="detail-close" id="detailClose">✕</button>
    </div>
    <div class="detail-info" id="detailInfo"></div>
    <div class="detail-actions">
      <button class="btn-edit" id="detailEditBtn">✏️ 编辑</button>
      <button class="btn-delete" id="detailDeleteBtn">🗑️ 删除</button>
    </div>
  </div>
</div>'''

if old_html:
    html = html[:old_html.start()] + new_html + html[old_html.end():]
    print('✅ HTML 替换成功')
else:
    print('❌ HTML 未匹配到，跳过')

# ===== 3. 替换 JS：openDetail 函数 =====
old_js = re.search(
    r'function openDetail\(id\)\s*\{.*?document\.getElementById\(\'detailModal\'\)\.classList\.add\(\'show\'\);\s*\}',
    html, re.DOTALL
)

new_js = r'''function openDetail(id) {
  const m = memorials.find(x => x.id === id);
  if (!m) return;
  currentDetailId = id;

  const cat = categoryMap[m.category] || categoryMap.other;
  const days = daysBetween(m.date);
  const absDays = Math.abs(days);

  // 计算年月
  const targetDate = new Date(m.date);
  const now = new Date();
  const diffMs = Math.abs(now - targetDate);
  const totalDays = Math.floor(diffMs / (1000*60*60*24));
  const years = Math.floor(totalDays / 365);
  const months = Math.floor((totalDays % 365) / 30);
  const remainDays = totalDays % 30;

  // 星期几
  const weekDays = ['日','一','二','三','四','五','六'];
  const weekDay = '周' + weekDays[targetDate.getDay()];

  // 状态判断
  const isToday = days === 0;
  const isPast = days > 0;
  const isFuture = days < 0;
  let statusText, statusEmoji;
  if (isToday) { statusText = '就是今天'; statusEmoji = '🎉'; }
  else if (isPast) { statusText = '已过去'; statusEmoji = '⏰'; }
  else { statusText = '即将到来'; statusEmoji = '⏳'; }

  // 相对时间描述
  let relativeText = '';
  if (isToday) relativeText = '今天就是这个日子！';
  else if (absDays === 1) relativeText = isPast ? '昨天刚过' : '明天就到';
  else if (absDays < 7) relativeText = isPast ? absDays + '天前' : '还有' + absDays + '天';
  else if (absDays < 30) relativeText = isPast ? Math.floor(absDays/7) + '周前' : '还有' + Math.floor(absDays/7) + '周';
  else if (absDays < 365) relativeText = isPast ? Math.floor(absDays/30) + '个月前' : '还有' + Math.floor(absDays/30) + '个月';
  else {
    const y = Math.floor(absDays/365);
    const rm = Math.floor((absDays%365)/30);
    relativeText = isPast ? y + '年' + (rm > 0 ? rm + '个月' : '') + '前' : '还有' + y + '年' + (rm > 0 ? rm + '个月' : '');
  }

  // 封面
  const detailPos = m.imagePosition != null ? m.imagePosition : 50;
  document.getElementById('detailCover').innerHTML = m.image
    ? '<img class="detail-cover" src="' + resolveImageSrc(m.image) + '" style="object-position:center ' + detailPos + '%;" alt="" onclick="openFullscreenViewer(this.src)">'
    : '<div class="detail-cover-placeholder" style="background:' + coverBg(m.id) + ';">' + cat.label.slice(0,2) + '</div>';

  // 标题
  document.getElementById('detailTitle').textContent = m.title;

  // 构建信息区域
  let infoHtml = '';

  // 倒计时区域
  const countdownClass = isToday ? 'is-today' : (isPast ? 'is-past' : '');
  infoHtml += '<div class="detail-countdown ' + countdownClass + '">';
  if (isToday) {
    infoHtml += '<span class="big-num">🎉</span>';
    infoHtml += '<div class="countdown-sub">今天就是这个纪念日！</div>';
    infoHtml += '<span class="celebration">🎊🎈🎂</span>';
  } else {
    infoHtml += '<span class="big-num">' + absDays + '</span>';
    infoHtml += '<span class="big-unit">' + (isPast ? '天前' : '天后') + '</span>';
    infoHtml += '<div class="countdown-sub">' + relativeText + '</div>';
  }
  infoHtml += '</div>';

  // 标签行
  infoHtml += '<div class="detail-tag-row">';
  infoHtml += '<span class="detail-tag" style="background:' + cat.color + '">' + cat.label + '</span>';
  if (m.favorite) infoHtml += '<span class="detail-tag" style="background:#EE5A24">❤️ 精选</span>';
  infoHtml += '</div>';

  // 信息卡片网格
  infoHtml += '<div class="detail-cards">';
  infoHtml += '<div class="detail-card"><div class="card-icon">📅</div><div class="card-label">日期</div><div class="card-value">' + formatDate(m.date) + '</div></div>';
  infoHtml += '<div class="detail-card"><div class="card-icon">📆</div><div class="card-label">星期</div><div class="card-value">' + weekDay + '</div></div>';
  infoHtml += '<div class="detail-card"><div class="card-icon">' + statusEmoji + '</div><div class="card-label">状态</div><div class="card-value">' + statusText + '</div></div>';
  infoHtml += '</div>';

  // 详细信息行
  if (years > 0 || months > 0) {
    infoHtml += '<div class="detail-row"><span class="label">⏳ 已过</span><span class="value">';
    if (years > 0) infoHtml += years + '年';
    if (months > 0) infoHtml += months + '个月';
    if (remainDays > 0) infoHtml += remainDays + '天';
    infoHtml += '</span></div>';
  }

  infoHtml += '<div class="detail-row"><span class="label">📊 天数</span><span class="value">' + absDays + '天（共' + totalDays + '天）</span></div>';

  // 下一个整百/整千天纪念日
  const nextMilestone = Math.ceil(totalDays / 100) * 100;
  if (nextMilestone > totalDays && nextMilestone - totalDays <= 100) {
    const daysToMilestone = nextMilestone - totalDays;
    const milestoneDate = new Date(now.getTime() + daysToMilestone * 86400000);
    infoHtml += '<div class="detail-row"><span class="label">🎯 下个</span><span class="value">第' + nextMilestone + '天 (' + formatDate(milestoneDate.toISOString().slice(0,10)) + ')</span></div>';
  }

  // 描述
  if (m.desc) {
    infoHtml += '<div class="detail-desc-box">📝 ' + m.desc + '</div>';
  }

  document.getElementById('detailInfo').innerHTML = infoHtml;

  document.body.dataset.scrollY = window.scrollY;
  document.body.style.top = -window.scrollY + 'px';
  document.body.classList.add('modal-open');
  document.getElementById('detailModal').classList.add('show');
}'''

if old_js:
    html = html[:old_js.start()] + new_js + html[old_js.end():]
    print('✅ JS openDetail 替换成功')
else:
    print('❌ JS openDetail 未匹配到，跳过')

# ===== 4. 替换关闭详情按钮事件（添加编辑按钮支持） =====
old_close = re.search(
    r'// 关闭详情\s*document\.getElementById\(\'detailClose\'\)\.addEventListener.*?currentDetailId = null;\s*\}\s*\);',
    html, re.DOTALL
)

new_close = r'''// 关闭详情
document.getElementById('detailClose').addEventListener('click', () => {
  document.getElementById('detailModal').classList.remove('show');
  document.body.classList.remove('modal-open');
  document.body.style.top = '';
  window.scrollTo(0, parseInt(document.body.dataset.scrollY || '0'));
  currentDetailId = null;
});
document.getElementById('detailModal').addEventListener('click', e => {
  if (e.target === document.getElementById('detailModal')) {
    document.getElementById('detailModal').classList.remove('show');
    document.body.classList.remove('modal-open');
    document.body.style.top = '';
    window.scrollTo(0, parseInt(document.body.dataset.scrollY || '0'));
    currentDetailId = null;
  }
});

// 编辑按钮
document.getElementById('detailEditBtn').addEventListener('click', () => {
  if (!currentDetailId) return;
  const m = memorials.find(x => x.id === currentDetailId);
  if (!m) return;
  // 关闭详情弹窗
  document.getElementById('detailModal').classList.remove('show');
  document.body.classList.remove('modal-open');
  document.body.style.top = '';
  window.scrollTo(0, parseInt(document.body.dataset.scrollY || '0'));
  currentDetailId = null;
  // 导航到编辑页面（如果Android端支持）
  if (window.Android && window.Android.navigateToEdit) {
    window.Android.navigateToEdit(m.id);
  } else {
    // 降级：打开内置编辑弹窗
    openAddModalForEdit(m);
  }
});'''

if old_close:
    html = html[:old_close.start()] + new_close + html[old_close.end():]
    print('✅ 关闭/编辑事件替换成功')
else:
    print('❌ 关闭事件未匹配到，跳过')

# 写回文件
with open(FILE, 'w', encoding='utf-8') as f:
    f.write(html)

print('\n🎉 补丁应用完成！请重新构建验证效果。')
