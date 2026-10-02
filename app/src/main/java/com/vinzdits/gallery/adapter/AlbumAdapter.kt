package com.vinzdits.gallery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vinzdits.gallery.R
import com.vinzdits.gallery.data.Album
import com.vinzdits.gallery.databinding.ItemAlbumBinding

/** Grid album: cover + nama + jumlah foto. */
class AlbumAdapter(
    private val onClick: (album: Album) -> Unit
) : ListAdapter<Album, AlbumAdapter.AlbumViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlbumViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AlbumViewHolder(
        private val binding: ItemAlbumBinding,
        private val onClick: (album: Album) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: Album) {
            val cover = album.coverUri
            if (cover != null) {
                binding.cover.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                Glide.with(binding.cover)
                    .load(cover)
                    .centerCrop()
                    .placeholder(R.color.photo_placeholder)
                    .into(binding.cover)
            } else {
                Glide.with(binding.cover).clear(binding.cover)
                binding.cover.setImageResource(R.drawable.ic_star)
                binding.cover.scaleType = android.widget.ImageView.ScaleType.CENTER
            }
            binding.name.text = album.name
            binding.count.text = binding.root.context.getString(R.string.photo_count, album.count)
            binding.root.setOnClickListener { onClick(album) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Album>() {
            override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean =
                oldItem.bucketId == newItem.bucketId

            override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean =
                oldItem == newItem
        }
    }
}
