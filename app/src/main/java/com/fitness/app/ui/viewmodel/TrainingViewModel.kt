package com.fitness.app.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.PlanWithItems
import com.fitness.app.data.local.WorkoutSessionEntity
import com.fitness.app.data.local.WorkoutSetEntity
import com.fitness.app.ui.feature.formatKg
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 训练总结数据（完成时算一次，含 PR 判定）。 */
data class SessionSummary(
    val session: WorkoutSessionEntity,
    val durationSec: Long,
    val volumeKg: Double,
    val doneSets: Int,
    val totalSets: Int,
    val prs: List<String>
)

/**
 * 训练页状态机（第 3 期从 TrainingScreen 迁出）：
 * 会话/组表数据、总用时计时、组间歇计时、计划选择与全部对话框状态都在这里，
 * 页面只负责渲染与转发点击。计时在 viewModelScope 里跑，切 Tab 不丢。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TrainingViewModel(
    private val repo: ExerciseRepository,
    private val appContext: Context
) : ViewModel() {

    val plans: StateFlow<List<PlanWithItems>> = repo.observePlans()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeSession: StateFlow<WorkoutSessionEntity?> = repo.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allSessions: StateFlow<List<WorkoutSessionEntity>> = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sets: StateFlow<List<WorkoutSetEntity>> = activeSession
        .flatMapLatest { s -> if (s == null) flowOf(emptyList()) else repo.observeSets(s.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _prevMap = MutableStateFlow<Map<String, List<WorkoutSetEntity>>>(emptyMap())
    val prevMap: StateFlow<Map<String, List<WorkoutSetEntity>>> = _prevMap.asStateFlow()

    private val _selectedPlanId = MutableStateFlow(0L)
    val selectedPlanId: StateFlow<Long> = _selectedPlanId.asStateFlow()

    // ---- 总用时 ----
    private val _totalSeconds = MutableStateFlow(0L)
    val totalSeconds: StateFlow<Long> = _totalSeconds.asStateFlow()
    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()
    private var accumulated = 0L
    private var startedAtUi = 0L
    private var timerSessionId = -1L
    private var timerJob: Job? = null

    // ---- 组间歇 ----
    private val _restTotal = MutableStateFlow(90)
    val restTotal: StateFlow<Int> = _restTotal.asStateFlow()
    private val _restRemaining = MutableStateFlow(0)
    val restRemaining: StateFlow<Int> = _restRemaining.asStateFlow()
    private val _restRunning = MutableStateFlow(false)
    val restRunning: StateFlow<Boolean> = _restRunning.asStateFlow()
    private var restEndAt = 0L
    private var restJob: Job? = null

    // ---- 对话框状态 ----
    private val _showPicker = MutableStateFlow(false)
    val showPicker: StateFlow<Boolean> = _showPicker.asStateFlow()
    private val _summary = MutableStateFlow<SessionSummary?>(null)
    val summary: StateFlow<SessionSummary?> = _summary.asStateFlow()
    private val _historyDetail = MutableStateFlow<WorkoutSessionEntity?>(null)
    val historyDetail: StateFlow<WorkoutSessionEntity?> = _historyDetail.asStateFlow()
    private val _confirmAbandon = MutableStateFlow(false)
    val confirmAbandon: StateFlow<Boolean> = _confirmAbandon.asStateFlow()
    private val _confirmDeleteHistory = MutableStateFlow<WorkoutSessionEntity?>(null)
    val confirmDeleteHistory: StateFlow<WorkoutSessionEntity?> = _confirmDeleteHistory.asStateFlow()

    init {
        // 计划选择：消费跨页请求 + 选中项失效时回落到第一个计划（与旧页 LaunchedEffect(plans) 同逻辑）
        viewModelScope.launch {
            plans.collect { list ->
                val requested = repo.consumeRequestedPlan()
                when {
                    requested != null && list.any { it.plan.id == requested } ->
                        _selectedPlanId.value = requested
                    list.isNotEmpty() && list.none { it.plan.id == _selectedPlanId.value } ->
                        _selectedPlanId.value = list.first().plan.id
                }
            }
        }
        // 会话切换时初始化/复位总用时（与旧页 LaunchedEffect(session?.id) 同逻辑）
        viewModelScope.launch {
            activeSession.collect { s ->
                if (s != null && timerSessionId != s.id) {
                    timerSessionId = s.id
                    accumulated = ((System.currentTimeMillis() - s.startedAt) / 1000).coerceAtLeast(0)
                    _totalSeconds.value = accumulated
                    startedAtUi = SystemClock.elapsedRealtime()
                    startTimerLoop()
                }
                if (s == null) {
                    timerSessionId = -1L
                    stopTimerLoop()
                    accumulated = 0L
                    _totalSeconds.value = 0L
                }
            }
        }
        // Previous 数据：会话内每个动作的上次组记录
        viewModelScope.launch {
            var lastIds: List<String>? = null
            sets.collect { list ->
                val s = activeSession.value ?: return@collect
                val ids = list.map { it.exerciseId }.distinct()
                if (ids.isEmpty() || ids == lastIds) return@collect
                lastIds = ids
                val map = mutableMapOf<String, List<WorkoutSetEntity>>()
                ids.forEach { id -> map[id] = repo.previousSets(id, s.id) }
                _prevMap.value = map
            }
        }
    }

    // ---------- 总用时 ----------
    private fun startTimerLoop() {
        timerJob?.cancel()
        _timerRunning.value = true
        timerJob = viewModelScope.launch {
            while (true) {
                _totalSeconds.value =
                    accumulated + (SystemClock.elapsedRealtime() - startedAtUi) / 1000
                delay(200L)
            }
        }
    }

    private fun stopTimerLoop() {
        timerJob?.cancel()
        timerJob = null
        _timerRunning.value = false
    }

    fun toggleTimer() {
        if (_timerRunning.value) {
            accumulated = _totalSeconds.value
            stopTimerLoop()
        } else {
            startedAtUi = SystemClock.elapsedRealtime()
            startTimerLoop()
        }
    }

    fun resetTimer() {
        val s = activeSession.value ?: return
        accumulated = ((System.currentTimeMillis() - s.startedAt) / 1000).coerceAtLeast(0)
        _totalSeconds.value = accumulated
        startedAtUi = SystemClock.elapsedRealtime()
        startTimerLoop()
    }

    // ---------- 组间歇 ----------
    private fun startRestLoop() {
        restJob?.cancel()
        _restRunning.value = true
        restJob = viewModelScope.launch {
            while (true) {
                val remainMs = restEndAt - SystemClock.elapsedRealtime()
                _restRemaining.value = ((remainMs + 999) / 1000).toInt().coerceAtLeast(0)
                if (remainMs <= 0L) {
                    _restRunning.value = false
                    vibrateOnce()
                    break
                }
                delay(200L)
            }
        }
    }

    private fun stopRestLoop() {
        restJob?.cancel()
        restJob = null
        _restRunning.value = false
    }

    fun startRest() {
        _restRemaining.value = _restTotal.value
        restEndAt = SystemClock.elapsedRealtime() + _restTotal.value * 1000L
        startRestLoop()
    }

    fun restPreset(secs: Int) {
        _restTotal.value = secs
        _restRemaining.value = secs
        restEndAt = SystemClock.elapsedRealtime() + secs * 1000L
        startRestLoop()
    }

    fun toggleRest() {
        if (_restRunning.value) {
            stopRestLoop()
        } else {
            if (_restRemaining.value <= 0) _restRemaining.value = _restTotal.value
            restEndAt = SystemClock.elapsedRealtime() + _restRemaining.value * 1000L
            startRestLoop()
        }
    }

    fun resetRest() {
        stopRestLoop()
        _restRemaining.value = 0
    }

    /** 组间歇结束震动提醒（minSdk 31，直接用 VibratorManager）。 */
    private fun vibrateOnce() {
        val vibrator = (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 320, 180, 320), -1))
    }

    // ---------- 计划与会话 ----------
    fun selectPlan(id: Long) {
        _selectedPlanId.value = id
    }

    fun startFromPlan() {
        val plan = plans.value.firstOrNull { it.plan.id == _selectedPlanId.value } ?: return
        viewModelScope.launch { repo.startSessionFromPlan(plan) }
    }

    fun startFree() {
        viewModelScope.launch { repo.startFreeSession() }
    }

    fun updateSet(set: WorkoutSetEntity) {
        viewModelScope.launch { repo.updateSet(set) }
    }

    fun toggleDone(set: WorkoutSetEntity) {
        viewModelScope.launch {
            repo.updateSet(set.copy(done = !set.done))
            if (!set.done) startRest()
        }
    }

    fun addSet(exerciseId: String) {
        val s = activeSession.value ?: return
        viewModelScope.launch { repo.addSetToExercise(s.id, exerciseId) }
    }

    fun removeExercise(exerciseId: String) {
        val s = activeSession.value ?: return
        viewModelScope.launch { repo.removeExerciseFromSession(s.id, exerciseId) }
    }

    fun addExercise(exerciseId: String) {
        val s = activeSession.value ?: return
        viewModelScope.launch { repo.addExerciseToSession(s.id, exerciseId) }
    }

    fun finish() {
        val s = activeSession.value ?: return
        val currentSets = sets.value
        val duration = _totalSeconds.value
        viewModelScope.launch {
            _summary.value = buildSummary(s, currentSets, duration)
            repo.finishSession(s, duration)
        }
    }

    fun dismissSummary() {
        _summary.value = null
    }

    fun saveSummaryAsPlan(name: String) {
        val s = _summary.value?.session ?: return
        viewModelScope.launch {
            repo.saveSessionAsPlan(s, name)
            _summary.value = null
        }
    }

    private suspend fun buildSummary(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
        durationSec: Long
    ): SessionSummary {
        val done = sets.filter { it.done }
        val volume = done.sumOf { it.weightKg * it.reps }
        val prs = mutableListOf<String>()
        sets.groupBy { it.exerciseId }.forEach { (exerciseId, exSets) ->
            val best = exSets.filter { it.done }.maxOfOrNull { it.weightKg } ?: 0.0
            if (best > 0) {
                val prevBest = repo.bestWeightBefore(exerciseId, session.id) ?: 0.0
                if (prevBest > 0 && best > prevBest) {
                    val name = exSets.first().exerciseName
                    prs.add("$name 新纪录 ${formatKg(best)} kg（原 ${formatKg(prevBest)} kg）")
                }
            }
        }
        return SessionSummary(session, durationSec, volume, done.size, sets.size, prs)
    }

    // ---------- 对话框 ----------
    fun setShowPicker(show: Boolean) {
        _showPicker.value = show
    }

    fun openHistoryDetail(session: WorkoutSessionEntity) {
        _historyDetail.value = session
    }

    fun dismissHistoryDetail() {
        _historyDetail.value = null
    }

    fun setConfirmAbandon(show: Boolean) {
        _confirmAbandon.value = show
    }

    fun confirmAbandonSession() {
        val s = activeSession.value ?: return
        viewModelScope.launch { repo.deleteSession(s.id) }
        _confirmAbandon.value = false
    }

    fun requestDeleteHistory(session: WorkoutSessionEntity) {
        _confirmDeleteHistory.value = session
    }

    fun dismissDeleteHistory() {
        _confirmDeleteHistory.value = null
    }

    fun confirmDeleteHistorySession() {
        val s = _confirmDeleteHistory.value ?: return
        viewModelScope.launch { repo.deleteSession(s.id) }
        _confirmDeleteHistory.value = null
    }

    /** 历史详情的组记录（对话框打开时一次性加载）。 */
    suspend fun historySets(sessionId: Long): List<WorkoutSetEntity> = repo.sessionSets(sessionId)
}
