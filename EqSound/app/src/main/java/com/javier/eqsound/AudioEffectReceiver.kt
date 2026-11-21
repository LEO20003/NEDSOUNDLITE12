package com.javier.eqsound

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

/** Spotify, YouTube Music y otras apps avisan aquí cuando empiezan a sonar. */
class AudioEffectReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        EffectsHub.init(c)
        val s = i.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        if (s < 0) return
        when (i.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> EffectsHub.attach(s)
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> EffectsHub.release(s)
        }
    }
}
