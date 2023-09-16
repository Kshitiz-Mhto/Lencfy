package com.enhance.lencfy.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.enhance.lencfy.R
import com.enhance.lencfy.databinding.ActivityMainBinding
import com.enhance.lencfy.util.Constants
import com.ramotion.circlemenu.CircleMenuView
import ly.img.android.pesdk.PhotoEditorSettingsList
import ly.img.android.pesdk.assets.filter.basic.FilterPackBasic
import ly.img.android.pesdk.assets.font.basic.FontPackBasic
import ly.img.android.pesdk.assets.frame.basic.FramePackBasic
import ly.img.android.pesdk.assets.overlay.basic.OverlayPackBasic
import ly.img.android.pesdk.assets.sticker.emoticons.StickerPackEmoticons
import ly.img.android.pesdk.assets.sticker.shapes.StickerPackShapes
import ly.img.android.pesdk.backend.model.EditorSDKResult
import ly.img.android.pesdk.backend.model.constant.OutputMode
import ly.img.android.pesdk.backend.model.state.LoadSettings
import ly.img.android.pesdk.backend.model.state.PhotoEditorSaveSettings
import ly.img.android.pesdk.ui.activity.PhotoEditorBuilder
import ly.img.android.pesdk.ui.model.state.UiConfigFilter
import ly.img.android.pesdk.ui.model.state.UiConfigFrame
import ly.img.android.pesdk.ui.model.state.UiConfigOverlay
import ly.img.android.pesdk.ui.model.state.UiConfigSticker
import ly.img.android.pesdk.ui.model.state.UiConfigText
import ly.img.android.pesdk.ui.panels.item.PersonalStickerAddItem
import ly.img.android.serializer._3.IMGLYFileWriter
import java.io.File
import java.io.IOException


class LencfyMainActivity() : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val PESDK_RESULT = 1
        const val GALLERY_RESULT = 2
        private const val CAMERA_REQUEST_CODE = 0x69
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setTheme(R.style.Base_Theme_Lencfy)
        setContentView(binding.root)

        val circularMenu = binding.circularMenu
        circularMenu.eventListener = object : CircleMenuView.EventListener() {

            override fun onButtonClickAnimationStart(view: CircleMenuView, index: Int) {
                Log.d("D", "onButtonClickAnimationStart| index: $index")
                super.onButtonClickAnimationStart(view, index)
                when(index){
                    0 -> openSystemGalleryToSelectAnImage()
                    1 -> startSystemCameraToCaptureAnImage()
                    else -> {}
                }
            }

        }

    }

    fun openSystemGalleryToSelectAnImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivityForResult(intent, GALLERY_RESULT)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No Gallery APP installed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun startSystemCameraToCaptureAnImage(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "demo_directory")
            directory.mkdirs() // Create the directory if it doesn't exist

            val file = File(directory, "demo.jpg")
            val photoURI: Uri = FileProvider.getUriForFile(this, Constants.AUTHORITY, file)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
        } catch (ex: ActivityNotFoundException) {
            // Handle the case where no camera app is installed
            Toast.makeText(
                this,
                "No Camera APP installed or Supported",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    fun openEditor(inputImage: Uri?) {
        val settingsList = createPesdkSettingsList()

        settingsList.configure<LoadSettings> {
            it.source = inputImage
        }

        PhotoEditorBuilder(this)
            .setSettingsList(settingsList)
            .startActivityForResult(this, PESDK_RESULT)

        settingsList.release()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        intent ?: return
        if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {
            // Open Editor with some uri in this case with an image selected from the system gallery.
            val selectedImage = intent.data
            if (selectedImage != null) {
                openEditor(selectedImage)
            } else if (requestCode == PESDK_RESULT) {
                // Editor has saved an Image.
                val data = EditorSDKResult(intent)

                Log.i("PESDK", "Source image is located here ${data.sourceUri}")
                Log.i("PESDK", "Result image is located here ${data.resultUri}")

                // TODO: Do something with the result image

                // OPTIONAL: read the latest state to save it as a serialisation
                val lastState = data.settingsList
                try {
                    IMGLYFileWriter(lastState).writeJson(
                        File(
                            Environment.getExternalStorageDirectory(),
                            "serialisationReadyToReadWithPESDKFileReader.json"
                        )
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                lastState.release()

            }
        }
        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST_CODE) {
            Log.i("result", "testing101")
            val imageUri = Uri.parse("file://${Constants.TEMP_IMAGEPATH}")
            openEditor(imageUri)
            // Open Editor with some uri in this case with an image selected from the system gallery.
            val selectedImage = intent.data
            if (selectedImage != null) {
                Log.i("result1", "testing1011111111")
                openEditor(selectedImage)
            } else if (requestCode == PESDK_RESULT) {
                // Editor has saved an Image.
                val data = EditorSDKResult(intent)

                Log.i("PESDK", "Source image is located here ${data.sourceUri}")
                Log.i("PESDK", "Result image is located here ${data.resultUri}")

                // TODO: Do something with the result image

                // OPTIONAL: read the latest state to save it as a serialisation
                val lastState = data.settingsList
                try {
                    IMGLYFileWriter(lastState).writeJson(
                        File(
                            Environment.getExternalStorageDirectory(),
                            "serialisationReadyToReadWithPESDKFileReader.json"
                        )
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                lastState.release()

            }
        }

    }
    private fun createPesdkSettingsList() =
        PhotoEditorSettingsList(true)
            .configure<UiConfigFilter> {
                it.setFilterList(FilterPackBasic.getFilterPack())
            }
            .configure<UiConfigText> {
                it.setFontList(FontPackBasic.getFontPack())
            }
            .configure<UiConfigFrame> {
                it.setFrameList(FramePackBasic.getFramePack())
            }
            .configure<UiConfigOverlay> {
                it.setOverlayList(OverlayPackBasic.getOverlayPack())
            }
            .configure<UiConfigSticker> {
                it.setStickerLists(
                    PersonalStickerAddItem(),
                    StickerPackEmoticons.getStickerCategory(),
                    StickerPackShapes.getStickerCategory()
                )
            }
            .configure<PhotoEditorSaveSettings> {
                // Set custom editor image export settings
                it.setOutputToGallery(Environment.DIRECTORY_DCIM)
                it.outputMode = OutputMode.EXPORT_IF_NECESSARY
            }
}

