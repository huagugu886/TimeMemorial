// push_now.js - 用于执行推送脚本
const { execSync } = require('child_process');

try {
  console.log('=== 开始推送到 GitHub ===');
  
  // 执行 push_release.sh
  const result = execSync('cd /sdcard/Download/TimeMemorial && bash push_release.sh', {
    encoding: 'utf8',
    timeout: 60000
  });
  
  console.log(result);
  console.log('✅ 推送完成！');
} catch (error) {
  console.error('❌ 推送失败:', error.message);
  if (error.stdout) console.log('stdout:', error.stdout);
  if (error.stderr) console.log('stderr:', error.stderr);
}
