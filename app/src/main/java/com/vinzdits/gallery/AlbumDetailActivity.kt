package com.vinzdits.gallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.vinzdits.gallery.adapter.PhotoAdapter
import com.vinzdits.gallery.data.MediaStoreRepository
import com.vinzdits.gallery.databinding.ActivityAlbumDetailBinding
import com.vinzdits.gallery.util.FavoritesManager

/** Isi satu album: grid foto + tap untuk buka viewer. */
class AlbumDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailBinding
    private val viewModel: PhotoViewModel by viewModels()
    private lateinit var adapter: PhotoAdapter

    private var bucketId: String = ""
    private var mode: String = ViewerActivity.MODE_ALBUM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bucketId = intent.getStringExtra(EXTRA_BUCKET_ID).orEmpty()
        val albumName = intent.getStringExtra(EXTRA_ALBUM_NAME).orEmpty()
        mode = if (bucketId == MediaStoreRepository.FAVORITES_BUCKET_ID) {
            ViewerActivity.MODE_FAVORITES
        } else {
            ViewerActivity.MODE_ALBUM
        }

        binding.toolbar.title = albumName
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = PhotoAdapter { _, position ->
            ViewerActivity.start(this, mode, bucketId, position)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.photos.observe(this) { all ->
            val list = when (mode) {
                ViewerActivity.MODE_FAVORITES -> {
                    val favIds = FavoritesManager.getInstance(this).getFavoriteIds()
                    all.filter { favIds.contains(it.id) }
                }
                else -> all.filter { it.bucketId == bucketId }
            }
            adapter.submitList(list)
            binding.emptyView.visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    companion object {
        private const val EXTRA_BUCKET_ID = "extra_bucket_id"
        private const val EXTRA_ALBUM_NAME = "extra_album_name"

        fun start(context: Context, bucketId: String, albumName: String) {
            val intent = Intent(context, AlbumDetailActivity::class.java).apply {
                putExtra(EXTRA_BUCKET_ID, bucketId)
                putExtra(EXTRA_ALBUM_NAME, albumName)
            }
            context.startActivity(intent)
        }
    }
}
