package com.vinzdits.gallery.view

import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max

/**
 * ImageView dengan pinch-to-zoom, drag, dan double-tap zoom.
 *
 * Dibuat sendiri sebagai pengganti library PhotoView (yang hanya tersedia
 * di JitPack) agar project ini 100% memakai dependensi Maven Central.
 *
 * View ini sadar ViewPager2: swipe horizontal diteruskan ke parent (pindah
 * foto) saat gambar belum di-zoom; saat di-zoom, gesture diambil alih
 * untuk menggeser gambar.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val drawMatrix = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = android.graphics.PointF()

    private var mode = MODE_NONE
    private var minScale = 1f
    private var maxScale = 5f
    private var fitted = false

    /** Dipanggil saat single-tap terkonfirmasi (untuk toggle overlay UI). */
    var onSingleTap: (() -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val values = FloatArray(9)
                drawMatrix.getValues(values)
                val current = values[Matrix.MSCALE_X]
                var factor = detector.scaleFactor
                factor = (current * factor).coerceIn(minScale, maxScale) / current
                drawMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                fixTranslation()
                imageMatrix = drawMatrix
                updateIntercept()
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTap?.invoke()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleZoom(e.x, e.y)
                return true
            }
        }
    )

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        fitted = false
        if (drawable != null && width > 0 && height > 0) {
            fitToScreen()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!fitted && drawable != null && w > 0 && h > 0) {
            fitToScreen()
        }
    }

    /** Kembalikan ke posisi fit-screen (dipakai saat view di-recycle). */
    fun resetZoom() {
        fitted = false
        if (drawable != null && width > 0 && height > 0) {
            fitToScreen()
        } else {
            drawMatrix.reset()
            imageMatrix = drawMatrix
        }
    }

    private fun fitToScreen() {
        val d = drawable ?: return
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW == 0f || viewH == 0f || d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) return
        val scale = max(
            minOf(viewW / d.intrinsicWidth, viewH / d.intrinsicHeight),
            0.0001f
        )
        minScale = scale
        drawMatrix.setScale(scale, scale)
        val dx = (viewW - d.intrinsicWidth * scale) / 2f
        val dy = (viewH - d.intrinsicHeight * scale) / 2f
        drawMatrix.postTranslate(dx, dy)
        imageMatrix = drawMatrix
        fitted = true
    }

    private fun toggleZoom(x: Float, y: Float) {
        val values = FloatArray(9)
        drawMatrix.getValues(values)
        val current = values[Matrix.MSCALE_X]
        val target = if (current > minScale * 1.1f) {
            minScale
        } else {
            (minScale * 2.5f).coerceAtMost(maxScale)
        }
        drawMatrix.postScale(target / current, target / current, x, y)
        fixTranslation()
        imageMatrix = drawMatrix
        updateIntercept()
    }

    private fun fixTranslation() {
        val d = drawable ?: return
        val values = FloatArray(9)
        drawMatrix.getValues(values)
        val scale = values[Matrix.MSCALE_X]
        val imgW = d.intrinsicWidth * scale
        val imgH = d.intrinsicHeight * scale
        val viewW = width.toFloat()
        val viewH = height.toFloat()

        var dx = 0f
        var dy = 0f

        if (imgW <= viewW) {
            dx = (viewW - imgW) / 2f - values[Matrix.MTRANS_X]
        } else {
            val left = values[Matrix.MTRANS_X]
            if (left > 0f) {
                dx = -left
            } else if (left + imgW < viewW) {
                dx = viewW - imgW - left
            }
        }

        if (imgH <= viewH) {
            dy = (viewH - imgH) / 2f - values[Matrix.MTRANS_Y]
        } else {
            val top = values[Matrix.MTRANS_Y]
            if (top > 0f) {
                dy = -top
            } else if (top + imgH < viewH) {
                dy = viewH - imgH - top
            }
        }

        drawMatrix.postTranslate(dx, dy)
    }

    private fun isZoomed(): Boolean {
        val values = FloatArray(9)
        drawMatrix.getValues(values)
        return values[Matrix.MSCALE_X] > minScale * 1.05f
    }

    private fun updateIntercept() {
        parent?.requestDisallowInterceptTouchEvent(isZoomed())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(drawMatrix)
                startPoint.set(event.x, event.y)
                mode = MODE_DRAG
                updateIntercept()
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == MODE_DRAG && !scaleDetector.isInProgress) {
                    drawMatrix.set(savedMatrix)
                    drawMatrix.postTranslate(event.x - startPoint.x, event.y - startPoint.y)
                    fixTranslation()
                    imageMatrix = drawMatrix
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = MODE_NONE
                updateIntercept()
            }
        }
        // Selalu konsumsi event agar stream gesture (drag/pinch) tidak terputus.
        return true
    }

    companion object {
        private const val MODE_NONE = 0
        private const val MODE_DRAG = 1
    }
}
