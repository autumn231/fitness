# 练记

一款完全离线的健身与营养管理应用，内置 1,324 个动作的中文指导、动态演示与详细步骤，以及 1,831 种中国常见食物的营养成分查询与每日饮食记录。无需联网，随时随地查阅与训练。

## 功能特性

- **今日总账**：一周日期条切换任意一天；热量卡显示「还可吃 = 预算 − 摄入 + 训练消耗」，三大营养素目标同步展示；概览卡片（热量 / 训练 / 体重 / 趋势）可自定义显隐与排序
- **记一餐**：早餐 / 午餐 / 晚餐 / 加餐分餐记录；添加页有最近吃过、最常吃、食谱、常用餐，点一次可连续添加；支持复制昨天同餐、只填热量的快速添加、整餐存为常用餐一键套用；点已记录条目可改克数，营养按比例重算
- **食谱**：12 道内置高蛋白家常菜（增肌 / 减脂），食材营养由食物库自动汇总，可按份数缩放整单加入；也可自建、编辑、删除自己的食谱
- **训练（组表模式）**：每个动作一张组表——组号、上次成绩、重量、次数、完成勾选，重量与次数按上次成绩预填；计时类动作（如平板支撑）按秒记录；勾完一组自动开始组间歇倒计时（结束震动提醒，并提示休息秒数）；记录实时落库，杀进程不丢；完成训练出总结页（时长、总容量、完成组数、新纪录 PR 高亮），自由训练可反存为计划
- **计划与模板**：自建训练计划；内置 6 套计划模板（新手全身、推拉腿一周三练 / 六练、上下肢分化、五分化、徒手居家），可预览全部动作，一键生成到「我的计划」；日历可按日期安排计划，已完成日期有对勾与当天小结
- **动作库**：1,324 个动作，含中文名称、目标肌群、器械、难度与步骤说明，配套动态 WebP 演示（全程离线）；按部位 / 肌群 / 器械筛选，支持收藏与最近浏览；动作搜索是底部一级栏目
- **食物库**：1,831 种中国常见食物的每 100g 营养成分（能量 / 蛋白质 / 碳水 / 脂肪 / 矿物质 / 维生素），支持搜索与分类浏览
- **体重与档案**：体重按天记录，趋势折线 + 「趋势结论」卡（近 90 天方向与周均变化率）；个人档案（性别、出生年份、身高、活动水平）填齐后，每日热量目标按 Mifflin-St Jeor 公式自动计算，也可手动覆盖
- **主题**：浅色 / 深色 / 跟随系统

## 导航结构

底部导航栏 5 个 Tab：

| 今日 | 营养 | 训练 | 搜索 | 我的 |
|------|------|------|------|------|

- **今日**：日期条 + 热量总账 + 四餐记录 + 当日训练记录，全局「+」可记饮食 / 记体重 / 开始训练
- **营养**：食物搜索与分类浏览、食谱入口、营养汇总
- **训练**：计划与组表训练，日历入口在顶栏（与「管理计划」并列）
- **搜索**：动作搜索与筛选、收藏
- **我的**：个人档案、体重进度、设置、关于（含数据来源与版权声明）

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **架构**：单 Activity + Navigation Compose + MVVM（训练 / 今日 / 体重等页状态与业务逻辑在 ViewModel）
- **本地存储**：Room（计划 / 训练会话与组记录 / 饮食日志 / 常用餐 / 食谱 / 体重 / 档案）+ DataStore（设置）
- **图片加载**：Coil（支持动态 WebP）
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
│   ├── prefs/SettingsDataStore.kt # 设置持久化（主题等）
│   └── local/                   # Room 数据库、Entity、Dao
├── i18n/                        # 中文化映射与显示
└── ui/
    ├── theme/                   # 主题、色彩、字体、形状
    ├── common/                  # 共享组件
    ├── nav/                     # 导航、路由、切换动画
    └── feature/                 # 各功能页面（今日 / 营养 / 训练 / 搜索 / 我的 / 日历等）
```

## 构建

需要 JDK 17 与 Android SDK（compileSdk 35、build-tools 35.0.0）。

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK（开启 R8 + 资源压缩，只打包 arm64-v8a）
./gradlew :app:assembleRelease
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

### 发布签名

Release 包使用独立签名密钥，避免每次构建签名不同导致无法覆盖安装。签名信息从仓库根目录的 `keystore.properties` 读取（**该文件不入库**）：

```properties
storeFile=/absolute/path/to/fitness-release.jks
storePassword=******
keyAlias=fitness
keyPassword=******
```

没有该文件时会自动回退到 debug 签名，方便在任意机器上验证构建，但这种包不能用于正式发布。

## 发布

发布不再依赖 GitHub Actions，由构建机本地脚本完成（构建 → 打 tag → 上传到 GitHub Release）：

```bash
./scripts/release.sh              # 构建并发布
./scripts/release.sh --build-only # 只构建，产物在 dist/
./scripts/release.sh --draft      # 发布为 draft
```

脚本会强制：

- release 构建（R8 混淆 + 资源压缩），不会误发 debug 包
- 只保留 `arm64-v8a` 单一 ABI，产物名 `fitness-v<version>-arm64-v8a.apk`
- 校验产物 ABI 与签名后才上传
- Release Notes 从 [CHANGELOG.md](CHANGELOG.md) 自动提取对应版本内容

发布前请先提升 `app/build.gradle.kts` 中的 `versionCode` 与 `versionName`。GitHub Releases 每天只保留当天最新一个版本。

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

高翔 · 邮箱 `wlong9892@gmail.com`
