package com.android.example.cameraxapp

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.android.example.cameraxapp.databinding.ActivityCameraBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraActivity : AppCompatActivity() {
    private lateinit var selectedDocumentText: TextView
    private lateinit var viewBinding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageView: ImageView

    private var selectedDocument: String? = null
    private var selectedCountry: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize the views
        selectedDocumentText = findViewById(R.id.selected_document_text)
        imageView = findViewById(R.id.imageView)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Retrieve the selected document type and country name from intent
        selectedDocument = intent.getStringExtra("selectedDocument")
        selectedCountry = intent.getStringExtra("selectedCountry")

        // Display the selected document type and country name
        selectedDocumentText.text = "Selected Document: $selectedDocument\nSelected Country: $selectedCountry"

        // Set up the listener for the image capture button
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Photo capture failed. Retry?", Toast.LENGTH_SHORT).show()
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    extractTextFromImage(bitmap)
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


        private fun extractTextFromImage(bitmap: Bitmap) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    Toast.makeText(baseContext, "Extracted Text: $resultText", Toast.LENGTH_LONG).show()

                    // Show the popup with extracted text
                    showExtractedTextPopup(resultText)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(baseContext, "Text extraction failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        private fun showExtractedTextPopup(extractedText: String) {
            val dialogBuilder = AlertDialog.Builder(this)
            val inflater = this.layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_extracted_text, null)
            dialogBuilder.setView(dialogView)

            val textViewExtractedText = dialogView.findViewById<TextView>(R.id.text_view_extracted_text)
            val textViewSelectedDocument = dialogView.findViewById<TextView>(R.id.text_view_selected_document)
            val textViewSelectedCountry = dialogView.findViewById<TextView>(R.id.text_view_selected_country)
            val buttonRetake = dialogView.findViewById<TextView>(R.id.button_retake)
            val buttonNext = dialogView.findViewById<TextView>(R.id.button_next)

            textViewExtractedText.text = extractedText
            textViewSelectedDocument.text = "Selected Document: $selectedDocument"
            textViewSelectedCountry.text = "Selected Country: $selectedCountry"

            val alertDialog = dialogBuilder.create()

            buttonRetake.setOnClickListener {
                alertDialog.dismiss()
                // Retake the image
                startCamera()
            }

            buttonNext.setOnClickListener {
                alertDialog.dismiss()
                // Start DisplayActivity with extracted text for processing
                val intent = Intent(this@CameraActivity, DisplayActivity::class.java)
                intent.putExtra("extractedText", extractedText)
                intent.putExtra("selectedDocument", selectedDocument)
                intent.putExtra("selectedCountry", selectedCountry)
                // Extract the name and ID number
                val name = extractNameFromText(extractedText) ?: "Name not available"
                val idNumber = extractIdNumberFromText(extractedText) ?: "ID No not available"
                intent.putExtra("name", name)
                intent.putExtra("idNumber", idNumber)
                startActivity(intent)
                finish() // Finish the current activity if you don't want to go back to it
            }

            alertDialog.show()
        }

    private fun extractNameFromText(text: String): String? {
        // Regex pattern to match a name (assumes names are capitalized)
        val namePattern = Regex("([A-Z][a-z]+(?: [A-Z][a-z]+)*)")
        val matchResult = namePattern.find(text)
        return matchResult?.value // Return the matched name or null if not found
    }

    private fun extractIdNumberFromText(text: String): String? {
        // Regex pattern to match an ID number (assuming it's a 10-digit number)
        val idPattern = Regex("\\b\\d{10}\\b")
        val matchResult = idPattern.find(text)
        return matchResult?.value // Return the matched ID number or null if not found
    }




    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val metrics = DisplayMetrics().also { viewBinding.viewFinder.display.getRealMetrics(it) }
            val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

            val previewWidth = viewBinding.viewFinder.width
            val previewHeight = viewBinding.viewFinder.height

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(previewWidth, previewHeight))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
