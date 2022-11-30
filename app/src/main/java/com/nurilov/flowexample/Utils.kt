package com.nurilov.flowexample

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

@Suppress("FunctionName")
internal fun <T> MutableSharedFlowConfigured(): MutableSharedFlow<T> {
    return MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
}

object IndexKeeper {
    private val indexes = mutableListOf<CoroutineTaskIdentifier>()

    fun taskCreated(parentIdentifier: CoroutineTaskIdentifier?, taskIdentifier: CoroutineTaskIdentifier) {
        parentIdentifier?.let {
            indexes.indexOf(it).let { parentIndex ->
                indexes.add(parentIndex + 1, taskIdentifier)
            }
        } ?: let {
            indexes.add(taskIdentifier)
        }
    }

    fun getIndexForTask(taskIdentifier: CoroutineTaskIdentifier): Int {
        return indexes.indexOf(taskIdentifier)
    }
}