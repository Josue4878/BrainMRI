package com.example.braintumorlab15

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var classifier: TumorClassifier? = null
    private var currentImage: Bitmap? = null
    private lateinit var status: TextView
    private lateinit var prediction: TextView
    private lateinit var details: TextView
    private lateinit var preview: ImageView
    private lateinit var probabilityBox: LinearLayout
    private lateinit var testResults: TextView
    private lateinit var classifyButton: Button

    private val samples = listOf(
        "glioma" to "four_test_images/glioma_test.jpg",
        "meningioma" to "four_test_images/meningioma_test.jpg",
        "notumor" to "four_test_images/notumor_test.jpg",
        "pituitary" to "four_test_images/pituitary_test.jpg"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        prediction = findViewById(R.id.prediction)
        details = findViewById(R.id.details)
        preview = findViewById(R.id.preview)
        probabilityBox = findViewById(R.id.probabilities)
        testResults = findViewById(R.id.testResults)
        classifyButton = findViewById(R.id.classify)
        classifyButton.isEnabled = false
        findViewById<Button>(R.id.pickImage).setOnClickListener { selectImage() }
        classifyButton.setOnClickListener { analyzeSelected() }
        findViewById<Button>(R.id.glioma).setOnClickListener { selectSample("glioma") }
        findViewById<Button>(R.id.meningioma).setOnClickListener { selectSample("meningioma") }
        findViewById<Button>(R.id.notumor).setOnClickListener { selectSample("notumor") }
        findViewById<Button>(R.id.pituitary).setOnClickListener { selectSample("pituitary") }
        findViewById<Button>(R.id.allTests).setOnClickListener { runAllSamples() }

        worker.execute {
            try {
                classifier = TumorClassifier(assets)
                runOnUiThread {
                    status.text = "Model loaded • TFLite + Flex • CPU"
                    classifyButton.isEnabled = currentImage != null
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Model load failed"
                    details.text = "${e.javaClass.simpleName}: ${e.message}"
                    prediction.text = "Runtime error"
                }
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, 42)
    }

    @Deprecated("Use Activity Result API in production apps")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            worker.execute {
                try {
                    val bitmap = decodeUri(uri)
                    runOnUiThread { showImage(bitmap, "Gallery image") }
                } catch (e: Exception) {
                    runOnUiThread { details.text = "Failed to open image: ${e.message}" }
                }
            }
        }
    }

    private fun decodeUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(2)
            }
        } else {
            contentResolver.openInputStream(uri).use { stream ->
                requireNotNull(BitmapFactory.decodeStream(stream)) { "Unsupported image format" }
            }
        }
    }

    private fun loadSample(path: String): Bitmap =
        assets.open(path).use { input ->
            requireNotNull(BitmapFactory.decodeStream(input)) { "Cannot decode $path" }
        }

    private fun selectSample(label: String) {
        val path = samples.first { it.first == label }.second
        worker.execute {
            try {
                val bitmap = loadSample(path)
                runOnUiThread { showImage(bitmap, "Sample: $label") }
            } catch (e: Exception) {
                runOnUiThread { details.text = "Sample error: ${e.message}" }
            }
        }
    }

    private fun showImage(bitmap: Bitmap, source: String) {
        currentImage = bitmap
        preview.setImageBitmap(bitmap)
        prediction.text = "Ready to classify"
        details.text = source
        testResults.text = ""
        probabilityBox.removeAllViews()
        classifyButton.isEnabled = classifier != null
    }

    private fun analyzeSelected() {
        val bitmap = currentImage ?: return
        if (classifier == null) {
            details.text = "Model is still loading"
            return
        }
        classifyButton.isEnabled = false
        status.text = "Running inference…"
        worker.execute {
            try {
                val t0 = SystemClock.elapsedRealtime()
                val scores = requireNotNull(classifier).classify(bitmap)
                val latency = SystemClock.elapsedRealtime() - t0
                runOnUiThread {
                    displayResult(scores, latency)
                    classifyButton.isEnabled = true
                    status.text = "Inference completed on device"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Inference failed"
                    details.text = "${e.javaClass.simpleName}: ${e.message}"
                    classifyButton.isEnabled = true
                }
            }
        }
    }

    private fun displayResult(scores: FloatArray, elapsed: Long) {
        val best = scores.indices.maxByOrNull { scores[it] } ?: 0
        prediction.text = TumorClassifier.CLASSES[best].replaceFirstChar { it.uppercase() }
        details.text = String.format(Locale.US, "Model score: %.1f%% • %d ms", scores[best] * 100, elapsed)
        probabilityBox.removeAllViews()
        TumorClassifier.CLASSES.forEachIndexed { i, label ->
            val tv = TextView(this).apply {
                text = String.format(Locale.US, "%s   %.1f%%", label, scores[i] * 100)
                setTextColor(getColor(R.color.white))
                textSize = 14f
                setPadding(0, 12, 0, 3)
            }
            probabilityBox.addView(tv)
            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000
                progress = (scores[i] * 1000).toInt().coerceIn(0, 1000)
                progressTintList = getColorStateList(R.color.accent)
            }
            probabilityBox.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 18))
        }
    }

    private fun runAllSamples() {
        if (classifier == null) {
            testResults.text = "Model is not ready"
            return
        }
        status.text = "Running four test images…"
        testResults.text = "Processing…"
        worker.execute {
            try {
                val lines = mutableListOf<String>()
                var correct = 0
                for ((label, path) in samples) {
                    val bitmap = loadSample(path)
                    val scores = requireNotNull(classifier).classify(bitmap)
                    bitmap.recycle()
                    val best = scores.indices.maxByOrNull { scores[it] } ?: 0
                    val predicted = TumorClassifier.CLASSES[best]
                    if (predicted == label) correct++
                    lines += String.format(Locale.US, "%s → %s (%.1f%%)", label, predicted, scores[best] * 100)
                }
                runOnUiThread {
                    status.text = "Four-image test completed"
                    testResults.text = "Four samples: $correct/4 correct\n" + lines.joinToString("\n")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Four-image test failed"
                    testResults.text = "${e.javaClass.simpleName}: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        worker.execute { classifier?.close() }
        worker.shutdown()
        super.onDestroy()
    }
}
