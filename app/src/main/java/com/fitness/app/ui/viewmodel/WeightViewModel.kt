package com.fitness.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.TrendConclusion
import com.fitness.app.data.Trends
import com.fitness.app.data.local.WeightLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 体重页状态（第 3 期从 WeightScreen 迁出）：体重序列、趋势结论（第 4 期）
 * 与新增/编辑/删除对话框状态。
 */
class WeightViewModel(
    private val repo: ExerciseRepository
) : ViewModel() {

    /** 按 dateKey 升序：末条是最新。 */
    val weights: StateFlow<List<WeightLogEntity>> = repo.observeWeights()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val trend: StateFlow<TrendConclusion> = weights
        .map { Trends.weightTrend(it) }
        .stateIn(
            viewModelScope, SharingStarted.Eagerly,
            Trends.weightTrend(emptyList())
        )

    private val _showAdd = MutableStateFlow(false)
    val showAdd: StateFlow<Boolean> = _showAdd.asStateFlow()
    private val _editTarget = MutableStateFlow<WeightLogEntity?>(null)
    val editTarget: StateFlow<WeightLogEntity?> = _editTarget.asStateFlow()
    private val _deleteTarget = MutableStateFlow<WeightLogEntity?>(null)
    val deleteTarget: StateFlow<WeightLogEntity?> = _deleteTarget.asStateFlow()

    fun setShowAdd(show: Boolean) {
        _showAdd.value = show
    }

    fun openEdit(entry: WeightLogEntity) {
        _editTarget.value = entry
    }

    fun dismissEdit() {
        _editTarget.value = null
    }

    fun openDelete(entry: WeightLogEntity) {
        _deleteTarget.value = entry
    }

    fun dismissDelete() {
        _deleteTarget.value = null
    }

    fun saveWeight(dateKey: String, kg: Double) {
        viewModelScope.launch { repo.setWeight(dateKey, kg) }
        _showAdd.value = false
        _editTarget.value = null
    }

    fun confirmDelete() {
        val entry = _deleteTarget.value ?: return
        viewModelScope.launch { repo.removeWeight(entry.dateKey) }
        _deleteTarget.value = null
    }
}
