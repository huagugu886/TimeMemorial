export default async function() {
  // Try Java Runtime
  try {
    const Runtime = java.lang.Runtime;
    const rt = Runtime.getRuntime();
    const proc = rt.exec("echo hello_from_java");
    const scanner = java.util.Scanner;
    const s = new scanner(proc.getInputStream()).useDelimiter("\\A");
    const output = s.hasNext() ? s.next() : "";
    proc.waitFor();
    return { method: "java_runtime", output: output.trim() };
  } catch(e) {
    return { error: "java_runtime failed: " + e.message };
  }
}
