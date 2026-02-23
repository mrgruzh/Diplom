package com.example.diplom.ui.field

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoskCommandRecognizer(
    context: Context,
    private val modelAssetPath: String = "model-ru"
) : RecognitionListener {

    enum class EngineState {
        PREPARING,
        READY,
        LISTENING,
        ERROR
    }

    enum class ListenMode {
        COMMAND,
        FREE
    }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var recognizer: Recognizer? = null
    private var currentMode: ListenMode = ListenMode.COMMAND

    private var stateCb: ((EngineState, String?) -> Unit)? = null
    private var partialCb: ((String) -> Unit)? = null
    private var textCb: ((String) -> Unit)? = null

    private var lastDeliveredText: String = ""

    init {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
    }

    fun prepare(onState: (EngineState, String?) -> Unit) {
        stateCb = onState
        postState(EngineState.PREPARING, null)

        StorageService.unpack(
            appContext,
            modelAssetPath,
            "vosk-model",
            { unpacked ->
                model = unpacked
                postState(EngineState.READY, null)
            },
            { e ->
                postState(
                    EngineState.ERROR,
                    "Не удалось загрузить модель Vosk. Положите модель в app/src/main/assets/$modelAssetPath"
                )
            }
        )
    }

    fun start(
        mode: ListenMode,
        onPartialText: (String) -> Unit,
        onUtteranceText: (String) -> Unit
    ): Boolean {
        partialCb = onPartialText
        textCb = onUtteranceText

        if (model == null) {
            postState(EngineState.ERROR, "Модель Vosk еще не загружена")
            return false
        }

        currentMode = mode
        return restartService()
    }

    fun switchMode(mode: ListenMode) {
        if (mode == currentMode) return
        if (speechService == null) {
            currentMode = mode
            return
        }
        currentMode = mode
        restartService()
    }

    fun stop() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        recognizer?.close()
        recognizer = null
        postState(EngineState.READY, null)
    }

    fun shutdown() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
    }

    override fun onPartialResult(hypothesis: String?) {
        val text = parseText(hypothesis, key = "partial")
        if (text.isBlank()) return
        main.post { partialCb?.invoke(text) }
    }

    override fun onResult(hypothesis: String?) {
        val text = parseText(hypothesis, key = "text")
        deliverUtterance(text)
    }

    override fun onFinalResult(hypothesis: String?) {
        val text = parseText(hypothesis, key = "text")
        deliverUtterance(text)
    }

    override fun onError(e: Exception?) {
        postState(EngineState.ERROR, e?.message ?: "Ошибка распознавания")
    }

    override fun onTimeout() {
        postState(EngineState.READY, null)
    }

    private fun restartService(): Boolean {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        recognizer?.close()
        recognizer = null

        val m = model ?: run {
            postState(EngineState.ERROR, "Модель Vosk не загружена")
            return false
        }

        try {
            recognizer = when (currentMode) {
                ListenMode.COMMAND -> Recognizer(m, SAMPLE_RATE.toFloat(), VoiceFormInterpreter.commandGrammarJson())
                ListenMode.FREE -> Recognizer(m, SAMPLE_RATE.toFloat())
            }

            speechService = SpeechService(recognizer!!, SAMPLE_RATE.toFloat())
            speechService?.startListening(this)
            postState(EngineState.LISTENING, null)
            return true
        } catch (e: IOException) {
            postState(EngineState.ERROR, e.message ?: "Ошибка инициализации распознавания")
            return false
        }
    }

    private fun deliverUtterance(text: String) {
        if (text.isBlank()) return
        if (text == lastDeliveredText) return
        lastDeliveredText = text
        main.post { textCb?.invoke(text) }
    }

    private fun parseText(raw: String?, key: String): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            JSONObject(raw).optString(key, "").trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun postState(state: EngineState, message: String?) {
        main.post { stateCb?.invoke(state, message) }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
    }
}
