package com.jankinwu.component.gif

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 异常时默认使用全局设定的异常图片
 */
@Composable
fun GifPainter(imagePath: String, modifier: Modifier = Modifier) {
    AnimatedGif(GifLoadManager.load(imagePath), modifier)
}

/**
 * 可自定义异常图片
 */
@Composable
fun GifPainter(imagePath: String, errorImagePath: String, modifier: Modifier = Modifier) {
    AnimatedGif(GifLoadManager.load(imagePath, errorImagePath), modifier)
}