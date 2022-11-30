package com.nurilov.flowexample

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@JvmInline
value class CoroutineTaskIdentifier(val value: Int)
val Int.asCoroutineTaskIdentifier: CoroutineTaskIdentifier get() = CoroutineTaskIdentifier(this)

@Suppress("PrivatePropertyName")
private val TAG = "${CoroutineTask::class.simpleName}"

sealed interface CoroutineTask {
    val name: String
    val identifier: CoroutineTaskIdentifier
    val level: Int
    val children: List<CoroutineTask>
}

interface ActiveCoroutineTask: CoroutineTask {
    val counter: StateFlow<Int>
    val events: SharedFlow<CoroutineEvent>

    val coroutineScope: CoroutineScope

    suspend fun observe(ticker: Ticker)
    fun cancel()
    suspend fun launchChild(name: String, ticker: Ticker)
}

interface CancelledCoroutineTask: CoroutineTask {
    val lastCounterValue: Int
}

interface ChildCoroutineTask {
    val parent: CoroutineTask
}

open class CancelledRootCoroutineTaskImpl(
    override val name: String,
    override val lastCounterValue: Int,
    override val identifier: CoroutineTaskIdentifier,
    override val children: List<CoroutineTask>,
): CancelledCoroutineTask {
    override val level = 0
}

class CancelledChildCoroutineTaskImpl(
    fromActive: ActiveChildCoroutineTaskImpl,
): CancelledRootCoroutineTaskImpl(fromActive.name, fromActive.counter.value, fromActive.identifier, fromActive.children), ChildCoroutineTask {
    override val level: Int = fromActive.level
    override val parent = fromActive.parent
}

open class ActiveRootCoroutineTaskImpl(
    override val name: String,
): ActiveCoroutineTask {

    override val counter = MutableStateFlow(0)
    override val events = MutableSharedFlowConfigured<CoroutineEvent>()
    override val children: MutableList<CoroutineTask> = mutableListOf()
    final override val identifier: CoroutineTaskIdentifier
        get() = hashCode().asCoroutineTaskIdentifier
    override val level = 0

    override val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        IndexKeeper.taskCreated(null, identifier)
    }

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
        children.add(child)
        events.tryEmit(CoroutineEvent.ChildrenChanged)
        child.observe(ticker)
        child.events.collectLatest { event ->
            when (event) {
                is CoroutineEvent.Cancelled -> {
                    children.remove(child)
                    children.add(CancelledChildCoroutineTaskImpl(child))
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

    init {
        IndexKeeper.taskCreated(parent.identifier, identifier)
    }

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