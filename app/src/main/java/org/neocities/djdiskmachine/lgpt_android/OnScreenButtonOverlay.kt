package org.neocities.djdiskmachine.lgpt_android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

class OnScreenButtonOverlay(context: Context) : View(context) {
    data class ButtonDef(
        val id: Int,
        val label: String,
        val keyCode: Int,
        var bounds: RectF,
        var isPressed: Boolean = false,
        val isDpad: Boolean = false
    )

    private val buttons = listOf(
        ButtonDef(0, "↑", KeyEvent.KEYCODE_I, RectF(), isDpad = false),    // Up
        ButtonDef(1, "↓", KeyEvent.KEYCODE_K, RectF(), isDpad = false),    // Down
        ButtonDef(2, "←", KeyEvent.KEYCODE_J, RectF(), isDpad = false),    // Left
        ButtonDef(3, "→", KeyEvent.KEYCODE_L, RectF(), isDpad = false),    // Right
        ButtonDef(4, "A", KeyEvent.KEYCODE_S, RectF()),                    // A
        ButtonDef(5, "B", KeyEvent.KEYCODE_A, RectF()),                    // B
        ButtonDef(6, "Start", KeyEvent.KEYCODE_SPACE, RectF()),            // Start
        ButtonDef(7, "Select", KeyEvent.KEYCODE_TAB, RectF()),             // Select (hidden)
        ButtonDef(8, "L", KeyEvent.KEYCODE_Q, RectF()),                    // L shoulder
        ButtonDef(9, "R", KeyEvent.KEYCODE_W, RectF()),                    // R shoulder
        ButtonDef(10, "cut", KeyEvent.KEYCODE_Z, RectF())                  // cut (A+B)
    )

