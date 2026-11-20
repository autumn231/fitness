package com.fitness.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.DateKeys
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.Meals
import com.fitness.app.data.TrendConclusion
import com.fitness.app.data.Trends
import com.fitness.app.data.effectiveTdee
import com.fitness.app.data.estimateBurnKcal
import com.fitness.app.data.local.CalendarPlanEntity
import com.fitness.app.data.local.FoodLogEntity
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.data.local.ProfileEntity
import com.fitness.app.data.local.RecentEntity
import com.fitness.app.data.local.WeightLogEntity
import com.fitness.app.data.local.WorkoutSessionEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 今日页概览卡片定义（顺序即默认顺序）。 */
object HomeCards {
    const val BUDGET = "budget"
    const val TRAINING = "training"
    const val WEIGHT = "weight"
    const val TREND = "trend"

    val DEFAULT_ORDER = listOf(BUDGET, TRAINING, WEIGHT, TREND)

    fun titleOf(id: String): String = when (id) {
        BUDGET -> "热量总账"
        TRAINING -> "今日训练"
        WEIGHT -> "体重"
        TREND -> "趋势"
        else -> id
    }
}

data class HomeCardConfig(val id: String, val visible: Boolean)

/** 选中日期当天的聚合数字（今日页总账/训练/体重卡共用）。 */
data class HomeDaySummary(
    val budget: Int = 0,
    val intake: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val burn: Int = 0,
    val remaining: Double = 0.0,
    val daySessions: List<WorkoutSessionEntity> = emptyList(),
    val todayPlan: PlanWithItems? = null,
    val latestWeight: WeightLogEntity? = null,
    /** 最新体重与上一次的差（kg），不足两条时为 null。 */
    val weightDelta: Double? = null
)

