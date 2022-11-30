package com.nurilov.flowexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nurilov.flowexample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels<MainViewModelImpl>()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.coroutinesList.adapter = RunningCoroutinesAdapter(
            viewModel,
            lifecycleScope,
        )

        binding.coroutinesList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        binding.coroutinesAddButton.setOnClickListener {
            viewModel.startNewCoroutine(this)
        }
    }
}