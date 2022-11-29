package com.nurilov.flowexample

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface CoroutineRepresentation {
    val counter: StateFlow<Int>

    fun increment()
}

class CoroutineRepresentationImpl: CoroutineRepresentation {

    override val counter = MutableStateFlow(0)

    override fun increment() {
        counter.value = counter.value + 1
    }
}