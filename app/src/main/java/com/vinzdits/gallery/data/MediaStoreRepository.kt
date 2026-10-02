package com.vinzdits.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore

/**
 * Akses baca ke MediaStore. Semua query berjalan di thread IO
 * (dipanggil dari coroutine / background thread oleh pemanggil).
 */
object MediaStoreRepository {

    /** ID semu untuk album "Favorit" (bukan bucket MediaStore asli). */
    const val FAVORITES_BUCKET_ID = "__favorites__"

    private val PROJECTION = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    private const val SORT_ORDER = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    private fun imagesCollection(): android.net.Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    /** Semua foto, urutan terbaru dulu. */
    fun queryAllPhotos(resolver: ContentResolver): List<Photo> =
        queryPhotos(resolver, selection = null, selectionArgs = null)

    /** Foto dalam satu bucket/album. */
    fun queryPhotosByBucket(resolver: ContentResolver, bucketId: String): List<Photo> =
        queryPhotos(
            resolver,
            selection = "${MediaStore.Images.Media.BUCKET_ID} = ?",
            selectionArgs = arrayOf(bucketId)
        )

    /** Foto berdasarkan sekumpulan ID (untuk album Favorit). */
    fun queryPhotosByIds(resolver: ContentResolver, ids: Set<Long>): List<Photo> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(separator = ",") { "?" }
        return queryPhotos(
            resolver,
            selection = "${MediaStore.Images.Media._ID} IN ($placeholders)",
            selectionArgs = ids.map { it.toString() }.toTypedArray()
        )
    }

    private fun queryPhotos(
        resolver: ContentResolver,
        selection: String?,
        selectionArgs: Array<String>?
    ): List<Photo> {
        val photos = mutableListOf<Photo>()
        val collection = imagesCollection()
        resolver.query(collection, PROJECTION, selection, selectionArgs, SORT_ORDER)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                photos.add(
                    Photo(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = cursor.getString(nameCol) ?: "Foto",
                        dateAddedMillis = cursor.getLong(dateCol) * 1000L,
                        bucketId = cursor.getString(bucketIdCol) ?: "",
                        bucketName = cursor.getString(bucketNameCol) ?: "Lainnya"
                    )
                )
            }
        }
        return photos
    }

    /**
     * Susun daftar album dari daftar foto: album "Favorit" selalu di urutan
     * pertama, sisanya alfabetis.
     */
    fun buildAlbums(photos: List<Photo>, favoriteIds: Set<Long>): List<Album> {
        val albums = photos
            .groupBy { it.bucketId to it.bucketName }
            .map { (key, list) ->
                Album(
                    bucketId = key.first,
                    name = key.second,
                    count = list.size,
                    coverUri = list.firstOrNull()?.uri
                )
            }
            .sortedBy { it.name.lowercase() }
            .toMutableList()

        val favoritePhotos = photos.filter { favoriteIds.contains(it.id) }
        albums.add(
            0,
            Album(
                bucketId = FAVORITES_BUCKET_ID,
                name = "Favorit",
                count = favoritePhotos.size,
                coverUri = favoritePhotos.firstOrNull()?.uri
            )
        )
        return albums
    }
}
