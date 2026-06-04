# TimeMemorial 开发文档

> **版本**: V2.0.117
> **包名**: `com.timememorial.app`
> **最低 Android 版本**: 8.0 (API 26)
> **目标 Android 版本**: 14 (API 34)
> **编译 SDK**: 35
> **JVM**: 17
> **架构**: Kotlin + WebView 混合架构

---

## 1. 项目概述

TimeMemorial（时光纪念）是一款 Android 纪念日管理应用。采用 **原生 Kotlin + WebView 混合架构**，核心 UI 由 HTML/CSS/JS 渲染，原生层负责数据持久化、系统通知、图片存储等能力。

### 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| UI 渲染 | WebView + HTML/CSS/JS | 三个页面各自独立 HTML 文件 |
| 原生桥接 | JavascriptInterface | `WebBridge` / `DarkModeBridge` |
| 数据持久化 | JSON 文件 + SharedPreferences | `AnniversaryRepository` 单源存储 |
| 通知提醒 | AlarmManager + BroadcastReceiver | 每日定时检查 + 系统通知 |
| 导航 | Navigation Component | 底部三 Tab 切换 |
| 构建 | Gradle Kotlin DSL | 自动版本号递增 |

### 依赖库

```
androidx.core:core-ktx:1.15.0
androidx.appcompat:appcompat:1.7.0
com.google.android.material:material:1.12.0
androidx.constraintlayout:constraintlayout:2.2.1
androidx.fragment:fragment-ktx:1.8.6
androidx.navigation:navigation-fragment-ktx:2.8.9
androidx.navigation:navigation-ui-ktx:2.8.9
androidx.webkit:webkit:1.12.1
```

---

## 2. 项目结构

```
app/src/main/
├── AndroidManifest.xml
├── assets/                          # WebView 前端资源
│   ├── home_page.html               # 首页（纪念日列表 + 添加/编辑/详情弹窗）
│   ├── calendar_page.html           # 日历页（月历视图 + 事件卡片）
│   └── settings_page.html           # 设置页（主题/深色模式/提醒/备份恢复）
├── java/com/timememorial/app/
│   ├── MainActivity.kt              # 主 Activity，底部导航 + 沉浸式状态栏
│   ├── TimeMemorialApp.kt           # Application，启动时恢复深色模式
│   ├── data/
│   │   ├── model/
│   │   │   └── Anniversary.kt       # 纪念日数据模型
│   │   └── local/
│   │       ├── AppDatabase.kt       # 数据库门面（单例）
│   │       ├── AnniversaryDao.kt    # DAO，委托给 Repository
│   │       └── AnniversaryRepository.kt  # 核心存储层（JSON 文件 + Prefs 同步）
│   ├── reminder/
│   │   ├── ReminderManager.kt       # AlarmManager 调度
│   │   ├── ReminderReceiver.kt      # 广播接收器，触发通知
│   │   ├── ReminderSettings.kt      # 提醒配置读写
│   │   └── BootReceiver.kt          # 开机重新调度提醒
│   └── ui/
│       ├── FragmentExtensions.kt    # WebView 返回键扩展函数
│       ├── home/
│       │   ├── HomeFragment.kt      # 首页 Fragment，WebView + 文件选择
│       │   └── WebBridge.kt         # JS 桥接：图片存取/IPC/纪念日同步
│       ├── calendar/
│       │   └── CalendarFragment.kt  # 日历 Fragment
│       ├── settings/
│       │   └── SettingsFragment.kt  # 设置 Fragment + DarkModeBridge
│       └── add/
│           └── (新增纪念日 Activity)
├── res/
│   ├── layout/                      # Fragment 布局（WebView 容器）
│   ├── navigation/nav_graph.xml     # 导航图
│   ├── menu/bottom_nav_menu.xml     # 底部导航菜单
│   ├── values/                      # 颜色/字符串/主题
│   ├── values-night/                # 深色模式资源
│   ├── color/                       # 状态选择器
│   ├── drawable/                    # 图标/背景
│   ├── mipmap-*/                    # 启动图标
│   └── xml/file_paths.xml           # FileProvider 路径配置
```

---

## 3. 数据模型

### Anniversary

