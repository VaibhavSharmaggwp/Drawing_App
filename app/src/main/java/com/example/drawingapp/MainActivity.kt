    package com.example.drawingapp
    import android.Manifest
    import android.app.AlertDialog
    import android.app.Dialog
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.graphics.Bitmap
    import android.graphics.Canvas
    import android.graphics.Color
    import android.media.MediaScannerConnection
    import android.os.Build
    import androidx.appcompat.app.AppCompatActivity
    import android.os.Bundle
    import android.provider.MediaStore
    import android.view.View
    import android.widget.Button
    import android.widget.FrameLayout
    import android.widget.ImageButton
    import android.widget.ImageView
    import android.widget.LinearLayout
    import android.widget.Toast
    import androidx.activity.result.ActivityResultLauncher
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.annotation.RequiresApi
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.core.view.get
    import androidx.lifecycle.lifecycleScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import java.io.ByteArrayOutputStream
    import java.io.File
    import java.io.FileOutputStream


    class MainActivity : AppCompatActivity() {


        private var drawingView: DrawingView? = null
        private var mImageButtonCurretnPaint: ImageButton? = null

        var customProgressDialog: Dialog?= null

        private val openGalleryLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
                if(result.resultCode == RESULT_OK && result.data!=null){
                    val imageBackGround: ImageView = findViewById(R.id.iv_background)

                    imageBackGround.setImageURI(result.data?.data)
                }
            }

        private val requestPermission:  ActivityResultLauncher<Array<String>> =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
                permissions.entries.forEach{
                    val permissionName = it.key
                    val isGranted = it.value
                    if(isGranted){
                        Toast.makeText(this, "Permission Granted. You can read the storage files",Toast.LENGTH_LONG).show()

                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }else{
                        if(permissionName==Manifest.permission.READ_MEDIA_IMAGES ||
                            permissionName == Manifest.permission.READ_MEDIA_VIDEO||
                            permissionName == Manifest.permission.READ_MEDIA_AUDIO)
                            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                    }
                }
            }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            drawingView = findViewById(R.id.drawing_view)
            drawingView?.setSizeForBrush(20.toFloat())

            val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
            mImageButtonCurretnPaint = linearLayoutPaintColors[2] as ImageButton
            mImageButtonCurretnPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            val ib_brush: ImageButton = findViewById(R.id.ib_brush)
            ib_brush.setOnClickListener {
                showBrushSizeChooserDialog()
            }

            val ibundo: ImageButton = findViewById(R.id.ib_undo)
            ibundo.setOnClickListener{
                drawingView?.onClickUndo()
            }
            val redobtn: ImageButton = findViewById(R.id.ib_redo)
            redobtn.setOnClickListener{
                drawingView?.onClickRedo()
            }

            val ibSave: ImageButton = findViewById(R.id.ib_save)
            ibSave.setOnClickListener{
                if(isReadStorageAllowed()){
                    showProgressDialog()
                    lifecycleScope.launch {
                        val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                        saveBitmapFile(getBitmapFromView(flDrawingView))
                    }
                }
            }

            val ib_eraser: ImageButton = findViewById(R.id.ib_eraser)
            ib_eraser.setOnClickListener {
                drawingView?.setColor("#FFFFFF")
                showEraserSizeChooserDialog()

                // if u want snak bar [Snackbar.make(view, "You clicked button", Snackbar.LENGTH_LONG).show()]
            }

            val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
            ibGallery.setOnClickListener{
                requestStorgePermission()
            }


        }

        private fun showBrushSizeChooserDialog(){
            val brushDialog = Dialog(this)
            brushDialog.setContentView(R.layout.dialog_brush_size)
            brushDialog.setTitle("Brush Size: ")
            val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
            smallBtn.setOnClickListener{
                drawingView?.setSizeForBrush(10.toFloat())
                brushDialog.dismiss()
            }

            val mediumlBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
            mediumlBtn.setOnClickListener{
                drawingView?.setSizeForBrush(20.toFloat())
                brushDialog.dismiss()
            }

            val LargeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
            LargeBtn.setOnClickListener{
                drawingView?.setSizeForBrush(30.toFloat())
                brushDialog.dismiss()
            }
            brushDialog.show()
        }

        private fun showEraserSizeChooserDialog(){
            val eraserDialog = Dialog(this)
            eraserDialog.setContentView(R.layout.dialog_brush_size)
            eraserDialog.setTitle("Eraser Size :")


            val smallBtn: ImageButton = eraserDialog.findViewById(R.id.ib_small_brush)
            smallBtn.setOnClickListener {
                drawingView?.setSizeForBrush(10.toFloat())
                drawingView?.setColor("#FFFFFF")
                eraserDialog.dismiss()
            }

            val mediumBtn: ImageButton = eraserDialog.findViewById(R.id.ib_medium_brush)
            mediumBtn.setOnClickListener {
                drawingView?.setSizeForBrush(25.toFloat())
                drawingView?.setColor("#FFFFFF")
                eraserDialog.dismiss()
            }

            val LgeBtn: ImageButton = eraserDialog.findViewById(R.id.ib_large_brush)
            LgeBtn.setOnClickListener{
                drawingView?.setSizeForBrush(35.toFloat())
                drawingView?.setColor("#FFFFFF")
                eraserDialog.dismiss()
            }
            eraserDialog.show()

        }

        fun paintClicked(view: View){
            if(view!= mImageButtonCurretnPaint){
                val imageButton = view as ImageButton
                val colorTag = imageButton.tag.toString()
                drawingView?.setColor(colorTag)
                imageButton.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
                )
                mImageButtonCurretnPaint?.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                )
                mImageButtonCurretnPaint = view
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun isReadStorageAllowed(): Boolean{
            val readImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
            val readVideoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
            val readAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

            val resultImage = ContextCompat.checkSelfPermission(this, readImagePermission)
            val resultVideo = ContextCompat.checkSelfPermission(this, readVideoPermission)
            val resultAudio = ContextCompat.checkSelfPermission(this, readAudioPermission)

            return resultImage == PackageManager.PERMISSION_GRANTED &&
                    resultVideo == PackageManager.PERMISSION_GRANTED &&
                    resultAudio == PackageManager.PERMISSION_GRANTED
        }
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun requestStorgePermission(){
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.READ_MEDIA_IMAGES) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.READ_MEDIA_VIDEO) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.READ_MEDIA_AUDIO)
            ) {
                showRationalDialog("Kids Drawing App", "Kids drawing app needs to Access your media files")
            } else {
                requestPermission.launch(arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ))
            }
        }

        private fun showRationalDialog(tittle: String, message: String){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(tittle)
                .setMessage(message)
                .setPositiveButton("Cancel"){dialog, _ ->
                    dialog.dismiss()

                }
            builder.create().show()
        }

        private fun getBitmapFromView(view: View): Bitmap{
            val returnedBitmap  = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(returnedBitmap)
            val bgDrawable = view.background
            if(bgDrawable!= null){
                bgDrawable.draw(canvas)
            }else{
                canvas.drawColor(Color.WHITE)
            }
            view.draw(canvas)
            return returnedBitmap
        }

        private  suspend fun saveBitmapFile(mBitmap: Bitmap?): String{
            var result = ""
            withContext(Dispatchers.IO){
                if(mBitmap!=null){
                    try {
                        val bytes = ByteArrayOutputStream()
                        mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                        val f = File(externalCacheDir?.absoluteFile.toString()
                        + File.separator + "KidsDrawingApp" + System.currentTimeMillis() / 1000 + ".png"
                        )
                        val fo = FileOutputStream(f)
                        fo.write(bytes.toByteArray())
                        fo.close()

                        result = f.absolutePath
                        runOnUiThread{
                            cancelProgressDialog()
                            if(result.isNotEmpty()){
                                Toast.makeText(this@MainActivity, "File Saved successfully :$result", Toast.LENGTH_LONG).show()
                                shareImage(result)
                            }

                            else{
                                Toast.makeText(this@MainActivity, "Something went wrong ", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    catch (e: Exception){
                        result = " "
                        e.printStackTrace()
                    }
                }
            }
            return result
        }

        private fun showProgressDialog(){
            customProgressDialog = Dialog(this@MainActivity)

            customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

            // start the dialog and display in the screen
            customProgressDialog?.show()
        }
        private fun cancelProgressDialog(){
            if(customProgressDialog != null){
                customProgressDialog?.dismiss()
                customProgressDialog = null
            }
        }

        private fun shareImage(result: String){
            MediaScannerConnection.scanFile(this, arrayOf(result), null){
                path, uri->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent, "Share"))
            }
        }


    }