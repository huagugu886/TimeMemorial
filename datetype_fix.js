
// ========== 农历/公历切换 ==========
let currentDateType = 'solar';
function toggleDateType() {
  currentDateType = currentDateType === 'solar' ? 'lunar' : 'solar';
  document.getElementById('dateTypeLabel').textContent = 
    currentDateType === 'lunar' ? '🌙 农历' : '📅 公历';
}
