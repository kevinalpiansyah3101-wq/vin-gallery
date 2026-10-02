package com.vinzdits.gallery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.vinzdits.gallery.adapter.ViewerPagerAdapter
import com.vinzdits.gallery.data.MediaStoreRepository
import com.vinzdits.gallery.data.Photo
import com.vinzdits.gallery.databinding.ActivityViewerBinding
import com.vinzdits.gallery.util.FavoritesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Viewer fullscreen: geser kiri-kanan antar foto, pinch-to-zoom,
 * tampil tanggal & nama file, aksi bagikan / hapus / favorit.
 */
class ViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewerBinding
    private lateinit var adapter: ViewerPagerAdapter
    private lateinit var favoritesManager: FavoritesManager

    private var mode: String = MODE_ALL
    private var bucketId: String? = null
    private var startPosition: Int = 0
    private var uiVisible: Boolean = true
    private var pendingDeletePhoto: Photo? = null

    private val dateFormat = SimpleDateFormat("d MMMM yyyy • HH:mm", Locale("id", "ID"))

    private val deleteIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val photo = pendingDeletePhoto
        pendingDeletePhoto = null
        if (result.resultCode == Activity.RESULT_OK && photo != null) {
            onPhotoDeleted(photo)
        } else {
            Toast.makeText(this, R.string.delete_cancelled, Toast.LENGTH_SHORT).show()
        }
    }

    private val writePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val photo = pendingDeletePhoto
        if (granted && photo != null) {
            doDeleteLegacy(photo)
        } else {
            pendingDeletePhoto = null
            Toast.makeText(this, R.string.delete_cancelled, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        favoritesManager = FavoritesManager.getInstance(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ALL
        bucketId = intent.getStringExtra(EXTRA_BUCKET_ID)
        startPosition = intent.getIntExtra(EXTRA_POSITION, 0)

        adapter = ViewerPagerAdapter(onPhotoTap = { toggleUi() })
        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnShare.setOnClickListener { currentPhoto()?.let { sharePhoto(it) } }
        binding.btnDelete.setOnClickListener { currentPhoto()?.let { confirmDelete(it) } }

        loadPhotos()
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateInfo()
        }
    }

    private fun loadPhotos() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resolver = contentResolver
            val photos = when (mode) {
                MODE_ALBUM -> {
                    val id = bucketId.orEmpty()
                    MediaStoreRepository.queryPhotosByBucket(resolver, id)
                }
                MODE_FAVORITES -> {
                    val ids = favoritesManager.getFavoriteIds()
                    MediaStoreRepository.queryPhotosByIds(resolver, ids)
                }
                else -> MediaStoreRepository.queryAllPhotos(resolver)
            }
            withContext(Dispatchers.Main) {
                if (photos.isEmpty()) {
                    finish()
                    return@withContext
                }
                adapter.setPhotos(photos)
                binding.viewPager.setCurrentItem(startPosition.coerceIn(photos.indices), false)
                updateInfo()
            }
        }
    }

    private fun currentPhoto(): Photo? {
        val pos = binding.viewPager.currentItem
        return if (pos in 0 until adapter.size()) adapter.getPhoto(pos) else null
    }

    private fun updateInfo() {
        val photo = currentPhoto() ?: return
        binding.tvTitle.text = photo.displayName
        binding.tvDate.text = dateFormat.format(photo.dateAddedMillis)
        updateFavoriteIcon(photo)
    }

    private fun toggleUi() {
        uiVisible = !uiVisible
        binding.topBar.isVisible = uiVisible
        binding.bottomBar.isVisible = uiVisible
    }

    // ---------- Favorit ----------

    private fun toggleFavorite() {
        val photo = currentPhoto() ?: return
        val nowFavorite = favoritesManager.toggleFavorite(photo.id)
        updateFavoriteIcon(photo)
        Toast.makeText(
            this,
            if (nowFavorite) R.string.added_to_favorites else R.string.removed_from_favorites,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateFavoriteIcon(photo: Photo) {
        val res = if (favoritesManager.isFavorite(photo.id)) {
            R.drawable.ic_star
        } else {
            R.drawable.ic_star_border
        }
        binding.btnFavorite.setImageResource(res)
    }

    // ---------- Bagikan ----------

    private fun sharePhoto(photo: Photo) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, photo.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
    }

    // ---------- Hapus ----------

    private fun confirmDelete(photo: Photo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, photo.displayName))
            .setPositiveButton(R.string.delete_confirm) { _, _ -> performDelete(photo) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performDelete(photo: Photo) {
        pendingDeletePhoto = photo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: pakai system delete request (tanpa permission tambahan).
            val request = MediaStore.createDeleteRequest(contentResolver, listOf(photo.uri))
            deleteIntentLauncher.launch(
                IntentSenderRequest.Builder(request.intentSender).build()
            )
        } else {
            // API 24–29: butuh WRITE_EXTERNAL_STORAGE.
            if (hasWritePermission()) {
                doDeleteLegacy(photo)
            } else {
                writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun hasWritePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun doDeleteLegacy(photo: Photo) {
        try {
            val rows = contentResolver.delete(photo.uri, null, null)
            pendingDeletePhoto = null
            if (rows > 0) {
                onPhotoDeleted(photo)
            } else {
                Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show()
            }
        } catch (e: android.app.RecoverableSecurityException) {
            // API 29: sistem minta konfirmasi user.
            deleteIntentLauncher.launch(
                IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
            )
        } catch (e: SecurityException) {
            pendingDeletePhoto = null
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPhotoDeleted(photo: Photo) {
        val pos = binding.viewPager.currentItem
        adapter.removeAt(pos)
        Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show()
        if (adapter.size() == 0) {
            finish()
        } else {
            binding.viewPager.setCurrentItem(pos.coerceIn(0, adapter.size() - 1), false)
            updateInfo()
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_BUCKET_ID = "extra_bucket_id"
        const val EXTRA_POSITION = "extra_position"

        const val MODE_ALL = "all"
        const val MODE_ALBUM = "album"
        const val MODE_FAVORITES = "favorites"

        fun start(context: Context, mode: String, bucketId: String?, position: Int) {
            val intent = Intent(context, ViewerActivity::class.java).apply {
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_BUCKET_ID, bucketId)
                putExtra(EXTRA_POSITION, position)
            }
            context.startActivity(intent)
        }
    }
}
