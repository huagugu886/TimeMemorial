export default async function () {
  const Runtime = Java.type('java.lang.Runtime');
  const BufferedReader = Java.type('java.io.BufferedReader');
  const InputStreamReader = Java.type('java.io.InputStreamReader');
  
  const runtime = Runtime.getRuntime();
  const proc = runtime.exec(new Java.array('java.lang.String', [
    '/system/bin/sh', '-c',
    'cd /sdcard/Download/TimeMemorial && bash push_release.sh 2>&1'
  ]));
  
  const reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
  let output = '';
  let line;
  while ((line = reader.readLine()) != null) {
    output += line + '\n';
  }
  
  const exitCode = proc.waitFor();
  return { exitCode, output: output.substring(output.length - 3000) };
}
