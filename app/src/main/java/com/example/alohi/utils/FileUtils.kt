package com.example.alohi.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val fileName = getFileName(context, uri) ?: "temp_file"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            return file
        }
        return null
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
