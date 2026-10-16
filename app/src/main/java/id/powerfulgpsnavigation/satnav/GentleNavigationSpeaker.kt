package id.powerfulgpsnavigation.satnav

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class GentleNavigationSpeaker(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("NavigationSettings", Context.MODE_PRIVATE)

    private var isMuted = sharedPreferences.getBoolean("is_muted", false)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Coba pasang Bahasa Indonesia
            var result = tts.setLanguage(Locale("id", "ID"))

            // JIKA HP tidak punya data Bahasa Indonesia, fallback ke Bahasa Default Sistem HP
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TTS", "Bahasa Indonesia tidak didukung/belum diunduh, menggunakan bahasa sistem.")
                result = tts.setLanguage(Locale.getDefault())
            }

            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                tts.setSpeechRate(1.05f)
                Log.d("TTS", "TextToSpeech Sukses Diinisialisasi!")
            }
        } else {
            Log.e("TTS", "Gagal inisialisasi TextToSpeech. Status: $status")
        }
    }

    fun playGentle(text: String) {
        if (text.isEmpty()) return
        if (!isInitialized) {
            Log.w("TTS", "Gagal memutar, TTS belum siap atau belum selesai inisialisasi.")
            return
        }
        if (isMuted) {
            Log.d("TTS", "Suara dilewati karena status sedang Muted.")
            return
        }

        requestAudioFocus()

        val utteranceId = "MapLibre_${System.currentTimeMillis()}"

        // SOLUSI: Bungkus parameter audio ke dalam Bundle agar dipaksa keluar ke speaker media
        val ttsParams = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        // Jalankan pemutar suara dengan Bundle parameter yang valid
        tts.speak(text, TextToSpeech.QUEUE_ADD, ttsParams, utteranceId)

        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TTS", "Mulai berbicara: $text")
            }
            override fun onDone(utteranceId: String?) {
                abandonAudioFocus()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                abandonAudioFocus()
            }
        })
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        sharedPreferences.edit().putBoolean("is_muted", isMuted).apply()
        if (isMuted) {
            stopAll()
        }
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    fun stopAll() {
        tts.stop()
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            focusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
