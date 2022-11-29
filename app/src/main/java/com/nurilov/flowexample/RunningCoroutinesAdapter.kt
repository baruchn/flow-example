package com.nurilov.flowexample

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.nurilov.flowexample.databinding.CoroutineRepresentationLayoutBinding
import kotlinx.coroutines.flow.StateFlow

interface RunningCoroutinesDataProvider {
    val runningCoroutines: StateFlow<List<CoroutineRepresentation>>
}

class RunningCoroutinesAdapter(
    private val dataProvider: RunningCoroutinesDataProvider,
    private val coroutineScope: LifecycleCoroutineScope,
): RecyclerView.Adapter<RunningCoroutinesAdapter.ViewHolder>() {

    private val dataset = mutableListOf<CoroutineRepresentation>()

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
        fun bind(coroutineRepresentation: CoroutineRepresentation) {
            coroutineScope.launchWhenResumed {
                coroutineRepresentation.counter.collect {
                    binding.coroutineCounter.text = it.toString()
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

    private fun updateDataset(newData: List<CoroutineRepresentation>) {
        dataset.clear()
        dataset.addAll(newData)
    }
}