package com.fruitbot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.min

/**
 * Screen capture karta hai, knife ke raaste (ek patli vertical patti) mein
 * bright/rangeen pixels ginta hai, aur fruit dikhne par TapService se tap karwata hai.
 */
class BotService : Service() {

    companion object {
        const val ACTION_STOP = "com.fruitbot.STOP"
        const val CHANNEL = "fruitbot"
    }

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vdisplay: VirtualDisplay? = null
    private var thread: HandlerThread? = null
    private lateinit var prefs: SharedPreferences

    private var running = false
    private var fullW = 1080
    private var fullH = 1920
    private var lastTap = 0L
    private var lastNotif = 0L
    private var lastBomb = -10000L
    private var taps = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAll()
            return START_NOT_STICKY
        }
        if (running) return START_NOT_STICKY

        prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        createChannel()
        val n = buildNotif("Bot chalu hai")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, n)
        }

        val code = intent?.getIntExtra("code", 0) ?: 0
        val data = readData(intent)
        if (data == null) {
            stopAll()
            return START_NOT_STICKY
        }

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mpm.getMediaProjection(code, data)
        projection = proj

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.maximumWindowMetrics.bounds
            fullW = b.width()
            fullH = b.height()
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            fullW = dm.widthPixels
            fullH = dm.heightPixels
        }
        // Aadhe size par capture: tez aur halka.
        val sw = max(2, fullW / 2)
        val sh = max(2, fullH / 2)

        val t = HandlerThread("fruitbot").also { it.start() }
        thread = t
        val handler = Handler(t.looper)

        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopAll()
            }
        }, handler)

        val r = ImageReader.newInstance(sw, sh, PixelFormat.RGBA_8888, 2)
        reader = r
        vdisplay = proj.createVirtualDisplay(
            "fruitbot", sw, sh, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, r.surface, null, handler
        )
        running = true
        r.setOnImageAvailableListener({ rd -> analyze(rd) }, handler)
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun readData(intent: Intent?): Intent? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("data", Intent::class.java)
        } else {
            intent.getParcelableExtra("data")
        }
    }

    private fun analyze(rd: ImageReader) {
        val img = try {
            rd.acquireLatestImage()
        } catch (e: Exception) {
            null
        } ?: return
        try {
            val plane = img.planes[0]
            val buf = plane.buffer
            val rowStride = plane.rowStride
            val pxStride = plane.pixelStride
            val w = img.width
            val h = img.height

            val xPct = prefs.getInt("x", 50)
            val cx = xPct * w / 100
            val half = max(2, prefs.getInt("width", 4) * w / 200)
            val y0 = prefs.getInt("top", 12) * h / 100
            val y1 = prefs.getInt("bottom", 66) * h / 100
            val xs = max(0, cx - half)
            val xe = min(w - 1, cx + half)

            var count = 0
            var black = 0
            var red = 0
            var y = y0
            while (y < y1) {
                var x = xs
                while (x <= xe) {
                    val o = y * rowStride + x * pxStride
                    val rr = buf.get(o).toInt() and 0xFF
                    val gg = buf.get(o + 1).toInt() and 0xFF
                    val bb = buf.get(o + 2).toInt() and 0xFF
                    val mx = max(rr, max(gg, bb))
                    val mn = min(rr, min(gg, bb))
                    // Bright (V >= ~0.65) aur rangeen (S >= 0.55) pixel = fruit.
                    if (mx >= 166 && (mx - mn) * 100 >= mx * 55) count++
                    // Bomb: kala body (lagbhag pure black) + gahri laal ring.
                    if (mx <= 24) black++
                    if (rr >= 200 && gg <= 60 && bb <= 60) red++
                    x++
                }
                y++
            }

            val now = SystemClock.uptimeMillis()
            val bomb = black >= prefs.getInt("bomb", 60) && red >= 10
            if (bomb) lastBomb = now
            // Bomb dikhne ke baad thodi der (400 ms) tap band, taaki bomb par tap na ho.
            val bombBlock = now - lastBomb < 400
            if (!bombBlock && count >= prefs.getInt("min", 40) &&
                now - lastTap >= prefs.getInt("cooldown", 700)
            ) {
                lastTap = now
                val tx = fullW * xPct / 100f
                val ty = fullH * prefs.getInt("tapy", 85) / 100f
                TapService.instance?.tap(tx, ty)
                taps++
            }

            if (now - lastNotif > 1000) {
                lastNotif = now
                getSystemService(NotificationManager::class.java)
                    .notify(1, buildNotif("pixels=$count  bomb=$black/$red  taps=$taps"))
            }
        } finally {
            img.close()
        }
    }

    private fun stopAll() {
        if (!running && projection == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        running = false
        try {
            reader?.setOnImageAvailableListener(null, null)
        } catch (_: Exception) {
        }
        vdisplay?.release()
        vdisplay = null
        reader?.close()
        reader = null
        val p = projection
        projection = null
        try {
            p?.stop()
        } catch (_: Exception) {
        }
        thread?.quitSafely()
        thread = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (running) stopAll()
        super.onDestroy()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL, "Fruit Bot", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    @Suppress("DEPRECATION")
    private fun buildNotif(text: String): Notification {
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, BotService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Fruit Bot")
            .setContentText(text)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPi)
            .build()
    }
}