/**
 * 今日页状态（第 3 期从 HomeScreen 迁出）：查看日期、数据流、当天聚合、
 * 趋势结论、卡片配置与全部对话框状态。餐次时间轴本身只是渲染 logs。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repo: ExerciseRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    private val _followingToday = MutableStateFlow(true)
    val followingToday: StateFlow<Boolean> = _followingToday.asStateFlow()

    val logs: StateFlow<List<FoodLogEntity>> = _selectedDate
        .flatMapLatest { date -> repo.observeFoodLogs(DateKeys.of(date)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tdeeOverride: StateFlow<Int> = repo.settings.tdee
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val profile: StateFlow<ProfileEntity?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val weights: StateFlow<List<WeightLogEntity>> = repo.observeWeights()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val sessions: StateFlow<List<WorkoutSessionEntity>> = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeSession: StateFlow<WorkoutSessionEntity?> = repo.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val calendarPlans: StateFlow<List<CalendarPlanEntity>> = repo.observeCalendarPlans()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val plans: StateFlow<List<PlanWithItems>> = repo.observePlans()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recents: StateFlow<List<RecentEntity>> = repo.observeRecents(8)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val allFoodLogs: StateFlow<List<FoodLogEntity>> = repo.observeAllFoodLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val daySummary: StateFlow<HomeDaySummary> = combine(
        _selectedDate, logs, tdeeOverride, profile, weights, sessions, calendarPlans, plans
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val date = values[0] as LocalDate
        @Suppress("UNCHECKED_CAST")
        val dayLogs = values[1] as List<FoodLogEntity>
        val override = values[2] as Int
        val prof = values[3] as ProfileEntity?
        @Suppress("UNCHECKED_CAST")
        val allWeights = values[4] as List<WeightLogEntity>
        @Suppress("UNCHECKED_CAST")
        val allSessions = values[5] as List<WorkoutSessionEntity>
        @Suppress("UNCHECKED_CAST")
        val calPlans = values[6] as List<CalendarPlanEntity>
        @Suppress("UNCHECKED_CAST")
        val allPlans = values[7] as List<PlanWithItems>

        val dateKey = DateKeys.of(date)
        val latestWeight = allWeights.lastOrNull()
        val budget = effectiveTdee(override, prof, latestWeight?.weightKg)
        val intake = dayLogs.sumOf { it.energy }
        val daySessions = allSessions.filter { it.dateKey == dateKey && it.endedAt != null }
        val burn = daySessions.sumOf { estimateBurnKcal(it.durationSec) }
        val plan = calPlans.firstOrNull { it.dateKey == dateKey }
            ?.let { entry -> allPlans.firstOrNull { it.plan.id == entry.planId } }
        HomeDaySummary(
            budget = budget,
            intake = intake,
            protein = dayLogs.sumOf { it.protein },
            carbs = dayLogs.sumOf { it.carbs },
            fat = dayLogs.sumOf { it.fat },
            burn = burn,
            remaining = budget - intake + burn,
            daySessions = daySessions,
            todayPlan = plan,
            latestWeight = latestWeight,
            weightDelta = if (allWeights.size >= 2)
                allWeights.last().weightKg - allWeights[allWeights.size - 2].weightKg
            else null
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeDaySummary())

    /** 趋势结论：体重 / 训练 / 摄入三条（第 4 期）。 */
    val trends: StateFlow<List<TrendConclusion>> = combine(weights, sessions, allFoodLogs) { w, s, l ->
        listOf(Trends.weightTrend(w), Trends.trainingTrend(s), Trends.intakeTrend(l))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------- 卡片配置 ----------
    private val _cardConfigs = MutableStateFlow(parseCardConfig(""))
    val cardConfigs: StateFlow<List<HomeCardConfig>> = _cardConfigs.asStateFlow()

    private val _editCardsMode = MutableStateFlow(false)
    val editCardsMode: StateFlow<Boolean> = _editCardsMode.asStateFlow()

    init {
        viewModelScope.launch {
            repo.settings.homeCardsConfig.collect { raw -> _cardConfigs.value = parseCardConfig(raw) }
        }
    }

    private fun parseCardConfig(raw: String): List<HomeCardConfig> {
        val parsed = raw.split(",").mapNotNull { part ->
            val seg = part.trim().split(":")
            val id = seg.getOrNull(0) ?: return@mapNotNull null
            if (id !in HomeCards.DEFAULT_ORDER) return@mapNotNull null
            HomeCardConfig(id, visible = seg.getOrNull(1) != "0")
        }
        val missing = HomeCards.DEFAULT_ORDER.filter { id -> parsed.none { it.id == id } }
        return parsed + missing.map { HomeCardConfig(it, visible = true) }
    }

    private fun persistCardConfig() {
        val raw = _cardConfigs.value.joinToString(",") { "${it.id}:${if (it.visible) 1 else 0}" }
        viewModelScope.launch { repo.settings.setHomeCardsConfig(raw) }
    }

    fun setEditCardsMode(on: Boolean) {
        _editCardsMode.value = on
    }

    fun setCardVisible(id: String, visible: Boolean) {
        _cardConfigs.value = _cardConfigs.value.map {
            if (it.id == id) it.copy(visible = visible) else it
        }
        persistCardConfig()
    }

    fun moveCard(id: String, up: Boolean) {
        val list = _cardConfigs.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val target = if (up) idx - 1 else idx + 1
        if (target !in list.indices) return
        val item = list.removeAt(idx)
        list.add(target, item)
        _cardConfigs.value = list
        persistCardConfig()
    }

    // ---------- 日期 ----------
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _followingToday.value = date == LocalDate.now()
    }

    fun backToToday() {
        _selectedDate.value = LocalDate.now()
        _followingToday.value = true
    }

    /** 回到前台时调用：停在「今天」则跟随跨天刷新（与旧页 ON_RESUME 逻辑一致）。 */
    fun onResume() {
        if (_followingToday.value) _selectedDate.value = LocalDate.now()
    }

    // ---------- 一次性提示（Snackbar） ----------
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // ---------- 对话框状态 ----------
    private val _editLog = MutableStateFlow<FoodLogEntity?>(null)
    val editLog: StateFlow<FoodLogEntity?> = _editLog.asStateFlow()
    private val _quickAddMeal = MutableStateFlow<String?>(null)
    val quickAddMeal: StateFlow<String?> = _quickAddMeal.asStateFlow()
    private val _saveTemplateMeal = MutableStateFlow<String?>(null)
    val saveTemplateMeal: StateFlow<String?> = _saveTemplateMeal.asStateFlow()
    private val _showTdee = MutableStateFlow(false)
    val showTdee: StateFlow<Boolean> = _showTdee.asStateFlow()
    private val _showWeight = MutableStateFlow(false)
    val showWeight: StateFlow<Boolean> = _showWeight.asStateFlow()
    private val _showQuickSheet = MutableStateFlow(false)
    val showQuickSheet: StateFlow<Boolean> = _showQuickSheet.asStateFlow()

    fun openEditLog(log: FoodLogEntity) { _editLog.value = log }
    fun dismissEditLog() { _editLog.value = null }
    fun confirmEditLog(grams: Double) {
        val log = _editLog.value ?: return
        viewModelScope.launch {
            repo.updateFoodLogAmount(log, grams)
            _editLog.value = null
        }
    }

    fun deleteLog(log: FoodLogEntity) {
        viewModelScope.launch { repo.removeFoodLog(log.id) }
    }

    fun openQuickAdd(meal: String) { _quickAddMeal.value = meal }
    fun dismissQuickAdd() { _quickAddMeal.value = null }
    fun confirmQuickAdd(kcal: Double, name: String) {
        val meal = _quickAddMeal.value ?: return
        val dateKey = DateKeys.of(_selectedDate.value)
        viewModelScope.launch {
            repo.quickAddFoodLog(kcal, name, dateKey, meal)
            _quickAddMeal.value = null
            _messages.tryEmit("已入账 ${kcal.toInt()} kcal")
        }
    }

    fun openSaveTemplate(meal: String) { _saveTemplateMeal.value = meal }
    fun dismissSaveTemplate() { _saveTemplateMeal.value = null }
    fun confirmSaveTemplate(name: String) {
        val meal = _saveTemplateMeal.value ?: return
        val mealLogs = logs.value.filter { it.meal == meal }
        viewModelScope.launch {
            repo.saveMealTemplate(name, mealLogs)
            _saveTemplateMeal.value = null
            _messages.tryEmit("已存为常用餐「$name」")
        }
    }

    fun copyYesterday(meal: String) {
        val date = _selectedDate.value
        viewModelScope.launch {
            val n = repo.copyMeal(DateKeys.of(date.minusDays(1)), meal, DateKeys.of(date))
            _messages.tryEmit(
                if (n > 0) "已复制昨天${meal}的 $n 样食物" else "昨天${meal}没有记录可复制"
            )
        }
    }

    fun setShowTdee(show: Boolean) { _showTdee.value = show }
    fun saveTdeeOverride(value: Int) {
        viewModelScope.launch {
            repo.settings.setTdee(value)
            _showTdee.value = false
        }
    }

    fun setShowWeight(show: Boolean) { _showWeight.value = show }
    fun recordWeight(dateKey: String, kg: Double) {
        viewModelScope.launch {
            repo.setWeight(dateKey, kg)
            _showWeight.value = false
            _messages.tryEmit("已记录体重 $kg kg")
        }
    }

    fun setShowQuickSheet(show: Boolean) { _showQuickSheet.value = show }

    /** 快速记录面板里「记饮食」的默认餐次（按当前时间推断）。 */
    fun inferMealNow(): String = Meals.inferNow()
}
