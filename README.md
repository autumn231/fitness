# Fitness

一款完全离线的健身与营养管理应用，内置 1300+ 个动作的中文指导、动图演示与详细步骤，以及 1800+ 种中国常见食物的营养成分查询与每日热量管理。无需联网，随时随地查阅与训练。

## 功能特性

- **动作库**：1300+ 健身动作，含中文名称、目标肌群、器械、难度与步骤说明，配套 GIF 动图演示
- **动作分类**：按身体部位、目标肌群、器械等多维度筛选（入口位于首页）
- **搜索**：关键词快速检索动作
- **收藏与历史**：收藏喜欢的动作，自动记录最近浏览
- **训练计划**：创建自定义训练计划，自由添加动作并设置组数与次数
- **训练执行**：独立训练页，支持切换计划、总用时计时（可暂停）、组间歇倒计时、训练清单待办勾选
- **日历安排**：为指定日期安排训练计划，月历视图直观管理训练日程
- **营养查询**：内置 1831 种中国常见食物的每 100g 营养成分（能量 / 蛋白质 / 碳水 / 脂肪 / 矿物质 / 维生素），支持搜索与分类浏览
- **每日热量管理**：设置每日能量消耗（TDEE），记录摄入食物自动计算热量缺口与三大营养累计，智能给出减脂建议
- **现代化 UI**：基于 Material 3 Expressive，页面切换动画、骨架屏、沉浸式详情页
- **主题切换**：支持浅色 / 深色 / 跟随系统

## 截图导航结构

底部导航栏 5 个 Tab：

| 首页 | 营养 | 训练 | 日历 | 我的 |
|------|------|------|------|------|

- **首页**：Hero 卡片、快捷入口（含动作分类入口）、按部位浏览、最近浏览、为你推荐
- **营养**：今日热量缺口卡片 + TDEE 设置 + 食物搜索 + 分类浏览 + 今日摄入记录
- **训练**：计划选择器 + 总用时计时 + 组间歇倒计时 + 训练清单
- **日历**：月历视图 + 日期安排 + 即将到来列表
- **我的**：数据统计 + 收藏与历史 + 训练计划管理 + 设置 + 关于

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **架构**：单 Activity + Navigation Compose
- **本地存储**：Room（收藏 / 历史 / 训练计划 / 日历安排 / 食物摄入日志）+ DataStore（设置 / TDEE）
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
│   ├── ExerciseRepository.kt    # 数据仓库统一入口（动作 + 食物 + 日志）
│   ├── AssetsLoader.kt          # 从 assets 加载动作与食物数据
│   ├── model/                   # 数据模型（Exercise、Food）
│   ├── prefs/SettingsDataStore.kt # 设置持久化（主题 + TDEE）
│   └── local/                   # Room 数据库、Entity、Dao
├── i18n/                        # 中文化映射与显示
└── ui/
    ├── theme/                   # 主题、色彩、字体、形状
    ├── common/                  # 共享组件（卡片、Chip、骨架屏等）
    ├── nav/                     # 导航、路由、切换动画
    └── feature/                 # 各功能页面
        ├── HomeScreen.kt
        ├── NutritionScreen.kt
        ├── FoodDetailScreen.kt
        ├── FoodListScreen.kt
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

推送到 `main` 分支会自动触发 GitHub Actions 工作流（`.github/workflows/build.yml`）构建 Release APK 并发布到 GitHub Releases。发布说明会从 [CHANGELOG.md](CHANGELOG.md) 自动提取对应版本内容。

## 数据来源与版权声明

### 动作数据

来自开源项目 [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset)（MIT License），已做中文化映射处理。

> **媒体版权说明**：动作数据集结构、指令文本/翻译部分遵循 MIT 协议。`images/` 与 `videos/` 目录下的 GIF 媒体版权归 © [Gym Visual](https://gymvisual.com/) 所有，原作者已获书面许可以 180×180 分辨率分发，使用时须保留署名 "© Gym Visual — https://gymvisual.com/"。

### 食物营养数据

基于《中国食物成分表》标准版第6版（中国疾病预防控制中心营养与健康所编，北京大学医学出版社出版），由开源项目 [Sanotsu/china-food-composition-data](https://github.com/Sanotsu/china-food-composition-data) 通过 OCR / 视觉大模型识别整理为结构化 JSON。

> **版权说明**：食物成分数据本身为事实数据。原书版权归出版社与编者所有，本项目仅以非商业用途使用 OCR 整理后的结构化数据，原仓库未附带 LICENSE，版权归原作者所有。

## 许可证

本应用源代码遵循 [MIT License](LICENSE)。第三方数据资源的版权见上方"数据来源与版权声明"。

## 作者

高翔 · 微信 `gx13598483383`
