path = '/sdcard/Download/TimeMemorial/app/src/main/assets/calendar_page.html'
with open(path, 'r') as f:
    lines = f.readlines()

# 在第865行(html +=)前插入提醒判断
insert = "    const isReminding = !single && diff > 0 && diff <= (ev.remindDays || 3);\n    if (isReminding) labelText += ' \U0001f514';\n"

# 找到第865行的 html += (模板字面量)
target = 864  # 0-indexed = 第865行
lines.insert(target, insert)

with open(path, 'w') as f:
    f.writelines(lines)
print("Bug #4 fixed!")
