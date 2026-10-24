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
 * Dedicated, permanent Virtual Mouse Pointer for Android TV.
 * Exclusively controls mouse movement and synthetic touch clicks,
 * completely replacing raw TV remote arrow events.
 */
class VirtualMouseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cursorX = 960f
    private var cursorY = 540f

    private val cursorRadius = 15f
    private val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.cursor_glow)
        style = Paint.Style.FILL
    }
    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
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
    private val maxSpeedMultiplier = 3.5f
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
        val radius = cursorRadius * clickScale
        // Outer glow
        canvas.drawCircle(cursorX, cursorY, radius * 1.6f, paintCircle)
        // White border ring
        canvas.drawCircle(cursorX, cursorY, radius, paintRing)
        // Center red dot
        canvas.drawCircle(cursorX, cursorY, 4f, paintDot)
    }

    /**
     * Intercepts all TV Remote D-Pad directional keys to move cursor.
     * Guaranteed to return true to consume the key so nothing reaches the video player.
     */
    fun handleRemoteKey(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            continuousPressCount = 0
            currentSpeedMultiplier = 1.0f
            return true
        }

        if (event.action != KeyEvent.ACTION_DOWN) return true

        // Acceleration on holding down arrow
        val repeatBoost = (event.repeatCount * 0.12f).coerceAtMost(2.0f)
        if (keyCode == lastDirection) {
            continuousPressCount++
            currentSpeedMultiplier = (1.0f + continuousPressCount * 0.15f + repeatBoost).coerceAtMost(maxSpeedMultiplier)
        } else {
            lastDirection = keyCode
            continuousPressCount = 0
            currentSpeedMultiplier = 1.0f + repeatBoost
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
        cursorX = (cursorX + dx).coerceIn(10f, width.toFloat() - 10f)
        cursorY = (cursorY + dy).coerceIn(10f, height.toFloat() - 10f)

        val target = targetView
        if (target != null) {
            // Hover move event for interactive web styling
            val now = SystemClock.uptimeMillis()
            val hoverEvent = MotionEvent.obtain(
                now, now, MotionEvent.ACTION_HOVER_MOVE,
                cursorX, cursorY, 0
            )
            target.dispatchGenericMotionEvent(hoverEvent)
            hoverEvent.recycle()

            // Page scrolling when reaching edges
            if (cursorY <= 30f) {
                target.scrollBy(0, -65)
            } else if (cursorY >= height - 30f) {
                target.scrollBy(0, 65)
            }
        }

        invalidate()
    }

    /**
     * Executes real touch click at the exact cursor position.
     */
    fun performSyntheticClick() {
        val target = targetView ?: return

        // Click visual ripple animation
        val anim = ValueAnimator.ofFloat(1.0f, 0.65f, 1.0f).apply {
            duration = 140
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

        // 2. Send ACTION_UP after 50ms
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
        }, 50)
    }
}
