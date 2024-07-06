package com.emm.justchill.hh.domain

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.emm.justchill.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

object FilesManager {

    fun createJsonFile(
        context: Context,
        filename: String,
        jsonContent: String
    ): Boolean {
        return try {

            val applicationId = BuildConfig.FLAVOR
            val displayName = "$filename-$applicationId.json"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val target: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val contentResolver: ContentResolver = context.contentResolver

            val existingFileUri = findFileUri(context, displayName, target)
            existingFileUri?.let {
                contentResolver.delete(it, null, null)
            }

            val uriOfFileCreated: Uri = contentResolver.insert(target, contentValues) ?: return false

            contentResolver.openOutputStream(uriOfFileCreated).use {
                it?.write(jsonContent.toByteArray())
            }

            true
        } catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

    private fun findFileUri(context: Context, displayName: String, target: Uri): Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(displayName)

        context.contentResolver.query(target, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return ContentUris.withAppendedId(target, id)
            }
        }
        return null
    }

    fun readJsonFile(context: Context, filename: String): String? = try {
        val applicationId = BuildConfig.FLAVOR

        val projections = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE
        )

        val selectionArgs = arrayOf("application/json", "$filename-$applicationId.json")

        val selection =
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"

        val target: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        context.contentResolver.query(
            target,
            projections,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                val contentUri = ContentUris.withAppendedId(target, id)
                context.contentResolver.openInputStream(contentUri)?.use { inputStream ->
                    inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                }
            } else {
                null
            }
        }
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        null
    }
}