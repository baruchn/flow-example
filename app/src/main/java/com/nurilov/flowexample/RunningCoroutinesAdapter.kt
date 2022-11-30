package com.nurilov.flowexample

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.nurilov.flowexample.databinding.CoroutineRepresentationLayoutBinding
import kotlinx.coroutines.flow.StateFlow

interface RunningCoroutinesDataProvider {
    val runningCoroutines: StateFlow<List<CoroutineTask>>
}

@SuppressLint("NotifyDataSetChanged")
class RunningCoroutinesAdapter(
    private val dataProvider: RunningCoroutinesDataProvider,
    private val coroutineScope: LifecycleCoroutineScope,
): RecyclerView.Adapter<RunningCoroutinesAdapter.ViewHolder>() {

    private val dataset = mutableListOf<CoroutineTask>()

    init {
        coroutineScope.launchWhenResumed {
            dataProvider.runningCoroutines.collect {
                updateDataset(it)
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
                            binding.coroutineName.text = binding.root.context.getString(R.string.coroutine_item_display, coroutineTask.hashCode(), coroutineTask.name, it)
                        }
                    }
                    binding.coroutineCancelButton.setOnClickListener {
                        coroutineTask.cancel()
                    }
                }
                is CancelledCoroutineTask -> {
                    binding.coroutineCancelButton.isEnabled = false
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