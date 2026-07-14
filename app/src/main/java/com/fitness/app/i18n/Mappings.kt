package com.fitness.app.i18n

import com.fitness.app.data.model.Exercise

/** 英文 -> 中文 映射表（基于数据集全部 distinct 取值，零回退） */

val bodyPartMap: Map<String, String> = mapOf(
    "back" to "背部",
    "cardio" to "有氧",
    "chest" to "胸部",
    "lower arms" to "前臂",
    "lower legs" to "小腿",
    "neck" to "颈部",
    "shoulders" to "肩部",
    "upper arms" to "上臂",
    "upper legs" to "大腿",
    "waist" to "腰腹"
)

val equipmentMap: Map<String, String> = mapOf(
    "assisted" to "助力",
    "band" to "弹力带",
    "barbell" to "杠铃",
    "body weight" to "自重",
    "bosu ball" to "波速球",
    "cable" to "绳索",
    "dumbbell" to "哑铃",
    "elliptical machine" to "椭圆机",
    "ez barbell" to "曲杠杠铃",
    "hammer" to "锤子",
    "kettlebell" to "壶铃",
    "leverage machine" to "综合训练机",
    "medicine ball" to "药球",
    "olympic barbell" to "奥林匹克杠铃",
    "resistance band" to "阻力带",
    "roller" to "滚轮",
    "rope" to "绳索",
    "skierg machine" to "滑雪机",
    "sled machine" to "推雪橇机",
    "smith machine" to "史密斯机",
    "stability ball" to "稳定球",
    "stationary bike" to "健身车",
    "stepmill machine" to "台阶机",
    "tire" to "轮胎",
    "trap bar" to "菱形杠铃",
    "upper body ergometer" to "上肢测功仪",
    "weighted" to "负重",
    "wheel roller" to "健腹轮"
)

/** 目标肌群 / 主肌群 / 辅助肌群 共用映射 */
val muscleMap: Map<String, String> = mapOf(
    "abductors" to "外展肌",
    "abs" to "腹肌",
    "adductors" to "内收肌",
    "abdominals" to "腹肌",
    "ankle stabilizers" to "踝稳定肌",
    "ankles" to "踝关节",
    "back" to "背部",
    "biceps" to "肱二头肌",
    "brachialis" to "肱肌",
    "calves" to "小腿",
    "cardiovascular system" to "心血管系统",
    "chest" to "胸部",
    "core" to "核心",
    "deltoids" to "三角肌",
    "delts" to "三角肌",
    "feet" to "足部",
    "forearms" to "前臂",
    "glutes" to "臀肌",
    "grip muscles" to "握力肌群",
    "groin" to "腹股沟",
    "hamstrings" to "腘绳肌",
    "hands" to "手部",
    "hip flexors" to "髋屈肌",
    "inner thighs" to "大腿内侧",
    "latissimus dorsi" to "背阔肌",
    "lats" to "背阔肌",
    "levator scapulae" to "肩胛提肌",
    "lower abs" to "下腹肌",
    "lower back" to "下背部",
    "obliques" to "腹斜肌",
    "pectorals" to "胸肌",
    "quadriceps" to "股四头肌",
    "quads" to "股四头肌",
    "rear deltoids" to "后三角肌",
    "rhomboids" to "菱形肌",
    "rotator cuff" to "肩袖",
    "serratus anterior" to "前锯肌",
    "shins" to "胫部",
    "shoulders" to "肩部",
    "soleus" to "比目鱼肌",
    "spine" to "脊柱",
    "sternocleidomastoid" to "胸锁乳突肌",
    "traps" to "斜方肌",
    "trapezius" to "斜方肌",
    "triceps" to "肱三头肌",
    "upper back" to "上背部",
    "upper chest" to "上胸部",
    "wrist extensors" to "腕伸肌",
    "wrist flexors" to "腕屈肌",
    "wrists" to "腕关节"
)

fun bodyPartZh(en: String): String = bodyPartMap[en] ?: en

fun equipmentZh(en: String): String = equipmentMap[en] ?: en

fun muscleZh(en: String): String = muscleMap[en] ?: en

/**
 * 显示名 = "{器械中文}·{目标肌群中文}"
 * 例如：dumbbell + biceps -> "哑铃·肱二头肌"
 */
fun Exercise.displayName(): String {
    val eq = equipmentZh(equipment)
    val tg = muscleZh(target)
    return if (eq.isNotBlank() && tg.isNotBlank()) "$eq·$tg" else eq.ifBlank { tg }.ifBlank { name }
}

/** 副标题：英文原名（用于区分重名动作） */
fun Exercise.subtitle(): String = name

/** 分类的维度项（带中文名与数量） */
data class CategoryEntry(val keyEn: String, val nameZh: String, val count: Int)

fun groupByBodyPart(exercises: List<Exercise>): List<CategoryEntry> =
    exercises.groupingBy { it.body_part }
        .eachCount()
        .map { (k, c) -> CategoryEntry(k, bodyPartZh(k), c) }
        .sortedByDescending { it.count }

fun groupByEquipment(exercises: List<Exercise>): List<CategoryEntry> =
    exercises.groupingBy { it.equipment }
        .eachCount()
        .map { (k, c) -> CategoryEntry(k, equipmentZh(k), c) }
        .sortedByDescending { it.count }

fun groupByTarget(exercises: List<Exercise>): List<CategoryEntry> =
    exercises.groupingBy { it.target }
        .eachCount()
        .map { (k, c) -> CategoryEntry(k, muscleZh(k), c) }
        .sortedByDescending { it.count }
