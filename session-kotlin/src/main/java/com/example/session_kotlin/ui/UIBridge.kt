package com.example.session_kotlin.ui

import androidx.compose.ui.platform.ComposeView

object UIBridge {
    @JvmStatic
    fun setMainContent(composeView: ComposeView, vm: ScoreViewModel, onQuit: Runnable) {
        composeView.setContent { AppRoot(vm = vm, onQuit = { onQuit.run() }) }
    }
}
