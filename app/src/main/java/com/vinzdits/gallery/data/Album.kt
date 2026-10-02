package com.vinzdits.gallery.data

import android.net.Uri

/** Satu album (grup foto per folder/bucket), atau album spesial "Favorit". */
data class Album(
    val bucketId: String,
    val name: String,
    val count: Int,
    val coverUri: Uri?
)
