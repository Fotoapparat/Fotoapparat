package io.fotoapparat.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import io.fotoapparat.hardware.metering.FocalRequest
import io.fotoapparat.hardware.metering.PointF
import io.fotoapparat.parameter.Resolution

/**
 * A view which is metering the focus & exposure of the camera to specific areas, if possible.
 *
 * If the camera doesn't support focus metering on specific area this will only display a visual feedback.
 */
open class FocusView
@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), FocalPointSelector {

    private val visualFeedbackCircle = FeedbackCircleView(context, attrs, defStyleAttr)
    private var focusMeteringListener: ((FocalRequest) -> Unit)? = null

    var scaleListener: ((Float) -> Unit)? = null
    var ptrListener: ((Int) -> Unit)? = null

    private var mPtrCount: Int = 0
        set(value) {
            field = value
            ptrListener?.invoke(value)
        }

    init {
        clipToPadding = false
        clipChildren = false
        addView(visualFeedbackCircle)
    }

    override fun setFocalPointListener(listener: (FocalRequest) -> Unit) {
        focusMeteringListener = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        tapDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> mPtrCount++
            MotionEvent.ACTION_POINTER_UP -> mPtrCount--
            MotionEvent.ACTION_DOWN -> mPtrCount++
            MotionEvent.ACTION_UP -> mPtrCount--
        }
        return true
    }

    private val gestureDetectorListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return focusMeteringListener
                    ?.let {
                        it(FocalRequest(
                                point = PointF(
                                        x = e.x,
                                        y = e.y),
                                previewResolution = Resolution(
                                        width = width,
                                        height = height)))
                        visualFeedbackCircle.showAt(
                                x = e.x - visualFeedbackCircle.width / 2,
                                y = e.y - visualFeedbackCircle.height / 2)
                        performClick()

                        true
                    }
                    ?: super.onSingleTapUp(e)
        }
    }

    private val tapDetector = GestureDetector(context, gestureDetectorListener)

    private val scaleGestureDetector = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return scaleListener
                    ?.let {
                        it(detector.scaleFactor)
                        true
                    }?: super.onScale(detector)
        }
    }
    private val scaleDetector = ScaleGestureDetector(context, scaleGestureDetector)
}
