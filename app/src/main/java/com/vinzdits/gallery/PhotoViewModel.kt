package com.vinzdits.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vinzdits.gallery.data.Album
import com.vinzdits.gallery.data.MediaStoreRepository
import com.vinzdits.gallery.data.Photo
import com.vinzdits.gallery.util.FavoritesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel bersama (activity-scoped) untuk daftar foto & album.
 */
class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val _photos = MutableLiveData<List<Photo>>(emptyList())
    val photos: LiveData<List<Photo>> = _photos

    private val _albums = MutableLiveData<List<Album>>(emptyList())
    val albums: LiveData<List<Album>> = _albums

    private val favoritesManager = FavoritesManager.getInstance(application)

    /** Muat ulang foto & album dari MediaStore (idempotent, aman dipanggil berulang). */
    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val all = MediaStoreRepository.queryAllPhotos(resolver)
            val favIds = favoritesManager.getFavoriteIds()
            val albums = MediaStoreRepository.buildAlbums(all, favIds)
            _photos.postValue(all)
            _albums.postValue(albums)
        }
    }
}
