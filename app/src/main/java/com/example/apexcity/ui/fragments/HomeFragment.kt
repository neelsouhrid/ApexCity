package com.example.apexcity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apexcity.databinding.FragmentHomeBinding
import com.example.apexcity.data.api.RetrofitClient
import com.example.apexcity.ui.adapters.IssueAdapter
import com.example.apexcity.utils.SessionManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var issueAdapter: IssueAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        setupRecyclerView()
        loadData()

        binding.reportFab.setOnClickListener {
            navigateToReportIssue()
        }

        binding.reportFirstIssueButton.setOnClickListener {
            navigateToReportIssue()
        }
    }

    private fun setupRecyclerView() {
        issueAdapter = IssueAdapter()
        binding.issuesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = issueAdapter
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                // Load stats
                val statsResponse = RetrofitClient.apiService.getDashboardStats()
                if (statsResponse.isSuccessful && statsResponse.body() != null) {
                    val stats = statsResponse.body()!!
                    binding.pendingCount.text = stats.pending.toString()
                    binding.progressCount.text = stats.inProgress.toString()
                    binding.resolvedCount.text = stats.resolved.toString()
                }

                // Load recent issues
                val issuesResponse = RetrofitClient.apiService.getMyComplaints()
                if (issuesResponse.isSuccessful && issuesResponse.body() != null) {
                    val issues = issuesResponse.body()!!
                    if (issues.isEmpty()) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.issuesRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.issuesRecyclerView.visibility = View.VISIBLE
                        issueAdapter.submitList(issues.take(5))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun navigateToReportIssue() {
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, ReportIssueSelectionFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}