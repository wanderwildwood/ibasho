package com.wanderwildwood.ibasho.ui.settings

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.databinding.ActivityDebuggingBinding
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import com.wanderwildwood.ibasho.utils.Utils
import kotlinx.coroutines.launch


class DebuggingActivity : FmdActivity() {

    private lateinit var viewBinding: ActivityDebuggingBinding

    private val viewModel: DebuggingViewModel by viewModels { DebuggingViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityDebuggingBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentJobs.collect {
                    setupJobs(it, viewBinding.textViewCurrentJobs)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentJobs.collect {
                    setupJobs(it, viewBinding.textViewRecentJobs)
                }
            }
        }
    }

    private fun setupJobs(jobs: List<JobInfoExt>, view: TextView) {
        val jobInfoString = jobs.joinToString("\n\n") { it.toInfoString() }

        // XXX: A RecyclerView would be cleaner, but a TextView is quicker and good enough for debugging
        view.text = jobInfoString

        view.setOnLongClickListener {
            Utils.copyToClipboard(it.context, "", jobInfoString)
            return@setOnLongClickListener true
        }
    }
}
