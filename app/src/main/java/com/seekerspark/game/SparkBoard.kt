package com.seekerspark.game

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class SparkBoard @JvmOverloads constructor(context: Context, val engine: GameEngine? = null) : View(context) {
    var onStats: (() -> Unit)? = null
    var onFinish: (() -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val sparkPath = Path()
    private var activeMs = 0L
    private var lastTick = 0L
    private var running = false
    private var finishSent = false
    private var flashCell = -1
    private var flashUntil = 0L
    private var flashText = ""
    private var flashGood = true
    private val density = resources.displayMetrics.density
    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            val now = SystemClock.elapsedRealtime()
            activeMs += (now - lastTick).coerceAtLeast(0)
            lastTick = now
            engine?.update(activeMs)
            onStats?.invoke()
            invalidate()
            if (engine?.finished == true) {
                running = false
                if (!finishSent) { finishSent = true; onFinish?.invoke() }
            } else postOnAnimation(this)
        }
    }

    init { contentDescription = "Game board. Tap green sparks, avoid red traps."; isFocusable = true }
    fun resumeGame() {
        if (engine == null || running) return
        if (engine.finished) {
            if (!finishSent) { finishSent = true; onFinish?.invoke() }
            return
        }
        running = true; lastTick = SystemClock.elapsedRealtime(); postOnAnimation(frame)
    }
    fun pauseGame() {
        if (running) { activeMs += (SystemClock.elapsedRealtime() - lastTick).coerceAtLeast(0); engine?.update(activeMs) }
        running = false; removeCallbacks(frame)
    }
    override fun onDetachedFromWindow() { pauseGame(); super.onDetachedFromWindow() }

    private fun geometry(): FloatArray {
        val gap = 10f * density
        val cell = min((width - gap * 4) / 3f, (height - gap * 5) / 4f).coerceAtLeast(1f)
        return floatArrayOf(cell, gap, (width - (cell * 3 + gap * 2)) / 2f, (height - (cell * 4 + gap * 3)) / 2f)
    }
    fun cellCenter(cell: Int): Pair<Float, Float> {
        val g = geometry()
        return Pair(g[2] + (cell % 3) * (g[0] + g[1]) + g[0] / 2, g[3] + (cell / 3) * (g[0] + g[1]) + g[0] / 2)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val g = geometry(); val size = g[0]
        for (cell in 0 until 12) {
            val center = cellCenter(cell)
            rect.set(center.first - size / 2, center.second - size / 2, center.first + size / 2, center.second + size / 2)
            paint.style = Paint.Style.FILL; paint.color = Color.rgb(19, 28, 46)
            canvas.drawRoundRect(rect, 18 * density, 18 * density, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = density; paint.color = Color.rgb(33, 46, 67)
            canvas.drawRoundRect(rect, 18 * density, 18 * density, paint)
            paint.style = Paint.Style.FILL
            val target = engine?.targets?.firstOrNull { it.cell == cell }
            val isDemo = engine == null && cell in listOf(1, 6, 11)
            if (target != null || isDemo) {
                val trap = target?.kind == GameEngine.Kind.TRAP || (isDemo && cell == 11)
                val color = if (trap) Color.rgb(255, 103, 133) else Color.rgb(66, 237, 192)
                paint.color = color; paint.alpha = 28
                canvas.drawCircle(center.first, center.second, size * 0.36f, paint)
                paint.alpha = 255; paint.color = color
                if (trap) {
                    paint.strokeWidth = 4 * density; paint.strokeCap = Paint.Cap.ROUND
                    val d = size * 0.12f
                    canvas.drawLine(center.first - d, center.second - d, center.first + d, center.second + d, paint)
                    canvas.drawLine(center.first - d, center.second + d, center.first + d, center.second - d, paint)
                } else {
                    val d = size * 0.24f
                    val path = sparkPath.apply {
                        reset()
                        moveTo(center.first + d * 0.30f, center.second - d)
                        lineTo(center.first - d * 0.58f, center.second + d * 0.12f)
                        lineTo(center.first - d * 0.03f, center.second + d * 0.12f)
                        lineTo(center.first - d * 0.30f, center.second + d)
                        lineTo(center.first + d * 0.60f, center.second - d * 0.18f)
                        lineTo(center.first + d * 0.05f, center.second - d * 0.18f); close()
                    }
                    canvas.drawPath(path, paint)
                }
                if (target != null) {
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 2 * density; paint.alpha = 130
                    val r = size * 0.39f
                    rect.set(center.first - r, center.second - r, center.first + r, center.second + r)
                    val fraction = ((target.expires - activeMs).toFloat() / (target.expires - target.born)).coerceIn(0f, 1f)
                    canvas.drawArc(rect, -90f, fraction * 360, false, paint)
                    paint.alpha = 255; paint.style = Paint.Style.FILL
                }
            } else {
                paint.color = Color.rgb(46, 59, 79); canvas.drawCircle(center.first, center.second, 2 * density, paint)
            }
            if (cell == flashCell && SystemClock.elapsedRealtime() < flashUntil) {
                paint.color = if (flashGood) Color.rgb(66, 237, 192) else Color.rgb(255, 103, 133)
                paint.textSize = 24 * density; paint.typeface = Typeface.create("sans-serif", Typeface.BOLD); paint.textAlign = Paint.Align.CENTER
                canvas.drawText(flashText, center.first, center.second + 8 * density, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (engine == null || !running) return false
        if (event.action != MotionEvent.ACTION_DOWN) return true
        val g = geometry()
        for (cell in 0 until 12) {
            val center = cellCenter(cell)
            if (kotlin.math.abs(event.x - center.first) <= g[0] / 2 && kotlin.math.abs(event.y - center.second) <= g[0] / 2) {
                val now = SystemClock.elapsedRealtime()
                activeMs += (now - lastTick).coerceAtLeast(0); lastTick = now
                engine.tap(cell, activeMs)?.let {
                    flashCell = cell; flashUntil = now + 180; flashGood = it.points > 0; flashText = if (flashGood) "+${it.points}" else "−10"
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                }
                onStats?.invoke(); invalidate(); performClick(); break
            }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
