package com.nurilov.flowexample

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion

sealed interface CoroutineTask {
    val name: String
}

interface ActiveCoroutineTask: CoroutineTask {
    val counter: StateFlow<Int>

    fun observe(ticker: Ticker)
    fun cancel()
}

interface CancelledCoroutineTask: CoroutineTask {
    val lastCounterValue: Int
}

class CancelledCoroutineTaskImpl(
    override val name: String,
    override val lastCounterValue: Int,
): CancelledCoroutineTask

class ActiveCoroutineTaskImpl(
    override val name: String,
    private val onCancellation: (ActiveCoroutineTask) -> Unit,
): ActiveCoroutineTask {

    override val counter = MutableStateFlow(0)

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun observe(ticker: Ticker) {
        coroutineScope.launch {
            ticker.ticks
                .onCompletion {
                    onCancellation(this@ActiveCoroutineTaskImpl)
                }
                .collect {
                    onTick()
                }
        }
    }

    override fun cancel() {
        coroutineScope.cancel()
    }

    private fun onTick() {
        counter.value = counter.value + 1
    }
}