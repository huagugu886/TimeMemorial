import re

path = "app/src/main/java/com/timememorial/app/MainActivity.kt"
with open(path, "r") as f:
    content = f.read()

old = re.search(r'private fun setupBottomNavAppearance\(\) \{.*?\n    \}', re.DOTALL)
if not old:
    print("ERROR: method not found!")
    exit(1)

new = '''private fun setupBottomNavAppearance() {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val density = resources.displayMetrics.density
        val themeColor = getThemeColor(this)

        // 胶囊背景 — GradientDrawable 干净绘制
        val capsuleColor = if (night) Color.parseColor("#1A1A1A") else Color.parseColor("#F0F0F5")
        val capsule = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 28 * density
            setColor(capsuleColor)
        }
        binding.bottomNav.background = capsule

        // 圆角裁剪 + 阴影
        binding.bottomNav.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val rect = RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())
                val path = Path()
                val radius = 28 * density
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
                outline.setPath(path, 0f)
            }
        }
        binding.bottomNav.clipToOutline = true
        binding.bottomNav.elevation = 8 * density

        // 图标和文字颜色
        val isNightBlack = night && themeColor == Color.parseColor("#000000")
        val isNightBlue = night && themeColor == Color.parseColor("#0A84FF")
        val accentInt = when {
            isNightBlack -> Color.WHITE
            isNightBlue -> Color.WHITE
            night -> when (themeColor) {
                Color.parseColor("#000000") -> Color.WHITE
                Color.parseColor("#0A84FF") -> Color.WHITE
                else -> Color.WHITE
            }
            else -> themeColor
        }
        val inactiveInt = if (night) Color.parseColor("#8899AA") else Color.parseColor("#999999")
        val indicatorBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            setColor(accentInt)
        }
        binding.bottomNav.itemActiveIndicator = indicatorBg
        binding.bottomNav.itemActiveIndicatorHeight = (32 * density).toInt()
        binding.bottomNav.itemActiveIndicatorWidth = (56 * density).toInt()
        binding.bottomNav.itemActiveIndicatorMarginHorizontal = (4 * density).toInt()

        binding.bottomNav.itemIconTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accentInt, inactiveInt)
        )
        binding.bottomNav.itemTextColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accentInt, inactiveInt)
        )
    }'''

content = content[:old.start()] + new + content[old.end():]
with open(path, "w") as f:
    f.write(content)
print("Kotlin fixed!")
