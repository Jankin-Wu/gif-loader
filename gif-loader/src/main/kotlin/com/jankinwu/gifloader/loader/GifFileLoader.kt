package com.jankinwu.gifloader.loader

import androidx.compose.runtime.remember
import com.jankinwu.component.gif.AnimatedGif
import com.jankinwu.component.gif.DataToBeLoaded
import com.jankinwu.component.gif.EmptyAnimatedGifProvider
import com.jankinwu.component.gif.GifLoadManager
import com.jankinwu.component.gif.loader.GifLoader
import java.io.File
import javax.imageio.ImageIO

/**
 * @description: GIF文件加载器
 * @author: Jankin Wu
 * @date: 2024-09-16 01:30
 **/
class GifFileLoader: GifLoader {
    override fun load(data: DataToBeLoaded): AnimatedGif? {
        val file = when (data.data) {
            is String -> remember { File(data.data) }
            is File -> data.data
            else -> return null
        }
        return try {
            AnimatedGif(ImageIO.createImageInputStream(file))
        } catch (e: Exception) {
            GifLoadManager.createErrorPainter(data)
        }
    }

    override fun canLoad(data: DataToBeLoaded): Boolean {
        val file = if (data.data is String)
            File(data.data)
        else if (data.data is File)
            data.data
        else
            return false
        if (file.exists() && file.isFile)
            return true
        return false
    }

    override fun canLoadErrorImage(data: DataToBeLoaded): Boolean {
        val file = File(data.errorImagePath)
        return file.exists() && file.isFile
    }

    override fun loadErrorImage(data: DataToBeLoaded): AnimatedGif? {
        val file = File(data.errorImagePath)
        return try {
            AnimatedGif(ImageIO.createImageInputStream(file))
        } catch (e: Exception) {
            EmptyAnimatedGifProvider.instance
        }
    }
}