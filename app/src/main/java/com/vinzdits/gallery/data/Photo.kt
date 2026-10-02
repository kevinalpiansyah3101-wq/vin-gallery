package com.vinzdits.gallery.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Satu foto dari MediaStore. */
@Parcelize
data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAddedMillis: Long,
    val bucketId: String,
    val bucketName: String
) : Parcelable
