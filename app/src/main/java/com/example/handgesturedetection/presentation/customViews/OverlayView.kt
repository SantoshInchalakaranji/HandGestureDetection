package com.example.handgesturedetection.presentation.customViews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.handgesturedetection.R
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: GestureRecognizerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private val gestureImages = mapOf(
        "Open_Palm" to R.drawable.one,
        "Closed_Fist" to R.drawable.two,
        "Thumb_Up" to R.drawable.three,
        "Thumb_Down" to R.drawable.four,

        )

    init {
        initPaints()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_secondary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { gestureRecognizerResult ->
            for (landmark in gestureRecognizerResult.landmarks()) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                HandLandmarker.HAND_CONNECTIONS.forEach {
                    canvas.drawLine(
                        gestureRecognizerResult.landmarks().get(0).get(it!!.start())
                            .x() * imageWidth * scaleFactor,
                        gestureRecognizerResult.landmarks().get(0).get(it.start())
                            .y() * imageHeight * scaleFactor,
                        gestureRecognizerResult.landmarks().get(0).get(it.end())
                            .x() * imageWidth * scaleFactor,
                        gestureRecognizerResult.landmarks().get(0).get(it.end())
                            .y() * imageHeight * scaleFactor,
                        linePaint
                    )

                    // Draw the image on top of the detected hand based on the recognized gesture
                    if (gestureRecognizerResult.gestures().isNotEmpty()) {
                        val recognizedGesture =
                            gestureRecognizerResult.gestures().first().first().categoryName()
                        recognizedGesture?.let {
                            val imageResource = gestureImages[it]
                            imageResource?.let { resourceId ->
                                val bitmap = BitmapFactory.decodeResource(resources, resourceId)
                                val scaledBitmap =
                                    Bitmap.createScaledBitmap(bitmap, 300, 300, false)
                                val middle = landmark.size/2
                                canvas.drawBitmap(
                                    scaledBitmap,
                                    landmark.get(middle).x() * imageWidth * scaleFactor,
                                    landmark.get(middle).y() * imageHeight * scaleFactor,
                                    null
                                )
                            }
                        }
                    }

                }
            }
        }
    }
    fun setResults(
        gestureRecognizerResult: GestureRecognizerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = gestureRecognizerResult
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}