```kotlin
data class Anniversary(
    val id: Long = 0,              // 唯一 ID（时间戳生成）
    val title: String,             // 纪念日名称
    val date: String,              // 日期（yyyy-MM-dd）
    val category: String,          // 分类（love/birthday/travel/work/other/favorite/collection + 自定义）
    val repeatYearly: Boolean = true, // 是否每年重复
    val reminderDays: Int = 3,     // 提前提醒天数
    val photoUri: String? = null,  // 图片文件名（images/ 目录下）
    val note: String? = null,      // 备注
    val createdAt: Long = System.currentTimeMillis()
)
```

### 存储格式（anniversaries.json）

```json
[
  {
    "id": 1717300000000,
    "title": "结婚纪念日",
    "date": "2020-06-15",
    "category": "love",
    "repeatYearly": true,
    "reminderDays": 3,
    "photoUri": "abc123.jpg",
    "note": "领证那天",
    "favorite": false
  }
]
```

---

## 4. 核心模块详解

### 4.1 数据存储层 — AnniversaryRepository

**文件**: `data/local/AnniversaryRepository.kt`

**设计原则**: 单源存储，所有数据读写统一走此模块。

| 方法 | 说明 |
|------|------|
| `getAll(context)` | 读取所有纪念日，按日期排序 |
| `getById(context, id)` | 按 ID 查询 |
| `getByCategory(context, category)` | 按分类筛选 |
| `writeAll(context, items)` | 全量写入（WebBridge syncAnniversaries 调用） |
| `insert(context, item)` | 新增一条，返回生成的 ID |
| `update(context, item)` | 更新一条 |
| `deleteById(context, id)` | 按 ID 删除 |
| `getUpcomingReminders(context, daysBefore)` | 获取即将到来的纪念日（供提醒使用） |

**双写机制**:
- 主存储: `filesDir/anniversaries.json`
- 同步副本: `SharedPreferences("reminder_settings").anniversaries`（供提醒模块读取）

### 4.2 JS 桥接层 — WebBridge

**文件**: `ui/home/WebBridge.kt`

**注册方式**: `addJavascriptInterface(WebBridge(...), "nativeBridge")`

| JS 接口 | 功能 |
|---------|------|
| `nativeBridge.saveImage(base64)` | 保存 base64 图片到 `filesDir/images/`，返回文件名 |
| `nativeBridge.getImagePath(fileName)` | 获取图片绝对路径 |
| `nativeBridge.getImageBase64(fileName)` | 获取图片 base64（含 data URL 前缀） |
| `nativeBridge.deleteImage(fileName)` | 删除图片文件 |
| `nativeBridge.cleanOrphanImages(activeNames)` | 清理不在列表中的孤儿图片 |
| `nativeBridge.syncAnniversaries(json)` | 全量同步纪念日数据到 Repository |
| `nativeBridge.getUpcomingReminders()` | 获取待提醒列表 JSON |
| `nativeBridge.getReminderSettings()` | 获取提醒配置 |
| `nativeBridge.saveReminderSettings(json)` | 保存提醒配置并调度闹钟 |
| `nativeBridge.writeIpcFile(path, content)` | 写文件（Termux IPC 通道） |
| `nativeBridge.readIpcFile(path)` | 读文件 |
| `nativeBridge.deleteIpcFile(path)` | 删文件 |
| `nativeBridge.setBottomNavVisibility(visible)` | 控制底栏显隐 |

### 4.3 设置页桥接 — DarkModeBridge

**文件**: `ui/settings/SettingsFragment.kt` 内部类

| JS 接口 | 功能 |
|---------|------|
| `nativeBridge.setDarkMode(enabled)` | 切换深色模式（同步 SharedPreferences + AppCompatDelegate） |
| `nativeBridge.exportData()` | 导出纪念日为 JSON 文件到 Downloads 目录 |
| `nativeBridge.setThemeColor(name)` | 切换主题色（purple/blue/pink/rose/teal/red） |
| `nativeBridge.saveReminderSettings(json)` | 保存提醒配置 |
| `nativeBridge.restoreData(json)` | 从备份文件恢复数据 |
| `nativeBridge.getCacheSize()` | 获取缓存大小 |
| `nativeBridge.clearCache()` | 清理缓存 |

