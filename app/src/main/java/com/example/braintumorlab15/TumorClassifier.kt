package com.example.braintumorlab15

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TumorClassifier(assets: AssetManager) : AutoCloseable {
    companion object {
        const val IMAGE_SIZE = 260
        val CLASSES = listOf("glioma", "meningioma", "notumor", "pituitary")
    }

    private val interpreter: Interpreter

    init {
        // Flex (SELECT_TF_OPS) auto-delegates when the matching AAR is bundled.
        // Reads the model as a direct ByteBuffer to support all asset packagings.
        val bytes = assets.open("brain_tumor_model_fp16.tflite").use { it.readBytes() }
        val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        model.put(bytes)
        model.rewind()
        interpreter = Interpreter(model, Interpreter.Options().setNumThreads(2))
        val inputShape = interpreter.getInputTensor(0).shape()
        require(inputShape.contentEquals(intArrayOf(1, 260, 260, 3))) {
            "Unexpected input shape: ${inputShape.contentToString()}"
        }
    }

    fun classify(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        scaled.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        // Model config: NHWC RGB float32, pixel range 0..255, no /255 scaling.
        val input = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())
        for (pixel in pixels) {
            input.putFloat(((pixel shr 16) and 0xFF).toFloat())
            input.putFloat(((pixel shr 8) and 0xFF).toFloat())
            input.putFloat((pixel and 0xFF).toFloat())
        }
        input.rewind()
        val output = Array(1) { FloatArray(4) }
        interpreter.run(input, output)
        if (scaled !== bitmap) scaled.recycle()
        return output[0]
    }

    override fun close() {
        interpreter.close()
    }
}
