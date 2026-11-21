package com.javier.eqsound

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null
    private var player: MediaPlayer? = null
    private var on = true
    private var bassV = 500; private var virtV = 300; private var loudV = 300
    private val bands = ArrayList<SeekBar>()
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private val ACC = Color.parseColor("#E8325A")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
            setBackgroundColor(Color.parseColor("#111111"))
        }
        setContentView(ScrollView(this).apply { addView(root) })
        text("EqSound", 26f, ACC)
        status = text("", 13f, Color.GRAY)

        root.addView(Switch(this).apply {
            text = "Efectos activados"; setTextColor(Color.WHITE); isChecked = true
            setOnCheckedChangeListener { _, v -> on = v; applyAll() }
        })
        root.addView(Button(this).apply {
            text = "Reproducir una canción"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "audio/*"
                }, 1)
            }
        })
        attach(0) // sesión global: afecta al audio de otras apps (según el teléfono)
        buildUi()
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        val uri = data?.data ?: return
        player?.release()
        player = MediaPlayer().apply { setDataSource(this@MainActivity, uri); prepare(); isLooping = true; start() }
        attach(player!!.audioSessionId) // sesión propia: siempre funciona
        applyAll()
    }

    private fun attach(session: Int) {
        eq?.release(); bass?.release(); virt?.release(); loud?.release()
        try {
            eq = Equalizer(0, session).apply { enabled = true }
            bass = BassBoost(0, session).apply { enabled = true }
            virt = Virtualizer(0, session).apply { enabled = true }
            loud = LoudnessEnhancer(session).apply { enabled = true }
            status.text = if (session == 0) "Modo global activo" else "Modo canción activo"
        } catch (e: Exception) {
            status.text = "Este teléfono no permite efectos globales. Usa «Reproducir una canción»."
        }
        applyAll()
    }

    private fun buildUi() {
        val e = eq ?: return
        val r = e.bandLevelRange
        val lo = r[0].toInt(); val span = r[1] - r[0]
        text("Ecualizador", 18f, Color.WHITE)
        // presets del sistema
        val names = (0 until e.numberOfPresets).map { e.getPresetName(it.toShort()) }
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    eq?.usePreset(pos.toShort())
                    bands.forEachIndexed { i, s -> s.progress = eq!!.getBandLevel(i.toShort()) - lo }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })
        for (i in 0 until e.numberOfBands) {
            val hz = e.getCenterFreq(i.toShort()) / 1000
            text(if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz", 12f, Color.GRAY)
            val s = SeekBar(this).apply {
                max = span.toInt(); progress = e.getBandLevel(i.toShort()) - lo
                progressTintList = android.content.res.ColorStateList.valueOf(ACC)
                thumbTintList = android.content.res.ColorStateList.valueOf(ACC)
                setOnSeekBarChangeListener(listener { eq?.setBandLevel(i.toShort(), (it + lo).toShort()) })
            }
            bands.add(s); root.addView(s)
        }
        text("Efectos", 18f, Color.WHITE)
        slider("Refuerzo de graves", 1000, bassV) { bassV = it; applyAll() }
        slider("Sonido envolvente", 1000, virtV) { virtV = it; applyAll() }
        slider("Refuerzo dinámico", 2000, loudV) { loudV = it; applyAll() }
    }

    private fun applyAll() {
        try {
            eq?.enabled = on; bass?.enabled = on; virt?.enabled = on; loud?.enabled = on
            if (bass?.strengthSupported == true) bass?.setStrength(bassV.toShort())
            if (virt?.strengthSupported == true) virt?.setStrength(virtV.toShort())
            loud?.setTargetGain(loudV)
        } catch (_: Exception) {}
    }

    private fun slider(label: String, max: Int, init: Int, f: (Int) -> Unit) {
        text(label, 13f, Color.GRAY)
        root.addView(SeekBar(this).apply {
            this.max = max; progress = init
            progressTintList = android.content.res.ColorStateList.valueOf(ACC)
            thumbTintList = android.content.res.ColorStateList.valueOf(ACC)
            setOnSeekBarChangeListener(listener(f))
        })
    }

    private fun listener(f: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { if (user) f(p) }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun text(t: String, size: Float, color: Int) = TextView(this).apply {
        this.text = t; textSize = size; setTextColor(color); setPadding(0, 28, 0, 6)
        root.addView(this)
    }

    override fun onDestroy() {
        super.onDestroy(); player?.release()
        eq?.release(); bass?.release(); virt?.release(); loud?.release()
    }
}
