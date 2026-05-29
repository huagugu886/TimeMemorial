export default async function() {
  // Try Rhino-style Java access
  try {
    const rt = Packages.java.lang.Runtime.getRuntime();
    const proc = rt.exec(["bash", "-c", "echo hello_rhino"]);
    const is = proc.getInputStream();
    const bytes = [];
    let b;
    while ((b = is.read()) !== -1) bytes.push(b);
    const output = java.lang.String(bytes, "UTF-8").toString();
    proc.waitFor();
    return { method: "rhino_java", output: output };
  } catch(e) {
    return { error: "rhino failed: " + e.message };
  }
}
