package com.Z.Pikon

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast


import com.Z.pikonlib.EditImageActivity
import com.Z.pikonlib.picchooser.SelectPictureActivity
import com.Z.pikonlib.utils.BitmapUtils

import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var context: MainActivity? = null
    private var imgView: ImageView? = null
    private var openAblum: View? = null
    private val editImage: View? = null//
    private var mainBitmap: Bitmap? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0//
    private var path: String? = null


    private var mTakenPhoto: View? = null
    private var photoURI: Uri? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        context = this
        val metrics = resources.displayMetrics
        imageWidth = metrics.widthPixels
        imageHeight = metrics.heightPixels

        imgView = findViewById(R.id.img) as ImageView
        openAblum = findViewById(R.id.select_ablum)
        //editImage = findViewById(R.id.edit_image);
        openAblum!!.setOnClickListener(this)
        //editImage.setOnClickListener(this);

        mTakenPhoto = findViewById(R.id.take_photo)
        mTakenPhoto!!.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.take_photo -> takePhotoClick()
        //case R.id.edit_image:
        //  editImageClick();
        // break;
            R.id.select_ablum -> selectFromAblum()
        }//end switch
    }

    /**
     * 拍摄照片
     */
    protected fun takePhotoClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestTakePhotoPermissions()
        } else {
            doTakePhoto()
        }//end if
    }

    /**
     * 请求拍照权限
     */
    private fun requestTakePhotoPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_PERMISSON_CAMERA)
            return
        }
        doTakePhoto()
    }

    /**
     * 拍摄照片
     */
    private fun doTakePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile = FileUtils.genEditFile()
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = Uri.fromFile(photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, TAKE_PHOTO_CODE)
            }

            //startActivityForResult(takePictureIntent, TAKE_PHOTO_CODE);
        }
    }

    /**
     * 编辑选择的图片

     * @author panyi
     */
    private fun editImageClick() {
        val outputFile = FileUtils.genEditFile()


        EditImageActivity.start(this, path, outputFile.absolutePath, ACTION_REQUEST_EDITIMAGE)
    }


    private fun selectFromAblum() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            openAblumWithPermissionsCheck()
        } else {
            openAblum()
        }//end if
    }

    private fun openAblum() {
        this@MainActivity.startActivityForResult(Intent(
                this@MainActivity, SelectPictureActivity::class.java),
                SELECT_GALLERY_IMAGE_CODE)
    }

    private fun openAblumWithPermissionsCheck() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSON_SORAGE)
            return
        }
        openAblum()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSON_SORAGE
                && grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openAblum()
            return
        }//end if

        if (requestCode == REQUEST_PERMISSON_CAMERA
                && grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doTakePhoto()
            return
        }//end if
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            // System.out.println("RESULT_OK");
            when (requestCode) {
                SELECT_GALLERY_IMAGE_CODE//
                -> handleSelectFromAblum(data)
                TAKE_PHOTO_CODE//拍照返回
                -> handleTakePhoto(data)
                ACTION_REQUEST_EDITIMAGE//
                -> handleEditorImage(data)
            }// end switch
        }
    }

    /**
     * 处理拍照返回

     * @param data
     */
    private fun handleTakePhoto(data: Intent) {
        if (photoURI != null) {//拍摄成功
            path = photoURI!!.path
            startLoadTask()
        }
    }

    private fun handleEditorImage(data: Intent) {
        var newFilePath = data.getStringExtra(EditImageActivity.EXTRA_OUTPUT)
        val isImageEdit = data.getBooleanExtra(EditImageActivity.IMAGE_IS_EDIT, false)

        if (isImageEdit) {
            Toast.makeText(this, getString(R.string.save_path, newFilePath), Toast.LENGTH_LONG).show()
        } else {//未编辑  还是用原来的图片
            newFilePath = data.getStringExtra(EditImageActivity.FILE_PATH)
        }
        //System.out.println("newFilePath---->" + newFilePath);
        //File file = new File(newFilePath);
        //System.out.println("newFilePath size ---->" + (file.length() / 1024)+"KB");
        Log.d("image is edit", isImageEdit.toString() + "")
        val loadTask = LoadImageTask()
        loadTask.execute(newFilePath)
    }

    private fun handleSelectFromAblum(data: Intent) {
        val filepath = data.getStringExtra("imgPath")
        path = filepath
        // System.out.println("path---->"+path);
        startLoadTask()
        editImageClick()
    }

    private fun startLoadTask() {
        val task = LoadImageTask()
        task.execute(path)
    }


    private inner class LoadImageTask : AsyncTask<String, Void, Bitmap>() {
        override fun doInBackground(vararg params: String): Bitmap {
            return BitmapUtils.getSampledBitmap(params[0], imageWidth / 4, imageHeight / 4)
        }

        override fun onCancelled() {
            super.onCancelled()
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        override fun onCancelled(result: Bitmap) {
            super.onCancelled(result)
        }

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun onPostExecute(result: Bitmap) {
            super.onPostExecute(result)
            if (mainBitmap != null) {
                mainBitmap!!.recycle()
                mainBitmap = null
                System.gc()
            }
            mainBitmap = result
            imgView!!.setImageBitmap(mainBitmap)
        }
    }// end inner class

    companion object {
        val REQUEST_PERMISSON_SORAGE = 1
        val REQUEST_PERMISSON_CAMERA = 2
        val SELECT_GALLERY_IMAGE_CODE = 7
        val TAKE_PHOTO_CODE = 8
        val ACTION_REQUEST_EDITIMAGE = 9
        val ACTION_STICKERS_IMAGE = 10
    }

}//end class
