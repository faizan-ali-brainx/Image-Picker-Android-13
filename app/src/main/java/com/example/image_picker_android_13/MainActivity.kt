package com.example.image_picker_android_13

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.image_picker_android_13.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.*

//    Tasks to perform normally on the selected Media
//    1 - Get extension of selected item
//    2 - Convert selected item into File
//    3 - Convert selected item into Bitmap and Base64
//    4 - Set image in ImageView


class MainActivity : AppCompatActivity() {

    private lateinit var mViewBinding: ActivityMainBinding
    private var isPickingMedia = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)

        mViewBinding.btnSelectMedia.setOnClickListener {
            isPickingMedia = true
            checkAndroidVersion()
        }
        mViewBinding.btnDocuments.setOnClickListener {
            isPickingMedia = false
            if (isPermissionGranted()) {
                pickDocuments()
            } else {
                requestForPermissions()
            }
        }
    }

    private fun pickDocuments() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/*"
            pickDocumentsLauncher.launch(this)
        }
    }

    private val pickDocumentsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let {
            Log.d("base64", "Base64 String: " + convertUriToBase64(it).toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            pickMediaInAndroid11AndAbove.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        } else{
            if (isPermissionGranted()) {
                pickMediaInAndroid10AndLower()
            } else {
                requestForPermissions()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (isPickingMedia) pickMediaInAndroid10AndLower() else pickDocuments()
            }
        }

    private fun requestForPermissions() {
        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isPermissionGranted(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private val pickMediaInAndroid10AndLowerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let {
            setSelectedImage(it)
            Log.d("base64", "Base64 String: " + convertUriToBase64(it).toString())
        }
    }

    private fun pickMediaInAndroid10AndLower() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            pickMediaInAndroid10AndLowerLauncher.launch(this)
        }
    }

    private val pickMediaInAndroid11AndAbove =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                setSelectedImage(uri)
                Log.d("base64", "Base64 String: " + convertUriToBase64(uri).toString())
            }
        }

    // region task - 1
    private fun Uri.getFileExtension(context: Context): String? {
        return try {
            Uri.fromFile(File(this.path))?.lastPathSegment?.split(".")?.get(1)
        } catch (ex: Exception) {
            context.contentResolver.getType(this)?.substringAfter("/")
        }
    }
    // end region

    // region task - 2
    private fun Context.convertUriToFile(uri: Uri) = with(contentResolver) {
        val data = readUriBytes(uri) ?: return@with null
        val extension = uri.getFileExtension(this@convertUriToFile)
        File(
            cacheDir.path,
            "${UUID.randomUUID()}.$extension"
        ).also { video -> video.writeBytes(data) }
    }

    private fun ContentResolver.readUriBytes(uri: Uri) = openInputStream(uri)
        ?.buffered()?.use { it.readBytes() }
    // end region

    // region task - 3
    private fun convertUriToBase64(fileUri: Uri?): String? {
        try {
            fileUri?.let {
                val fileMimeType = getMimeType(it)
                return "data:${fileMimeType.first};base64," + it.convertUriToBase64(this)
            }
        } catch (ex: Exception) {
            print(ex)
        }
        return null
    }

    private fun getMimeType(uri: Uri): Pair<String?, String?> {
        var mimeType: String? = null
        var extension: String? = null
        mimeType = if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            contentResolver.getType(uri)
        } else {
            extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                extension.lowercase(Locale.ROOT)
            )
        }
        return Pair(mimeType, extension)
    }

    private fun Uri.convertUriToBase64(context: Context?): String? {
        try {
            val fileInputStream = context?.contentResolver?.openInputStream(this)
            val bytes = fileInputStream?.let { getBytes(it) }
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (ex: OutOfMemoryError) {
            print(ex)
        } catch (e: Exception) {
            print(e)
        }
        return null
    }

    private fun getBytes(inputStream: InputStream): ByteArray? {
        try {
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var len = 0
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            return byteBuffer.toByteArray()
        } catch (e: Exception) {
            print(e)
        }
        return null
    }
    // end region

    // region task - 4
    private fun setSelectedImage(uri: Uri) {
        mViewBinding.IvSelectedImage.setImageURI(uri)
    }
    // end region

}