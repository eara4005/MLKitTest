package com.example.mlkittest
/* *
* 画像解析クラス
* */

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import java.io.IOException

// textFoundListenerは、Stringを引数にとり、Unit型の戻り値を返す。けどこれはJavaとかでいうvoidだから何も返さない。
// onTextFind という関数をリテラルで参照してるので、解析した値がonTextFoundの引数として実行される。という認識で合ってるはず？
// https://qiita.com/k5n/items/964d765767a65cc3de5b#%E9%AB%98%E9%9A%8E%E9%96%A2%E6%95%B0

class TextReaderAnalyzer (private val textFoundListener: (String) -> Unit ) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.image?.let { process(it, imageProxy) }
    }

    // 画像に文字が含まれているか確認
    private fun process(image: Image, imageProxy: ImageProxy) {
        try {
            // fromMediaImage (Image image, int rotationDegrees)
            readTextFromImage(InputImage.fromMediaImage(image,90), imageProxy)
        } catch (e: IOException) {
            Log.d(TAG, "Failed to load the image")
            e.printStackTrace()
        }
    }

    // 画像内の文字を認識
    private fun readTextFromImage(image: InputImage, imageProxy: ImageProxy) {
        TextRecognition.getClient()
            .process(image)
            .addOnSuccessListener { visionText ->
                processTextFromImage(visionText)
                imageProxy.close()
            }
            .addOnFailureListener { error ->
                Log.d(TAG, "Failed to process the image")
                error.printStackTrace()
                imageProxy.close()
            }
    }

    // 認識した文字をメインへ返す
    private fun processTextFromImage(visionText: Text) {
        for (block in visionText.textBlocks) {
            // You can access whole block of text using block.text
            for (line in block.lines) {
                // You can access whole line of text using line.text
                for (element in line.elements) {
                    // 突貫工事　正規表現で数字以外を弾く
                        // https://www.bedroomcomputing.com/2020/09/2020-0905-kotlin-regex/#%E6%AD%A3%E8%A6%8F%E8%A1%A8%E7%8F%BE%E3%82%AF%E3%83%A9%E3%82%B9%E3%81%AE%E5%AE%A3%E8%A8%80

                    val regex = Regex("""\d{2,3}+""")
                    var isMatched = regex.matches(element.text)
                    if(isMatched){
                        textFoundListener(element.text)
                    }

                }
            }
        }
    }
}
