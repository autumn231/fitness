# Fitness

一款完全离线的健身动作指导应用，内置 1300+ 个动作的中文指导、动图演示与详细步骤。无需联网，随时随地查阅与训练。

## 功能特性

- **动作库**：1300+ 健身动作，含中文名称、目标肌群、器械、难度与步骤说明，配套 GIF 动图演示
- **分类浏览**：按身体部位、目标肌群、器械等多维度筛选
- **搜索**：关键词快速检索动作
- **收藏与历史**：收藏喜欢的动作，自动记录最近浏览
- **训练计划**：创建自定义训练计划，自由添加动作并设置组数与次数
- **训练执行**：独立训练页，支持切换计划、总用时计时（可暂停）、组间歇倒计时、训练清单待办勾选
- **日历安排**：为指定日期安排训练计划，月历视图直观管理训练日程
- **现代化 UI**：基于 Material 3 Expressive，页面切换动画、骨架屏、沉浸式详情页
- **主题切换**：支持浅色 / 深色 / 跟随系统

## 截图导航结构

底部导航栏 5 个 Tab：

| 首页 | 分类 | 训练 | 日历 | 我的 |
|------|------|------|------|------|

- **首页**：Hero 卡片、快捷入口、按部位浏览、最近浏览、为你推荐
- **分类**：多维度动作分类
- **训练**：计划选择器 + 总用时计时 + 组间歇倒计时 + 训练清单
- **日历**：月历视图 + 日期安排 + 即将到来列表
- **我的**：数据统计 + 收藏与历史 + 训练计划管理 + 设置 + 关于

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **架构**：单 Activity + Navigation Compose
- **本地存储**：Room（收藏 / 历史 / 训练计划 / 日历安排）+ DataStore（设置）
- **图片加载**：Coil（支持 GIF）
- **序列化**：kotlinx.serialization
- **最低系统**：Android 12（API 31）
- **目标系统**：Android 15（API 35）

## 项目结构

```
app/src/main/java/com/fitness/app/
├── MainActivity.kt              # 入口 Activity
├── FitnessApp.kt                # Application，初始化 Repository
├── data/
│   ├── ExerciseRepository.kt    # 数据仓库统一入口
│   ├── AssetsLoader.kt          # 从 assets 加载动作数据
│   ├── prefs/SettingsDataStore.kt # 设置持久化
│   └── local/                   # Room 数据库、Entity、Dao
├── i18n/                        # 中文化映射与显示
└── ui/
    ├── theme/                   # 主题、色彩、字体、形状
    ├── common/                  # 共享组件（卡片、Chip、骨架屏等）
    ├── nav/                     # 导航、路由、切换动画
    └── feature/                 # 各功能页面
        ├── HomeScreen.kt
        ├── CategoryScreen.kt
        ├── TrainingScreen.kt
        ├── CalendarScreen.kt
        ├── ProfileScreen.kt
        └── ...
```

## 构建

需要 JDK 17 与 Android SDK（compileSdk 35）。

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK
./gradlew :app:assembleRelease
```

产物路径：`app/build/outputs/apk/`

## 发布

推送到 `main` 分支会自动触发 GitHub Actions 工作流（`.github/workflows/build.yml`）构建 Release APK 并发布到 GitHub Releases。

## 数据来源

动作数据来自开源项目 [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset)，已做中文化映射处理。所有图片与 GIF 素材均随应用离线分发。

## 许可证

[MIT License](LICENSE)

## 作者

高翔 · 微信 `gx13598483383`
