package com.nurilov.flowexample

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface MainViewModel: RunningCoroutinesDataProvider {
    val ticker: Ticker

    fun startNewCoroutine(name: String)
}

class MainViewModelImpl: ViewModel(), MainViewModel {

    @Suppress("PrivatePropertyName")
    private val TAG = "CoroutineTask:${MainViewModel::class.simpleName}"

    override val runningCoroutines = MutableStateFlow<List<CoroutineTask>>(emptyList())

    override val ticker = TickerImpl()

    private val lock = ReentrantLock()

    init {
        // this observation and all other observations on viewModelScope will be cancelled when the ViewModel is cleared by the viewModelScope
        viewModelScope.launch {
            ticker.startTicking(1000)
        }
    }

    override fun startNewCoroutine(name: String) {
        runningCoroutines.value = runningCoroutines.value + ActiveRootCoroutineTaskImpl(name).also { newRootTask ->

            //@formatter:off
            Log.d(TAG, "startNewCoroutine: starting new coroutine with identifier: ${newRootTask.identifier}")

            viewModelScope.launch {
                newRootTask.observe(ticker)
                newRootTask.events.collect { event ->
                    Log.d(TAG, "startNewCoroutine: event: $event from ${newRootTask.identifier}")
                    when(event) {
                        is CoroutineEvent.Cancelled -> {
                            lock.withLock {
                                runningCoroutines.replaceWithCancelledTask(newRootTask)
                            }
                        }
                        CoroutineEvent.ChildrenChanged -> {
                            lock.withLock {
                                onChildrenChanged(runningCoroutines.value.first { it.identifier == newRootTask.identifier })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onChildrenChanged(task: CoroutineTask) {
        Log.d(TAG, "onChildrenChanged() called with: task = ${task}, children = ${task.children}, ${runningCoroutines.value}")
        removeTaskWithChildren(task)
        Log.d(TAG, "onChildrenChanged1: runningCoroutines.value: ${runningCoroutines.value}")
        addTaskWithChildren(task)
        Log.d(TAG, "onChildrenChanged2: runningCoroutines.value: ${runningCoroutines.value}")
    }

    private fun removeTaskWithChildren(task: CoroutineTask) {
        runningCoroutines.value = runningCoroutines.value.filter { it.identifier != task.identifier }
        task.children.forEach { child ->
            removeTaskWithChildren(child)
        }
    }

    private fun addTaskWithChildren(task: CoroutineTask) {
        runningCoroutines.value = runningCoroutines.value + task
        task.children.sortedBy { it.level }.forEach { child ->
            addTaskWithChildren(child)
        }
    }
}

fun MutableStateFlow<List<CoroutineTask>>.replaceWithCancelledTask(task: ActiveCoroutineTask) {
    value = value - task + CancelledRootCoroutineTaskImpl(task.name, task.counter.value, task.identifier, task.children)
}