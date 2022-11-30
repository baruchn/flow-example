package com.nurilov.flowexample

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.nurilov.flowexample.databinding.CoroutineRepresentationLayoutBinding
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface RunningCoroutinesDataProvider {
    val runningCoroutines: StateFlow<List<CoroutineTask>>
}

@SuppressLint("NotifyDataSetChanged")
class RunningCoroutinesAdapter(
    private val dataProvider: RunningCoroutinesDataProvider,
    private val coroutineScope: LifecycleCoroutineScope,
    private val ticker: Ticker,
): RecyclerView.Adapter<RunningCoroutinesAdapter.ViewHolder>() {

    @Suppress("PrivatePropertyName")
    private val TAG = "${RunningCoroutinesAdapter::class.simpleName}"

    private val dataset = mutableListOf<CoroutineTask>()

    init {
        coroutineScope.launchWhenResumed {
            dataProvider.runningCoroutines.collect { newTasks ->
                updateDataset(newTasks)
                notifyDataSetChanged()
            }
        }
    }

    inner class ViewHolder(
        private val binding: CoroutineRepresentationLayoutBinding
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(coroutineTask: CoroutineTask) {
            when(coroutineTask) {
                is ActiveCoroutineTask -> {
                    coroutineScope.launchWhenResumed {
                        coroutineTask.counter.collect {
                            binding.coroutineName.text = binding.root.context.getString(R.string.coroutine_item_display, coroutineTask.level, coroutineTask.identifier, coroutineTask.name, it)
                        }
                    }
                    binding.coroutineCancelButton.setOnClickListener {
                        coroutineTask.cancel()
                    }
                    binding.coroutineAddButton.setOnClickListener {
                        coroutineTask.coroutineScope.launch {
                            coroutineTask.launchChild(binding.root.context.getString(R.string.child_coroutine_name, coroutineTask.identifier), ticker)
                        }
                    }
                    binding.coroutineCancelButton.isEnabled = true
                    binding.coroutineAddButton.isEnabled = true
                }
                is CancelledCoroutineTask -> {
                    binding.coroutineName.text = binding.root.context.getString(R.string.coroutine_cancelled_item_display, coroutineTask.level, coroutineTask.identifier, coroutineTask.name, coroutineTask.lastCounterValue)
                    binding.coroutineCancelButton.isEnabled = false
                    binding.coroutineAddButton.isEnabled = false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        CoroutineRepresentationLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        ).also { binding ->
            return ViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataset[position])
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    private fun updateDataset(newData: List<CoroutineTask>) {
        dataset.clear()
        dataset.addAll(newData)
    }
}