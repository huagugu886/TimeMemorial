export default async function() {
  const rt = globalThis.getRuntime ? globalThis.getRuntime() : (globalThis.runtime || globalThis.Runtime);
  if (!rt) return { error: "Runtime not available" };

  try {
    const result = await rt.exec("bash /sdcard/Download/TimeMemorial/push_release.sh", {
      cwd: "/sdcard/Download/TimeMemorial",
      timeout: 120000
    });
    return { success: true, output: result };
  } catch (e) {
    return { error: e.message || String(e) };
  }
}
