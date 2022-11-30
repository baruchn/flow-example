package com.nurilov.flowexample

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface MainViewModel: RunningCoroutinesDataProvider {
    fun startNewCoroutine(context: Context)
}

class MainViewModelImpl: ViewModel(), MainViewModel {
    override val runningCoroutines = MutableStateFlow<List<CoroutineTask>>(listOf())

    private val ticker = TickerImpl()

    init {
        // this observation will be cancelled when the ViewModel is cleared by the viewModelScope
        viewModelScope.launch {
            ticker.startTicking(1000)
        }
    }

    override fun startNewCoroutine(context: Context) {
        runningCoroutines.value = runningCoroutines.value + ActiveCoroutineTaskImpl(context.getString(R.string.root_coroutine_name)) {
            runningCoroutines.value = runningCoroutines.value - it + CancelledCoroutineTaskImpl(it.name, it.counter.value)
        }.also {
            it.observe(ticker)
        }
    }
}