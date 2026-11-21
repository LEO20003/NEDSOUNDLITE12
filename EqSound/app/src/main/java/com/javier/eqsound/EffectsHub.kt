package com.javier.eqsound

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

/** Guarda los ajustes y los aplica a cada app de música (cada una tiene su "sesión" de audio). */
object EffectsHub {
    class Fx(val eq: Equalizer, val bass: BassBoost, val virt: Virtualizer, val loud: LoudnessEnhancer)

    private val map = HashMap<Int, Fx>()
    private lateinit var prefs: SharedPreferences
    const val NONE = Int.MIN_VALUE

    fun init(c: Context) { if (!::prefs.isInitialized) prefs = c.applicationContext.getSharedPreferences("eq", 0) }

    var on: Boolean
        get() = prefs.getBoolean("on", true)
        set(v) { prefs.edit().putBoolean("on", v).apply(); applyAll() }
    var bass: Int
        get() = prefs.getInt("bass", 500)
        set(v) { prefs.edit().putInt("bass", v).apply(); applyAll() }
    var virt: Int
        get() = prefs.getInt("virt", 300)
        set(v) { prefs.edit().putInt("virt", v).apply(); applyAll() }
    var loud: Int
        get() = prefs.getInt("loud", 300)
        set(v) { prefs.edit().putInt("loud", v).apply(); applyAll() }

    fun band(i: Int) = prefs.getInt("b$i", NONE)
    fun setBand(i: Int, level: Int) { prefs.edit().putInt("b$i", level).apply(); applyAll() }

    fun sessions() = map.size
    fun any(): Equalizer? = map.values.firstOrNull()?.eq

    @Synchronized fun attach(session: Int): Boolean {
        release(session)
        return try {
            val fx = Fx(
                Equalizer(0, session), BassBoost(0, session),
                Virtualizer(0, session), LoudnessEnhancer(session)
            )
            map[session] = fx
            apply(fx)
            true
        } catch (e: Exception) { false }
    }

    @Synchronized fun release(session: Int) {
        map.remove(session)?.let {
            try { it.eq.release(); it.bass.release(); it.virt.release(); it.loud.release() } catch (_: Exception) {}
        }
    }

    @Synchronized fun applyAll() { map.values.forEach { apply(it) } }

    private fun apply(f: Fx) {
        try {
            val e = f.eq; val r = e.bandLevelRange
            e.enabled = on; f.bass.enabled = on; f.virt.enabled = on; f.loud.enabled = on
            for (i in 0 until e.numberOfBands) {
                val v = band(i)
                if (v != NONE) e.setBandLevel(i.toShort(), v.coerceIn(r[0].toInt(), r[1].toInt()).toShort())
            }
            if (f.bass.strengthSupported) f.bass.setStrength(bass.toShort())
            if (f.virt.strengthSupported) f.virt.setStrength(virt.toShort())
            f.loud.setTargetGain(loud)
        } catch (_: Exception) {}
    }
}
