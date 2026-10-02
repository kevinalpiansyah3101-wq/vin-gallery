package com.vinzdits.gallery.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Menyimpan ID foto favorit di SharedPreferences.
 * Thread-safe singleton.
 */
class FavoritesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFavorite(photoId: Long): Boolean =
        prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().contains(photoId.toString())

    /**
     * Toggle status favorit. Return true jika sekarang menjadi favorit.
     */
    fun toggleFavorite(photoId: Long): Boolean {
        val current = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        val idStr = photoId.toString()
        val nowFavorite = if (current.contains(idStr)) {
            current.remove(idStr)
            false
        } else {
            current.add(idStr)
            true
        }
        prefs.edit().putStringSet(KEY_IDS, current).apply()
        return nowFavorite
    }

    fun getFavoriteIds(): Set<Long> =
        prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    companion object {
        private const val PREFS_NAME = "vin_gallery_prefs"
        private const val KEY_IDS = "favorite_ids"

        @Volatile
        private var instance: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager =
            instance ?: synchronized(this) {
                instance ?: FavoritesManager(context).also { instance = it }
            }
    }
}
