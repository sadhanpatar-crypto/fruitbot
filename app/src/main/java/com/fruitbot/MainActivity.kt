package com.fruitbot

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var status: TextView
    private val reqCapture = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("cfg", Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val scroll = ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        }
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "Fruit Bot"
            textSize = 24f
        })
        status = TextView(this)
        root.addView(status)

        root.addView(Button(this).apply {
            text = "1. Accessibility chalu karo"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })
        root.addView(Button(this).apply {
            text = "2. Bot Start (screen capture allow karo)"
            setOnClickListener {
                if (TapService.instance == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Pehle Accessibility mein Fruit Bot chalu karo",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    @Suppress("DEPRECATION")
                    startActivityForResult(mpm.createScreenCaptureIntent(), reqCapture)
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Bot Stop"
            setOnClickListener {
                startService(
                    Intent(this@MainActivity, BotService::class.java)
                        .setAction(BotService.ACTION_STOP)
                )
            }
        })

        root.addView(TextView(this).apply {
            text = "\nTuning (live badalta hai)"
            textSize = 18f
        })
        slider(root, "Patti ki jagah (X, knife ke upar)", "x", 50, 0, 100, "%")
        slider(root, "Patti ki chaudai", "width", 4, 1, 20, "%")
        slider(root, "Patti ka upar ka kinara", "top", 12, 0, 100, "%")
        slider(root, "Patti ka neeche ka kinara (knife ke upar tak)", "bottom", 66, 0, 100, "%")
        slider(root, "Sensitivity (kam = jaldi tap)", "min", 40, 5, 400, " px")
        slider(root, "Bomb sensitivity (kala pixel, kam = jaldi bomb pakde)", "bomb", 60, 10, 400, " px")
        slider(root, "Do tap ke beech ka gap", "cooldown", 700, 200, 2000, " ms")
        slider(root, "Tap ki height (screen par)", "tapy", 85, 10, 95, "%")
    }

    private fun slider(
        root: LinearLayout, label: String, key: String,
        def: Int, min: Int, max: Int, unit: String
    ) {
        val tv = TextView(this)
        val sb = SeekBar(this)
        sb.max = max - min
        val cur = prefs.getInt(key, def)
        sb.progress = cur - min
        tv.text = "$label: $cur$unit"
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                val v = p + min
                tv.text = "$label: $v$unit"
                prefs.edit().putInt(key, v).apply()
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        root.addView(tv)
        root.addView(sb)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (TapService.instance != null) {
            "Accessibility: CHALU hai"
        } else {
            "Accessibility: band hai (button 1 dabao)"
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqCapture && resultCode == RESULT_OK && data != null) {
            val i = Intent(this, BotService::class.java)
                .putExtra("code", resultCode)
                .putExtra("data", data)
            startForegroundService(i)
            Toast.makeText(this, "Bot chalu. Ab game kholo.", Toast.LENGTH_LONG).show()
        }
    }
}
