package com.example.apexcity.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.apexcity.R
import com.example.apexcity.data.model.Complaint
import com.example.apexcity.databinding.ItemIssueBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class IssueAdapter : ListAdapter<Complaint, IssueAdapter.IssueViewHolder>(IssueDiffCallback()) {

    private var onItemClickListener: ((Complaint) -> Unit)? = null

    fun setOnItemClickListener(listener: (Complaint) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val binding = ItemIssueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IssueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IssueViewHolder(private val binding: ItemIssueBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(getItem(position))
                }
            }
        }

        fun bind(complaint: Complaint) {
            binding.apply {
                issueTitle.text = complaint.title
                issueDescription.text = complaint.description
                issueLocation.text = complaint.location.address
                issueStatus.text = complaint.status
                
                // Set status badge color
                val statusColor = when (complaint.status) {
                    "Pending" -> R.color.status_pending
                    "In Progress" -> R.color.status_progress
                    "Resolved" -> R.color.status_resolved
                    else -> R.color.dark_blue
                }
                issueStatus.setBackgroundResource(getStatusBackground(complaint.status))

                // Load image
                if (complaint.images.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(complaint.images[0].url)
                        .placeholder(R.drawable.placeholder_image)
                        .into(issueImage)
                }

                // Format date
                issueDate.text = getTimeAgo(complaint.createdAt)
            }
        }

        private fun getStatusBackground(status: String): Int {
            return when (status) {
                "Pending" -> R.drawable.status_pending_bg
                "In Progress" -> R.drawable.status_progress_bg
                "Resolved" -> R.drawable.status_resolved_bg
                else -> R.drawable.status_badge
            }
        }

        private fun getTimeAgo(dateString: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(dateString) ?: return "Unknown"
                val now = Date()
                val diff = now.time - date.time

                val days = TimeUnit.MILLISECONDS.toDays(diff)
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

                when {
                    days > 0 -> "$days day${if (days > 1) "s" else ""}"
                    hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
                    minutes > 0 -> "$minutes min${if (minutes > 1) "s" else ""}"
                    else -> "Just now"
                }
            } catch (e: Exception) {
                "Unknown"
            }
        }
    }

    class IssueDiffCallback : DiffUtil.ItemCallback<Complaint>() {
        override fun areItemsTheSame(oldItem: Complaint, newItem: Complaint): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Complaint, newItem: Complaint): Boolean {
            return oldItem == newItem
        }
    }
}