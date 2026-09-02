package com.codebasetemplate.features.feature_language.ui.v2

import android.widget.TextView

internal class ApplyingLanguageMessageAnimator(
    private val textView: TextView,
    private val frameDurationMillis: Long = DEFAULT_FRAME_DURATION_MILLIS
) {
    private val messageSequence = ApplyingLanguageMessageSequence(textView.text)

    private val updateMessage = object : Runnable {
        override fun run() {
            textView.text = messageSequence.next()
            textView.postDelayed(this, frameDurationMillis)
        }
    }

    fun start() {
        stop()
        messageSequence.reset()
        updateMessage.run()
    }

    fun stop() {
        textView.removeCallbacks(updateMessage)
    }

    private companion object {
        const val DEFAULT_FRAME_DURATION_MILLIS = 400L
    }
}

internal class ApplyingLanguageMessageSequence(
    private val baseMessage: CharSequence
) {
    private var dotCount = FIRST_DOT_COUNT

    fun next(): CharSequence = buildString {
        append(baseMessage)
        repeat(dotCount) { append('.') }
        dotCount = dotCount % LAST_DOT_COUNT + FIRST_DOT_COUNT
    }

    fun reset() {
        dotCount = FIRST_DOT_COUNT
    }

    private companion object {
        const val FIRST_DOT_COUNT = 1
        const val LAST_DOT_COUNT = 3
    }
}