    private val paint = Paint().apply {
        color = 0xFF7B68A6.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintPressed = Paint().apply {
        color = 0xFF6A5A94.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintStroke = Paint().apply {
        color = 0xFF4A3A74.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val touchMap = mutableMapOf<Int, ButtonDef>() // pointerId -> button

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutButtons(w, h)
    }

    private fun layoutButtons(width: Int, height: Int) {
        val btnSize = 160
        val spacing = 8
        val dpadX = (width * 0.05f).toInt()

        // L button (bottom left) - moved up by one button height
        buttons[8].bounds = RectF(
            dpadX.toFloat(),
            (height - btnSize * 2).toFloat(),
            (dpadX + btnSize * 2).toFloat(),
            (height - btnSize).toFloat()
        )

        // Start button (bottom center) - moved up by one button height
        buttons[6].bounds = RectF(
            (width / 2 - btnSize / 2).toFloat(),
            (height - btnSize * 2).toFloat(),
            (width / 2 + btnSize / 2).toFloat(),
            (height - btnSize).toFloat()
        )

        // R button (bottom right) - moved up by one button height
        val mirrorLeft = width - (dpadX + btnSize * 2)
        buttons[9].bounds = RectF(
            mirrorLeft.toFloat(),
            (height - btnSize * 2).toFloat(),
            (mirrorLeft + btnSize * 2).toFloat(),
            (height - btnSize).toFloat()
        )

        // Now position D-pad buttons - Left / Down / Right on same line, Up above
        val shoulderY = (height - btnSize * 2).toFloat()
        val leftRightY = shoulderY - btnSize * 2 - spacing

        // Left button
        buttons[2].bounds = RectF(
            dpadX.toFloat(),
            leftRightY,
            (dpadX + btnSize).toFloat(),
            leftRightY + btnSize
        )

        // Down button: center, same Y as Left/Right
        buttons[1].bounds = RectF(
            (dpadX + btnSize + spacing).toFloat(),
            leftRightY,
            (dpadX + btnSize * 2 + spacing).toFloat(),
            leftRightY + btnSize
        )

        // Right button
        buttons[3].bounds = RectF(
            (dpadX + btnSize * 2 + spacing * 2).toFloat(),
            leftRightY,
            (dpadX + btnSize * 3 + spacing * 2).toFloat(),
            leftRightY + btnSize
        )

        // Up button: above Left/Down/Right
        val upY = leftRightY - btnSize - spacing
        buttons[0].bounds = RectF(
            (dpadX + btnSize + spacing).toFloat(),
            upY,
            (dpadX + btnSize * 2 + spacing).toFloat(),
            upY + btnSize
        )

        // B button - anchor at leftmost of R button, same height as Left/Down/Right
        val rLeft = buttons[9].bounds.left
        buttons[5].bounds = RectF(
            rLeft,
            leftRightY,
            rLeft + btnSize,
            leftRightY + btnSize
        )

        // Cut button - to the left of A
        val rRight = buttons[9].bounds.right
        buttons[10].bounds = RectF(
            rRight - btnSize * 2 - spacing,
            upY,
            rRight - btnSize - spacing,
            upY + btnSize
        )

        // A button - to the right of cut
        buttons[4].bounds = RectF(
            rRight - btnSize,
            upY,
            rRight,
            upY + btnSize
        )

        // Select button (hidden by default, can place it elsewhere or in a submenu)
        buttons[7].bounds = RectF(-1000f, -1000f, -900f, -900f) // Off-screen
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        buttons.forEach { button ->
            val buttonPaint = if (button.isPressed) paintPressed else paint

            // Draw button background
            if (button.isDpad) {
                // Draw D-pad button as diamond/cross shape
                drawDpadButton(canvas, button, buttonPaint)
            } else {
                // Draw action button as rounded rectangle
                val radius = 8f
                canvas.drawRoundRect(button.bounds, radius, radius, buttonPaint)
                canvas.drawRoundRect(button.bounds, radius, radius, paintStroke)
            }

            // Draw label
            val cx = button.bounds.centerX()
            val cy = button.bounds.centerY() + textPaint.textSize / 3
            canvas.drawText(button.label, cx, cy, textPaint)
        }
    }

    private fun drawDpadButton(canvas: Canvas, button: ButtonDef, paint: Paint) {
        val bounds = button.bounds
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val hw = bounds.width() / 2
        val hh = bounds.height() / 2

        // Draw as a circle for simplicity
        canvas.drawCircle(cx, cy, hw.coerceAtMost(hh), paint)
        canvas.drawCircle(cx, cy, hw.coerceAtMost(hh), paintStroke)
    }

    private fun dispatchButtonKeyEvent(action: Int, button: ButtonDef) {
        if (button.id == 10) {
            // Cut button: dispatch both A and B
            dispatchKeyEvent(KeyEvent(action, KeyEvent.KEYCODE_S))  // A
            dispatchKeyEvent(KeyEvent(action, KeyEvent.KEYCODE_A))  // B
        } else {
            dispatchKeyEvent(KeyEvent(action, button.keyCode))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                val button = buttons.find { it.bounds.contains(x, y) }
                if (button != null) {
                    button.isPressed = true
                    touchMap[pointerId] = button
                    dispatchButtonKeyEvent(KeyEvent.ACTION_DOWN, button)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)

                val button = touchMap[pointerId]
                if (button != null) {
                    button.isPressed = false
                    touchMap.remove(pointerId)
                    dispatchButtonKeyEvent(KeyEvent.ACTION_UP, button)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Update pressed state based on movement
                (0 until event.pointerCount).forEach { pointerIndex ->
                    val pointerId = event.getPointerId(pointerIndex)
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)

                    val oldButton = touchMap[pointerId]
                    val newButton = buttons.find { it.bounds.contains(x, y) }

                    if (oldButton != newButton) {
                        if (oldButton != null) {
                            oldButton.isPressed = false
                            dispatchButtonKeyEvent(KeyEvent.ACTION_UP, oldButton)
                        }
                        if (newButton != null) {
                            newButton.isPressed = true
                            touchMap[pointerId] = newButton
                            dispatchButtonKeyEvent(KeyEvent.ACTION_DOWN, newButton)
                        } else {
                            touchMap.remove(pointerId)
                        }
                        invalidate()
                    }
                }
            }
        }
        return true
    }

    override fun dispatchKeyEvent(keyEvent: KeyEvent): Boolean {
        return (context as? android.app.Activity)?.dispatchKeyEvent(keyEvent) ?: super.dispatchKeyEvent(keyEvent)
    }
}
