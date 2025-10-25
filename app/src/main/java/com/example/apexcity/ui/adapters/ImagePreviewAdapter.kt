package com.example.apexcity.ui.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.apexcity.databinding.ItemImagePreviewBinding

class ImagePreviewAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : ListAdapter<Uri, ImagePreviewAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImagePreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(private val binding: ItemImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            Glide.with(binding.root.context)
                .load(uri)
                .into(binding.imageView)

            binding.removeButton.setOnClickListener {
                onRemoveClick(uri)
            }
        }
    }

    class ImageDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }
    }
}