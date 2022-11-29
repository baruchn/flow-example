package com.nurilov.flowexample

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

interface Ticker {
    val ticks: SharedFlow<Unit>
}

class TickerImpl: Ticker {
    override val ticks = MutableSharedFlowConfigured<Unit>()

    suspend fun startTicking(delayMillis: Long) {
        while (true) {
            delay(delayMillis)
            ticks.emit(Unit)
        }
    }
}