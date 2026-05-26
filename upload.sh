
#!/bin/bash

cd /sdcard/Download/TimeMemorial



TOKEN="YOUR_TOKEN_HERE"



FILE_B64=$(base64 -w 0 gradle/wrapper/gradle-wrapper.jar)



curl -s -X PUT \

  -H "Authorization: token $TOKEN" \

  -H "Content-Type: application/json" \

  "https://api.github.com/repos/huaguugu886/TimeMemorial/contents/gradle/wrapper/gradle-wrapper.jar" \

  -d "{\"message\":\"fix: add gradle-wrapper.jar\",\"content\":\"$FILE_B64\",\"branch\":\"main\"}"

