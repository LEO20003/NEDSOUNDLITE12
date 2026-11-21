package com.javier.eqsound

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val ACC = Color.parseColor("#E8325A")
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private val bands = ArrayList<SeekBar>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        EffectsHub.init(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
            setBackgroundColor(Color.parseColor("#111111"))
        }
        setContentView(ScrollView(this).apply { addView(root) })
        text("EqSound", 26f, ACC)
        status = text("", 13f, Color.GRAY)
        text("Abre Spotify (o tu app de música) y pon una canción. Si la app lo permite, el ecualizador se activa solo.", 13f, Color.LTGRAY)

        root.addView(Switch(this).apply {
            text = "Efectos activados"; setTextColor(Color.WHITE); isChecked = EffectsHub.on
            setOnCheckedChangeListener { _, v -> EffectsHub.on = v }
        })
        root.addView(Button(this).apply {
            text = "Probar modo global (todo el teléfono)"
            setOnClickListener { EffectsHub.attach(0); refresh() }
        })
        // sesión global como respaldo, para poder leer las bandas del teléfono
        if (EffectsHub.any() == null) EffectsHub.attach(0)
        buildUi()
        refresh()
    }

    private fun refresh() {
        val n = EffectsHub.sessions()
        status.text = if (n > 0) "Efectos listos ($n sesión activa)" else "Este teléfono no permite efectos globales. Abre Spotify y reproduce algo."
    }

    private fun buildUi() {
        val e = EffectsHub.any() ?: return
        val r = e.bandLevelRange; val lo = r[0].toInt(); val span = r[1] - r[0]
        text("Ecualizador", 18f, Color.WHITE)
        val names = (0 until e.numberOfPresets).map { e.getPresetName(it.toShort()) }
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    val q = EffectsHub.any() ?: return
                    q.usePreset(pos.toShort())
                    bands.forEachIndexed { i, s ->
                        val lvl = q.getBandLevel(i.toShort()).toInt()
                        s.progress = lvl - lo; EffectsHub.setBand(i, lvl)
                    }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })
        for (i in 0 until e.numberOfBands) {
            val hz = e.getCenterFreq(i.toShort()) / 1000
            text(if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz", 12f, Color.GRAY)
            val saved = EffectsHub.band(i)
            val s = SeekBar(this).apply {
                max = span.toInt()
                progress = (if (saved == EffectsHub.NONE) e.getBandLevel(i.toShort()).toInt() else saved) - lo
                tint(this)
                setOnSeekBarChangeListener(listener { EffectsHub.setBand(i, it + lo) })
            }
            bands.add(s); root.addView(s)
        }
        text("Efectos", 18f, Color.WHITE)
        slider("Refuerzo de graves", 1000, EffectsHub.bass) { EffectsHub.bass = it }
        slider("Sonido envolvente", 1000, EffectsHub.virt) { EffectsHub.virt = it }
        slider("Refuerzo dinámico", 2000, EffectsHub.loud) { EffectsHub.loud = it }
    }

    private fun tint(s: SeekBar) {
        s.progressTintList = ColorStateList.valueOf(ACC); s.thumbTintList = ColorStateList.valueOf(ACC)
    }

    private fun slider(label: String, max: Int, init: Int, f: (Int) -> Unit) {
        text(label, 13f, Color.GRAY)
        root.addView(SeekBar(this).apply {
            this.max = max; progress = init; tint(this); setOnSeekBarChangeListener(listener(f))
        })
    }

    private fun listener(f: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { if (user) f(p) }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun text(t: String, size: Float, color: Int) = TextView(this).apply {
        this.text = t; textSize = size; setTextColor(color); setPadding(0, 28, 0, 6); root.addView(this)
    }
}
