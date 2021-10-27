package com.example.mlkittest

/*
* MLKitTest
* 拝借元：https://ichi.pro/riarutaimu-no-tekisutoin-ime-ji-ninshiki-android-apuri-o-sakuseisuru-258139656422671
* なんか普通の体温計は読めるっぽい
* */


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Color.YELLOW
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mlkittest.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private lateinit var binding: ActivityMainBinding

    private companion object {
        val TAG = "test" // Log見る用のタグ

        // 必要なパーミッション群
        const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            // 微熱以上あるか聞くトースト
                askThermo()
                startCamera()
        } else {
                requestPermissions()
                askThermo()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    //　全てのパーミッションが許可されてるか
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // パーミッション許可処理
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // パーミッションが許可済みかチェック
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera() // 許可されてたらカメラ処理へ
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    private fun askThermo(){
        AlertDialog.Builder(this).apply {
            setTitle("使用上の注意")
            setMessage(R.string.dialogMsg)
            setPositiveButton("OK",{_,_,->})
            show()
        }
    }

    // 画像解析を行うオブジェクトを生成
    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder()
                // アスペクト比の指定
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    //　引数にonTextFoundを指定→解析した文字がonTextFoundへ渡される？
                    TextReaderAnalyzer(::onTextFound)
                )
            }
    }

    // TextReaderAnalyzerからの認識結果をログとtextViewに反映
    private fun onTextFound(foundText: String)  {
        Log.d(TAG, "We got new text: $foundText")
        var thermo = 0f

        val regex = Regex("""\d{3}+""")
        var isMatched = regex.matches(foundText)

        if(isMatched){
            thermo = foundText.toFloat()/10
        }

        var snackbar = Snackbar
            .make(binding.root, "スキャンしました" ,Snackbar.LENGTH_SHORT)
            .setTextColor(Color.YELLOW)

        if(thermo < 42.0f){
            binding.textView.text = thermo.toString()
        } else {
            binding.textView.text = null
        }

        // ここからテスト
        binding.button.setOnClickListener {

            binding.editTextText.setText(thermo.toString())
            snackbar.show()
        }
    }

    // カメラをアクティビティのライフサイクルにバインド
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(binding.cameraPreviewView.surfaceProvider) }
                cameraProviderFuture.get().bind(preview, imageAnalyzer)
            },
            ContextCompat.getMainExecutor(this)
        )
    }
    private fun ProcessCameraProvider.bind(preview: Preview, imageAnalyzer: ImageAnalysis) = try {
        unbindAll()
        bindToLifecycle(this@MainActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
    } catch (ise: IllegalStateException) {
        // Thrown if binding is not done from the main thread
        Log.e(TAG, "Binding failed", ise)
    }
}