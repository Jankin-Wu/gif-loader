package com.jankinwu.composeApp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jankinwu.component.gif.GifPainter

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GifLoader",
    ) {
//        App()
        Box(Modifier.fillMaxSize()) {
            GifPainter("image/cheers.gif")
        }
    }
}