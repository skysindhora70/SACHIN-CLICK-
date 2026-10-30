package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.domain.AutomationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isExpanded = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayView()
        AutomationManager.setOverlayActive(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        val wm = windowManager ?: return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 250
        }

        // Root container
        val root = FrameLayout(this)

        // Pill card
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            elevation = dpToPx(8).toFloat()

            val bg = GradientDrawable().apply {
                setColor(0xEE0F172A.toInt()) // Deep slate
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(1), 0xFF38BDF8.toInt()) // Sky blue subtle border
            }
            background = bg
        }

        // Status Indicator Dot
        val statusDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(10), dpToPx(10)).apply {
                marginEnd = dpToPx(8)
            }
            background = createCircleDrawable(0xFF10B981.toInt())
        }
        container.addView(statusDot)

        // Title / Status Label
        val statusText = TextView(this).apply {
            text = "Sachin: Active"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(10)
            }
        }
        container.addView(statusText)

        // Start/Stop Toggle Button
        val toggleBtn = TextView(this).apply {
            text = "Stop"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            background = createRoundedDrawable(0xFFEF4444.toInt(), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }
            setOnClickListener {
                if (AutomationManager.isAutomationRunning.value) {
                    AutomationManager.stopAutomation(this@FloatingOverlayService, "Stopped via floating overlay")
                } else {
                    AutomationManager.startAutomation(this@FloatingOverlayService)
                }
            }
        }
        container.addView(toggleBtn)

        // Emergency Stop Button (Fast kill)
        val emergencyBtn = TextView(this).apply {
            text = "EMG"
            setTextColor(0xFFFEF2F2.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            background = createRoundedDrawable(0xFF991B1B.toInt(), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }
            setOnClickListener {
                AutomationManager.emergencyStop(this@FloatingOverlayService)
            }
        }
        container.addView(emergencyBtn)

        // Close Overlay Button
        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(0xFF94A3B8.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
            setOnClickListener {
                stopSelf()
            }
        }
        container.addView(closeBtn)

        root.addView(container)

        // Touch Drag Listener with click threshold
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        wm.updateViewLayout(root, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Toggle expand / collapse details
                        isExpanded = !isExpanded
                        emergencyBtn.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        statusText.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    }
                    true
                }
                else -> false
            }
        }

        wm.addView(root, params)
        overlayView = root

        // Collect state updates to keep overlay in sync
        serviceScope.launch {
            AutomationManager.isAutomationRunning.collectLatest { running ->
                if (running) {
                    statusDot.background = createCircleDrawable(0xFF10B981.toInt()) // Emerald
                    statusText.text = "Sachin: Active"
                    toggleBtn.text = "Stop"
                    toggleBtn.background = createRoundedDrawable(0xFFEF4444.toInt(), dpToPx(12))
                } else {
                    statusDot.background = createCircleDrawable(0xFF94A3B8.toInt()) // Gray
                    statusText.text = "Sachin: Idle"
                    toggleBtn.text = "Start"
                    toggleBtn.background = createRoundedDrawable(0xFF10B981.toInt(), dpToPx(12))
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun createRoundedDrawable(color: Int, radiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(color)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { view ->
            windowManager?.removeView(view)
            overlayView = null
        }
        AutomationManager.setOverlayActive(false)
        serviceScope.cancel()
    }
}
