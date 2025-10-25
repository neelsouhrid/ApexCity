package com.example.apexcity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.apexcity.databinding.FragmentReportIssueSelectionBinding

class ReportIssueSelectionFragment : Fragment() {

    private var _binding: FragmentReportIssueSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportIssueSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // AI Report
        binding.aiReportCard.setOnClickListener {
            navigateToAIReport()
        }

        // Category cards
        binding.adminCivicCard.setOnClickListener {
            navigateToManualReport("Administrative & Civic Services")
        }

        binding.citizenSafetyCard.setOnClickListener {
            navigateToManualReport("Citizen & Safety")
        }

        binding.infrastructureCard.setOnClickListener {
            navigateToManualReport("Infrastructure")
        }

        binding.publicUtilitiesCard.setOnClickListener {
            navigateToManualReport("Public Utilities")
        }

        binding.trafficTransportCard.setOnClickListener {
            navigateToManualReport("Traffic/Transport")
        }

        binding.environmentCard.setOnClickListener {
            navigateToManualReport("Environment & Public Spaces")
        }

        binding.othersCard.setOnClickListener {
            navigateToManualReport("Other")
        }
    }

    private fun navigateToAIReport() {
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, AIReportFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToManualReport(category: String) {
        val fragment = ManualReportFragment().apply {
            arguments = Bundle().apply {
                putString("category", category)
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