package com.nurilov.flowexample

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


@Suppress("PrivatePropertyName")
private val TAG = "${CoroutineTask::class.simpleName}"

sealed interface CoroutineTask {
    val name: String
    val identifier: Int
    val level: Int
}

interface ActiveCoroutineTask: CoroutineTask {
    val counter: StateFlow<Int>
    val events: SharedFlow<CoroutineEvent>
    val children: StateFlow<List<CoroutineTask>>

    val coroutineScope: CoroutineScope

    suspend fun observe(ticker: Ticker)
    fun cancel()
    suspend fun launchChild(name: String, ticker: Ticker)
}

interface CancelledCoroutineTask: CoroutineTask {
    val lastCounterValue: Int
    val children: List<CoroutineTask>
}

interface ChildCoroutineTask {
    val parent: CoroutineTask
}

open class CancelledRootCoroutineTaskImpl(
    override val name: String,
    override val lastCounterValue: Int,
    override val identifier: Int,
    override val children: List<CoroutineTask>,
): CancelledCoroutineTask {
    override val level = 0
}

class CancelledChildCoroutineTaskImpl(
    name: String,
    fromActive: ActiveCoroutineTask,
    override val parent: ActiveCoroutineTask,
): CancelledRootCoroutineTaskImpl(name, fromActive.counter.value, fromActive.identifier, fromActive.children.value), ChildCoroutineTask {
    override val level: Int = fromActive.level
}

open class ActiveRootCoroutineTaskImpl(
    override val name: String,
): ActiveCoroutineTask {

    override val counter = MutableStateFlow(0)
    override val events = MutableSharedFlowConfigured<CoroutineEvent>()
    override val children = MutableStateFlow<List<CoroutineTask>>(emptyList())
    override val identifier: Int
        get() = hashCode()
    override val level = 0

    override val coroutineScope = CoroutineScope(Dispatchers.Default)

    override suspend fun observe(ticker: Ticker) {
        coroutineScope.launch {
            //@formatter:off
            Log.d(TAG, "root task $identifier started observing ticker")
            ticker.ticks
                .onCompletion {
                    //@formatter:off
                    Log.d(TAG, "root task $identifier stopped observing ticker")
                    events.tryEmit(CoroutineEvent.Cancelled)
                }
                .collect {
                    onTick()
                }
        }
    }

    override fun cancel() {
        Log.d(TAG, "root task $identifier cancel() called")
        coroutineScope.cancel()
    }

    override suspend fun launchChild(name: String, ticker: Ticker) {
        coroutineScope.launch {
            doLaunchChild(name, ticker)
        }
    }

    protected suspend fun CoroutineScope.doLaunchChild(name: String, ticker: Ticker) {
        val child = ActiveChildCoroutineTaskImpl(name, this, this@ActiveRootCoroutineTaskImpl)
        children.value = children.value + child
        events.tryEmit(CoroutineEvent.ChildrenChanged)
        child.observe(ticker)
        child.events.collectLatest { event ->
            when (event) {
                is CoroutineEvent.Cancelled -> {
                    children.replaceWithCancelledChildTask(child)
                    events.tryEmit(CoroutineEvent.ChildrenChanged)
                }
                CoroutineEvent.ChildrenChanged -> {
                    events.tryEmit(CoroutineEvent.ChildrenChanged)
                }
            }
        }
    }

    private fun onTick() {
        counter.value = counter.value + 1
    }

    override fun toString(): String {
        return "ActiveRootCoroutineTaskImpl(identifier='$identifier')"
    }
}

class ActiveChildCoroutineTaskImpl(
    name: String,
    override val coroutineScope: CoroutineScope,
    override val parent: ActiveCoroutineTask,
): ActiveRootCoroutineTaskImpl(name), ChildCoroutineTask {

    override val level = parent.level + 1

    override suspend fun launchChild(name: String, ticker: Ticker) = coroutineScope {
        doLaunchChild(name, ticker)
    }
}

sealed class CoroutineEvent {
    object Cancelled: CoroutineEvent()
    object ChildrenChanged: CoroutineEvent()

    override fun toString(): String {
        return "${this::class.simpleName}"
    }
}

fun MutableStateFlow<List<CoroutineTask>>.replaceWithCancelledChildTask(task: ActiveChildCoroutineTaskImpl) {
    value = value - task + CancelledChildCoroutineTaskImpl(task.name, task, task.parent)
}