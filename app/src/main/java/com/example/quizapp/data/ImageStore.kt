package com.example.quizapp.data

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/** Stores question images in internal storage; references by stored path. */
class ImageStore(private val context: Context) {

    private val dir: File by lazy {
        File(context.filesDir, "question_images").apply { if (!exists()) mkdirs() }
    }

    /** Save a stream to storage, returns the absolute path of the stored file. */
    fun saveStream(input: InputStream, ext: String = ".png"): String {
        val file = File(dir, UUID.randomUUID().toString() + ext)
        file.outputStream().use { input.copyTo(it) }
        return file.absolutePath
    }

    /** Save raw bytes; returns stored path. */
    fun saveBytes(bytes: ByteArray, ext: String = ".png"): String {
        val file = File(dir, UUID.randomUUID().toString() + ext)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    /** Decode a base64 string into a stored image file; returns path or null. */
    fun saveBase64(b64: String?): String? {
        if (b64.isNullOrBlank()) return null
        return try {
            val data = Base64.decode(b64, Base64.NO_WRAP)
            saveBytes(data)
        } catch (_: Exception) { null }
    }

    fun fileOf(path: String?): File? = path?.let { File(it).takeIf { f -> f.exists() } }

    fun toBase64(path: String?): String? {
        val f = fileOf(path) ?: return null
        return try { Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) } catch (_: Exception) { null }
    }

    fun exists(path: String?): Boolean = fileOf(path) != null
}
