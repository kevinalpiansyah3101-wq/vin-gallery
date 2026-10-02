package com.vinzdits.gallery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vinzdits.gallery.data.Photo
import com.vinzdits.gallery.databinding.ItemViewerPageBinding

/** Halaman viewer fullscreen: satu foto dengan pinch-to-zoom per halaman. */
class ViewerPagerAdapter(
    private val onPhotoTap: () -> Unit
) : RecyclerView.Adapter<ViewerPagerAdapter.ViewerViewHolder>() {

    private val items = mutableListOf<Photo>()

    fun setPhotos(photos: List<Photo>) {
        items.clear()
        items.addAll(photos)
        notifyDataSetChanged()
    }

    fun getPhoto(position: Int): Photo = items[position]

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun size(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewerViewHolder {
        val binding = ItemViewerPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewerViewHolder(
        private val binding: ItemViewerPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Photo) {
            binding.zoomableImage.resetZoom()
            binding.zoomableImage.onSingleTap = onPhotoTap
            Glide.with(binding.zoomableImage)
                .load(photo.uri)
                .into(binding.zoomableImage)
        }
    }
}
