package dev.prajwal.waterreminder.ui

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

enum class PostureState {
    INITIALIZING,
    GOOD,
    TILTED_DOWN,
    LEANING_CLOSE,
    TURNED_AWAY,
    NO_FACE
}

class PostureAnalyzer(
    private val onStateChanged: (PostureState) -> Unit
) : ImageAnalysis.Analyzer {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onStateChanged(PostureState.NO_FACE)
                } else {
                    val face = faces[0]
                    val pitch = face.headEulerAngleX  // nodding up (positive) or down (negative)
                    val yaw = face.headEulerAngleY    // turning left/right
                    
                    val isRotated = rotation == 90 || rotation == 270
                    val frameWidth = if (isRotated) mediaImage.height else mediaImage.width
                    val faceWidth = face.boundingBox.width()
                    val ratio = faceWidth.toFloat() / frameWidth

                    val newState = when {
                        yaw > 22f || yaw < -22f -> PostureState.TURNED_AWAY
                        ratio > 0.40f -> PostureState.LEANING_CLOSE
                        pitch < -12f -> PostureState.TILTED_DOWN
                        else -> PostureState.GOOD
                    }
                    onStateChanged(newState)
                }
            }
            .addOnFailureListener {
                onStateChanged(PostureState.NO_FACE)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