### 4.4 提醒系统

**文件**: `reminder/` 目录

**工作流程**:

```
AlarmManager (每日定时)
    ↓ 触发
ReminderReceiver.onReceive()
    ↓ 读取
AnniversaryRepository.getUpcomingReminders(daysBefore)
    ↓ 筛选出 0~N 天内到期的纪念日
发送系统通知（最多 5 条，避免轰炸）
    ↓
ReminderManager.scheduleDaily() 重新调度明天
```

**配置项** (`SharedPreferences("reminder_settings")`):

| Key | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `enabled` | Boolean | true | 是否启用提醒 |
| `days_before` | Int | 3 | 提前多少天提醒 |
| `hour` | Int | 9 | 提醒小时（24h） |
| `minute` | Int | 0 | 提醒分钟 |
| `default_repeat` | String | "yearly" | 默认重复类型 |
| `anniversaries` | String | "[]" | 纪念日 JSON 副本（同步用） |

**通知渠道**: `anniversary_reminders` / 纪念日提醒（IMPORTANCE_HIGH）

**通知格式**:
- 当天到期: `🎉 今天是「XXX」！`
- 即将到期: `📅 XXX 还有 N 天`

### 4.5 数据恢复流程（已修复的 V2.0.84 bug）

**问题**: 恢复备份后数据未写入，因为 `syncToNative()` 把 localStorage 空数据反向覆盖了 `anniversaries.json`。

**修复方案**:

```
SettingsFragment.restoreData()
  → 清空旧数据，写入新数据到 Repository
  → 将首页格式 JSON 暂存到 SharedPreferences("settings").pending_restore_data
  → 设置 pending_restore_sync = true
  → 导航到首页

HomeFragment.syncRestoreDataIfNeeded()
  → 检测 pending_restore_sync 标志
  → 从 SharedPreferences 读取 pending_restore_data
  → 注入 WebView (__injectRestoreData)
  → reload 页面
  → 清除 pending_restore_sync 标志
```

---

## 5. 前端页面

### 5.1 首页 (home_page.html)

**大小**: ~94KB
**功能**:
- 纪念日卡片列表（带封面图、分类标签、倒计时天数）
- 搜索栏 + 分类筛选 Tab
- 精选回忆横向滚动区
- FAB 添加按钮 → 底部弹窗表单（添加/编辑）
- 纪念日详情弹窗（含农历/阳历、倒计时大数字）
- 全屏图片预览
- IPC 文件读写（Termux 通信）

**数据同步**: 页面初始化时调用 `nativeBridge.syncAnniversaries()` 全量推送数据到原生端。

**外部库**: `lunar-javascript@1.6.12`（农历计算）

### 5.2 日历页 (calendar_page.html)

**大小**: ~31KB
**功能**:
- 月历视图（今日高亮、事件圆点标记）
- 月份切换导航
- 点击日期展示当天事件卡片
- 事件详情弹窗（倒计时、分类、日期信息）
- 深色模式适配

### 5.3 设置页 (settings_page.html)

**大小**: ~44KB
**功能**:
- 深色模式开关
- 主题色选择（6 色：紫/蓝/粉/玫红/青/红）
- 提醒设置（开关、提前天数、提醒时间滚轮选择器）
- 备份与恢复（JSON 导入/导出）
- 缓存清理
- 版本信息展示

---

## 6. 主题系统

### 6.1 主题色

| 名称 | 亮色 | 暗色 |
|------|------|------|
| purple | `#8B5CF6` | `#A78BFA` |
| blue | `#3B82F6` | `#60A5FA` |
| pink | `#EC4899` | `#F472B6` |
| rose | `#F43F5E` | `#FB7185` |
| teal | `#14B8A6` | `#2DD4BF` |
| red | `#EF4444` | `#F87171` |

### 6.2 深色模式

- **切换入口**: 设置页开关
- **持久化**: `SharedPreferences("settings").dark_mode`
- **应用时机**: `TimeMemorialApp.onCreate()` 启动时读取并设置 `AppCompatDelegate`
- **同步范围**: 底栏颜色、WebView 注入 `data-theme="dark"`、values-night 资源

### 6.3 底栏颜色动态切换

