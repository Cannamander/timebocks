package com.timebox.app.percy

data class PercyState(
    val dialogue: PercyDialogue = PercyDialogue.Idle,
    val isVisible: Boolean = false,
    val showSpeechBubble: Boolean = false
)

