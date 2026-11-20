package com.fitness.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 用仓库（Application 持有的进程内单例）构造 ViewModel 的通用工厂。
 * 页面里：viewModel(factory = repoViewModelFactory { XxxViewModel(repo) })
 */
fun <T : ViewModel> repoViewModelFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <R : ViewModel> create(modelClass: Class<R>): R = create() as R
    }
