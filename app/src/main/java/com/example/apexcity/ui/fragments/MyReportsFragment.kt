package com.example.apexcity.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apexcity.R
import com.example.apexcity.data.api.RetrofitClient
import com.example.apexcity.data.model.Complaint
import com.example.apexcity.databinding.FragmentMyReportsBinding
import com.example.apexcity.ui.adapters.IssueAdapter
import kotlinx.coroutines.launch

class MyReportsFragment : Fragment() {

    private var _binding: FragmentMyReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var issueAdapter: IssueAdapter
    private var allComplaints = listOf<Complaint>()
    private var filteredComplaints = listOf<Complaint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        setupSearch()
        loadReports()
    }

    private fun setupRecyclerView() {
        issueAdapter = IssueAdapter()
        issueAdapter.setOnItemClickListener { complaint ->
            // Navigate to complaint detail
            navigateToDetail(complaint)
        }
        
        binding.reportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = issueAdapter
        }
    }

    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.allChip -> filterByStatus(null)
                    R.id.pendingChip -> filterByStatus("Pending")
                    R.id.progressChip -> filterByStatus("In Progress")
                    R.id.resolvedChip -> filterByStatus("Resolved")
                }
            }
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchReports(s.toString())
            }
        })
    }

    private fun loadReports() {
        binding.progressBar.visibility = View.VISIBLE
        binding.reportsRecyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMyComplaints()
                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    allComplaints = response.body()!!
                    filteredComplaints = allComplaints
                    updateUI()
                } else {
                    showEmptyState()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showEmptyState()
            }
        }
    }

    private fun filterByStatus(status: String?) {
        filteredComplaints = if (status == null) {
            allComplaints
        } else {
            allComplaints.filter { it.status == status }
        }
        updateUI()
    }

    private fun searchReports(query: String) {
        filteredComplaints = if (query.isEmpty()) {
            allComplaints
        } else {
            allComplaints.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.location.address.contains(query, ignoreCase = true)
            }
        }
        updateUI()
    }

    private fun updateUI() {
        if (filteredComplaints.isEmpty()) {
            showEmptyState()
        } else {
            binding.emptyState.visibility = View.GONE
            binding.reportsRecyclerView.visibility = View.VISIBLE
            issueAdapter.submitList(filteredComplaints)
        }
    }

    private fun showEmptyState() {
        binding.reportsRecyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun navigateToDetail(complaint: Complaint) {
        val fragment = ComplaintDetailFragment().apply {
            arguments = Bundle().apply {
                putString("complaint_id", complaint.id)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}