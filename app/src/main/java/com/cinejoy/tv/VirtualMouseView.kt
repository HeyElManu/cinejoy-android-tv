package com.cinejoy.tv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

/**
 * High-precision Virtual Mouse Pointer for Android TV.
 * Translates TV Remote D-Pad directional keys into smooth screen cursor motion
 * and generates real touch clicks, preventing the video player from hijacking arrow keys.
 */
class VirtualMouseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cursorX = 960f
    private var cursorY = 540f

    private val cursorRadius = 16f
    private val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.cursor_glow)
        style = Paint.Style.FILL
    }
    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.cursor_dot)
        style = Paint.Style.FILL
    }

    private var targetView: View? = null
    private var clickScale = 1.0f

    // Motion parameters
    private val baseStep = 24f
    private var currentSpeedMultiplier = 1.0f
    private val maxSpeedMultiplier = 3.2f
    private var lastDirection: Int = -1
    private var continuousPressCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    fun attachTargetView(view: View) {
        this.targetView = view
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (cursorX == 960f && cursorY == 540f && w > 0 && h > 0) {
            cursorX = w / 2f
            cursorY = h / 2f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (visibility != VISIBLE) return

        val radius = cursorRadius * clickScale
        // Outer translucent glow ring
        canvas.drawCircle(cursorX, cursorY, radius * 1.6f, paintCircle)
        // White boundary ring
        canvas.drawCircle(cursorX, cursorY, radius, paintRing)
        // Center red bullseye dot
        canvas.drawCircle(cursorX, cursorY, 4f, paintDot)
    }

    /**
     * Intercepts D-Pad keys so that arrow keys move the pointer instead of
     * altering the video player's volume or seeking the video.
     */
    fun handleRemoteKey(keyCode: Int, event: KeyEvent): Boolean {
        if (visibility != VISIBLE) return false

        if (event.action == KeyEvent.ACTION_UP) {
            continuousPressCount = 0
            currentSpeedMultiplier = 1.0f
            return true
        }

        if (event.action != KeyEvent.ACTION_DOWN) return false

        // Acceleration on holding button
        if (keyCode == lastDirection) {
            continuousPressCount++
            if (continuousPressCount > 2) {
                currentSpeedMultiplier = (1.0f + continuousPressCount * 0.18f).coerceAtMost(maxSpeedMultiplier)
            }
        } else {
            lastDirection = keyCode
            continuousPressCount = 0
            currentSpeedMultiplier = 1.0f
        }

        val step = baseStep * currentSpeedMultiplier

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                moveCursor(0f, -step)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                moveCursor(0f, step)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                moveCursor(-step, 0f)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                moveCursor(step, 0f)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                performSyntheticClick()
                return true
            }
        }
        return false
    }

    private fun moveCursor(dx: Float, dy: Float) {
        cursorX = (cursorX + dx).coerceIn(12f, width.toFloat() - 12f)
        cursorY = (cursorY + dy).coerceIn(12f, height.toFloat() - 12f)

        // Dispatch hover move to target WebView so interactive buttons react
        val target = targetView
        if (target != null) {
            val now = SystemClock.uptimeMillis()
            val hoverEvent = MotionEvent.obtain(
                now, now, MotionEvent.ACTION_HOVER_MOVE,
                cursorX, cursorY, 0
            )
            target.dispatchGenericMotionEvent(hoverEvent)
            hoverEvent.recycle()

            // Auto-scroll when near top or bottom edges
            if (cursorY <= 35f) {
                target.scrollBy(0, -70)
            } else if (cursorY >= height - 35f) {
                target.scrollBy(0, 70)
            }
        }

        invalidate()
    }

    /**
     * Performs a native touch click at the exact cursor position.
     */
    fun performSyntheticClick() {
        val target = targetView ?: return

        // Click animation ripple
        val anim = ValueAnimator.ofFloat(1.0f, 0.65f, 1.0f).apply {
            duration = 150
            addUpdateListener {
                clickScale = it.animatedValue as Float
                invalidate()
            }
        }
        anim.start()

        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        // 1. Send ACTION_DOWN
        val downEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            cursorX,
            cursorY,
            0
        )
        target.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        // 2. Send ACTION_UP after 60ms
        mainHandler.postDelayed({
            val upTime = SystemClock.uptimeMillis()
            val upEvent = MotionEvent.obtain(
                downTime,
                upTime,
                MotionEvent.ACTION_UP,
                cursorX,
                cursorY,
                0
            )
            target.dispatchTouchEvent(upEvent)
            upEvent.recycle()
        }, 60)
    }
}
