package app.bolinhamodos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class OverlayService : Service() {

    companion object {
        @Volatile
        var running: Boolean = false
    }

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var container: LinearLayout? = null
    private var panel: LinearLayout? = null
    private var badge: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
        buildOverlay()
        running = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        try {
            container?.let { wm.removeView(it) }
        } catch (e: Exception) {
            // already removed
        }
        container = null
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun startAsForeground() {
        val channelId = "bolinha"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Bolinha de modos", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, channelId)
            .setContentTitle("Bolinha de modos ligada")
            .setContentText("Toque para abrir o app")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun buildOverlay() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL

        val bubble = FrameLayout(this)
        val bubbleBg = GradientDrawable()
        bubbleBg.shape = GradientDrawable.OVAL
        bubbleBg.setColor(Color.parseColor("#D97757"))
        bubble.background = bubbleBg

        val icon = ImageView(this)
        icon.setImageResource(android.R.drawable.ic_menu_manage)
        icon.setColorFilter(Color.WHITE)
        val iconLp = FrameLayout.LayoutParams(dp(28), dp(28))
        iconLp.gravity = Gravity.CENTER
        bubble.addView(icon, iconLp)

        val dot = View(this)
        val dotBg = GradientDrawable()
        dotBg.shape = GradientDrawable.OVAL
        dotBg.setColor(Color.parseColor("#2E9E5B"))
        dotBg.setStroke(dp(2), Color.WHITE)
        dot.background = dotBg
        dot.visibility = View.GONE
        val dotLp = FrameLayout.LayoutParams(dp(16), dp(16))
        dotLp.gravity = Gravity.TOP or Gravity.END
        bubble.addView(dot, dotLp)
        badge = dot

        box.addView(bubble, LinearLayout.LayoutParams(dp(56), dp(56)))

        val p = LinearLayout(this)
        p.orientation = LinearLayout.VERTICAL
        p.setPadding(dp(14), dp(8), dp(14), dp(10))
        val panelBg = GradientDrawable()
        panelBg.setColor(Color.WHITE)
        panelBg.cornerRadius = dp(16).toFloat()
        panelBg.setStroke(dp(1), Color.parseColor("#C9C9C9"))
        p.background = panelBg
        p.visibility = View.GONE
        val panelLp = LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT)
        panelLp.topMargin = dp(8)
        box.addView(p, panelLp)
        panel = p

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = dp(8)
        params.y = dp(220)

        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var moved = false

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = e.rawX
                        touchY = e.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - touchX).toInt()
                        val dy = (e.rawY - touchY).toInt()
                        if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                        if (moved) {
                            params.x = startX + dx
                            params.y = startY + dy
                            container?.let { wm.updateViewLayout(it, params) }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePanel()
                        return true
                    }
                }
                return false
            }
        })

        container = box
        wm.addView(box, params)
        refreshBadge()
    }

    private fun togglePanel() {
        val p = panel ?: return
        if (p.visibility == View.VISIBLE) {
            p.visibility = View.GONE
        } else {
            rebuildPanel()
            p.visibility = View.VISIBLE
            val screenW = resources.displayMetrics.widthPixels
            val maxX = screenW - dp(260) - dp(8)
            if (params.x > maxX) params.x = maxX
            if (params.x < 0) params.x = 0
        }
        container?.let { wm.updateViewLayout(it, params) }
    }

    private fun rebuildPanel() {
        val p = panel ?: return
        p.removeAllViews()
        val modes = Store.load(this)

        if (modes.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Nenhum modo. Crie um no app."
            empty.setTextColor(Color.parseColor("#1F1F1F"))
            empty.textSize = 16f
            p.addView(empty)
        }

        for (m in modes) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.minimumHeight = dp(52)

            val label = TextView(this)
            label.text = m.name
            label.textSize = 17f
            label.setTextColor(Color.parseColor("#1F1F1F"))
            row.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val sw = Switch(this)
            sw.isChecked = Store.isOn(this, m.id)
            sw.setOnCheckedChangeListener { _, checked ->
                Store.setOn(this, m.id, checked)
                if (checked) {
                    copyText(m.prompt, m.name + " (ligar)")
                } else {
                    copyText(m.offCommand, m.name + " (desligar)")
                }
                refreshBadge()
            }
            row.addView(sw)
            p.addView(row)
        }

        val offAll = Button(this)
        offAll.text = "Desligar todos"
        offAll.setOnClickListener {
            for (m in Store.load(this)) Store.setOn(this, m.id, false)
            copyText("Desativar todos os modos", "desligar todos")
            rebuildPanel()
            refreshBadge()
        }
        p.addView(offAll)

        val close = Button(this)
        close.text = "Fechar bolinha"
        close.setOnClickListener { stopSelf() }
        p.addView(close)
    }

    private fun refreshBadge() {
        badge?.visibility = if (Store.anyOn(this)) View.VISIBLE else View.GONE
    }

    private fun copyText(text: String, label: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("modo", text))
        Toast.makeText(this, "Copiado: " + label, Toast.LENGTH_SHORT).show()
    }
}
