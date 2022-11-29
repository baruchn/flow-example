package com.nurilov.flowexample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface MainViewModel: RunningCoroutinesDataProvider {
    fun startNewCoroutine()
}

class MainViewModelImpl: ViewModel(), MainViewModel {
    override val runningCoroutines = MutableStateFlow<List<CoroutineRepresentation>>(listOf())

    private val ticker = TickerImpl()

    init {
        viewModelScope.launch {
            ticker.ticks.collect {
                runningCoroutines.value.forEach { coroutineRepresentation ->
                    coroutineRepresentation.increment()
                }
            }
        }

        viewModelScope.launch {
            ticker.startTicking(1000)
        }
    }

    override fun startNewCoroutine() {
        runningCoroutines.value = runningCoroutines.value + CoroutineRepresentationImpl()
    }
}