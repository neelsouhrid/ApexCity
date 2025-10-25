package com.example.apexcity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.apexcity.R
import com.example.apexcity.data.model.Issue

class ComplaintDetailFragment : Fragment() {
    
    private var issue: Issue? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            issue = it.getParcelable("issue")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_complaint_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        issue?.let { setupUI(view, it) }
    }
    
    private fun setupUI(view: View, issue: Issue) {
        // Header
        view.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        view.findViewById<TextView>(R.id.tvComplaintId)?.text = "ID: ${issue.id}"
        
        // Issue Image
        val imageView = view.findViewById<ImageView>(R.id.ivIssueImage)
        if (issue.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(issue.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(imageView)
        }
        
        // Issue Details
        view.findViewById<TextView>(R.id.tvIssueTitle)?.text = issue.title
        view.findViewById<TextView>(R.id.tvIssueCategory)?.text = issue.category
        view.findViewById<TextView>(R.id.tvIssueDescription)?.text = issue.description
        view.findViewById<TextView>(R.id.tvIssueLocation)?.text = issue.location
        view.findViewById<TextView>(R.id.tvIssueDate)?.text = formatDate(issue.createdAt)
        
        // Status
        val statusText = view.findViewById<TextView>(R.id.tvStatus)
        val statusColor = when (issue.status.lowercase()) {
            "pending" -> "#FCA311"
            "in progress" -> "#3498db"
            "resolved" -> "#27ae60"
            "rejected" -> "#e74c3c"
            else -> "#95a5a6"
        }
        statusText?.apply {
            text = issue.status
            setTextColor(android.graphics.Color.parseColor(statusColor))
        }
        
        // Progress Bar
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val progressText = view.findViewById<TextView>(R.id.tvProgressPercentage)
        val progress = when (issue.status.lowercase()) {
            "pending" -> 25
            "in progress" -> 60
            "resolved" -> 100
            else -> 0
        }
        progressBar?.progress = progress
        progressText?.text = "$progress%"
        
        // Timeline
        setupTimeline(view, issue)
        
        // Action Buttons
        view.findViewById<Button>(R.id.btnUpdateStatus)?.setOnClickListener {
            showUpdateStatusDialog()
        }
        
        view.findViewById<Button>(R.id.btnShareComplaint)?.setOnClickListener {
            shareComplaint(issue)
        }
    }
    
    private fun setupTimeline(view: View, issue: Issue) {
        val timelineContainer = view.findViewById<LinearLayout>(R.id.timelineContainer)
        
        val timeline = listOf(
            TimelineItem("Submitted", issue.createdAt, true),
            TimelineItem("Under Review", issue.createdAt + 86400000, issue.status != "pending"),
            TimelineItem("In Progress", issue.createdAt + 172800000, issue.status == "in progress" || issue.status == "resolved"),
            TimelineItem("Resolved", issue.createdAt + 259200000, issue.status == "resolved")
        )
        
        timeline.forEach { item ->
            addTimelineItem(timelineContainer, item)
        }
    }
    
    private fun addTimelineItem(container: LinearLayout, item: TimelineItem) {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_timeline, container, false)
        
        itemView.findViewById<TextView>(R.id.tvTimelineTitle)?.text = item.title
        itemView.findViewById<TextView>(R.id.tvTimelineDate)?.text = formatDate(item.date)
        
        val indicator = itemView.findViewById<View>(R.id.timelineIndicator)
        val line = itemView.findViewById<View>(R.id.timelineLine)
        
        if (item.completed) {
            indicator?.setBackgroundResource(R.drawable.circle_timeline_completed)
            line?.setBackgroundColor(android.graphics.Color.parseColor("#FCA311"))
        } else {
            indicator?.setBackgroundResource(R.drawable.circle_timeline_pending)
            line?.setBackgroundColor(android.graphics.Color.parseColor("#E5E5E5"))
        }
        
        container.addView(itemView)
    }
    
    private fun showUpdateStatusDialog() {
        val statuses = arrayOf("Pending", "In Progress", "Resolved", "Rejected")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Update Status")
            .setItems(statuses) { _, which ->
                val newStatus = statuses[which]
                updateIssueStatus(newStatus)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateIssueStatus(newStatus: String) {
        // TODO: Update status via API
        Toast.makeText(requireContext(), "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
    }
    
    private fun shareComplaint(issue: Issue) {
        val shareText = """
            Complaint ID: ${issue.id}
            Title: ${issue.title}
            Category: ${issue.category}
            Location: ${issue.location}
            Status: ${issue.status}
            
            Description: ${issue.description}
        """.trimIndent()
        
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
    
    data class TimelineItem(
        val title: String,
        val date: Long,
        val completed: Boolean
    )
    
    companion object {
        fun newInstance(issue: Issue) = ComplaintDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable("issue", issue)
            }
        }
    }
}