`MainActivity.refreshBottomNavColors()` 根据当前主题色 + 深色模式动态计算 `ColorStateList`，解决切换后不生效的问题。

---

## 7. 权限

| 权限 | 用途 |
|------|------|
| `INTERNET` | WebView 加载 CDN 资源（lunar.js） |
| `READ_MEDIA_IMAGES` | 读取图片（Android 13+） |
| `POST_NOTIFICATIONS` | 发送纪念日提醒通知（Android 13+） |
| `RECEIVE_BOOT_COMPLETED` | 开机重新调度提醒 |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | 精确闹钟调度 |
| `VIBRATE` | 通知振动 |

---

## 8. 构建系统

### 8.1 自动版本号递增

`app/build.gradle.kts` 中读取 `version.properties`，每次构建自动 patch+1、build+1：

```properties
major=2
minor=0
patch=117
build=xxx
```

版本号格式: `major.minor.patch` → `2.0.117`

### 8.2 构建命令（Termux 环境）

```bash
# 完整构建
cd /sdcard/Download/TimeMemorial
./gradlew assembleDebug

# Termux 专用构建脚本
./termux_build.sh
```

---

## 9. 页面导航架构

```
MainActivity
├── BottomNavigationView (3 Tab)
│   ├── 首页 (HomeFragment)     ← nav_home
│   ├── 日历 (CalendarFragment) ← nav_calendar
│   └── 设置 (SettingsFragment) ← nav_settings
│
└── 详细导航图 (nav_graph.xml)
    ├── nav_home → HomeFragment
    ├── nav_calendar → CalendarFragment
    └── nav_settings → SettingsFragment
```

**返回键处理**:
- 首页: JS 侧 `__handleWebViewBack()` 优先关闭弹窗，否则 `goBack()` 或放行系统返回
- 日历: 详情弹窗关闭 → WebView goBack → 系统返回
- 设置: WebView goBack → 系统返回

---

## 10. 文件存储路径

| 路径 | 内容 | 说明 |
|------|------|------|
| `filesDir/anniversaries.json` | 纪念日数据 | 主存储，JSON 数组 |
| `filesDir/images/*.jpg` | 纪念日封面图 | WebBridge.saveImage() 保存 |
| `SharedPreferences("settings")` | 深色模式/恢复标记 | `dark_mode`, `pending_restore_sync` |
| `SharedPreferences("reminder_settings")` | 提醒配置 + 纪念日副本 | 闹钟调度读取 |
| `SharedPreferences("timememorial_prefs")` | 主题色选择 | `theme_color` |
| `Downloads/anniversaries_*.json` | 导出备份文件 | exportData() 写入 |

---

## 11. 已知设计决策

1. **WebView 混合架构**: UI 全部用 HTML/CSS/JS 渲染，原生只负责系统能力。优点是迭代快、动画流畅；缺点是首次加载有白屏、JS Bridge 调试不便。

2. **JSON 文件存储而非 SQLite/Room**: 虽然有 `AppDatabase` 和 `AnniversaryDao`，但底层全部委托给 `AnniversaryRepository` 的 JSON 文件读写。`AnniversaryDao` 只是兼容层，实际不使用 SQLite。

3. **SharedPreferences 双写**: `writeAll()` 同时写 JSON 文件和 SharedPreferences，因为提醒模块（`ReminderReceiver`）在 BroadcastReceiver 中无法访问 WebView，只能从 Prefs 读数据。

4. **恢复数据走 SharedPreferences 中转**: 避免 `syncToNative()` 的 localStorage 空数据反向覆盖原生 JSON。

5. **IPC 文件通道**: `WebBridge` 提供 `writeIpcFile/readIpcFile`，用于与 Termux 环境通信（如定时脚本执行）。

---

## 12. 版本更新日志

### V2.0.117 (当前版本)
- 所有功能逻辑完善并验证通过
- 修复恢复备份功能异常（V2.0.84 bug）
- 深色模式全页面适配
- 主题色动态切换（6 色）
- 日历页面月历视图
- 提醒系统（AlarmManager + 通知）

### V2.0.84
- 恢复备份功能异常：数据写入后被 syncToNative() 覆盖（已修复）

---

*文档生成时间: 2026-06-03*
