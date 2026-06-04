// test_shell.js - 测试是否可以执行 shell 命令
export default async function() {
  try {
    // 尝试使用 Android 的 Runtime.exec()
    const process = Runtime.getRuntime().exec("echo hello");
    const reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
    const line = reader.readLine();
    console.log("Shell output: " + line);
    return { success: true, output: line };
  } catch (e) {
    console.log("Runtime.exec not available: " + e.message);
    
    try {
      // 尝试使用 ProcessBuilder
      const pb = new java.lang.ProcessBuilder("echo", "hello");
      const process = pb.start();
      const reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
      const line = reader.readLine();
      console.log("ProcessBuilder output: " + line);
      return { success: true, output: line };
    } catch (e2) {
      console.log("ProcessBuilder not available: " + e2.message);
      return { success: false, error: e2.message };
    }
  }
}
