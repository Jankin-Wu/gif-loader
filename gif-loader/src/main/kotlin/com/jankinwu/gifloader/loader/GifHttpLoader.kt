package com.jankinwu.component.gif.loader

import com.jankinwu.component.gif.*
import java.net.URL
import javax.imageio.ImageIO

class GifHttpLoader : GifLoader {

    override fun load(data: DataToBeLoaded): AnimatedGif {
        val url = data.data as? String
        return URL(url).openStream().use { urlStream ->
            ImageIO.createImageInputStream(urlStream).use {
                return try {
                    AnimatedGif(it)
                } catch (e: Exception) {
                    GifLoadManager.createErrorPainter(data)
                }
            }
        }
    }

    override fun canLoad(data: DataToBeLoaded): Boolean {
        val url = data.data as? String ?: return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    override fun canLoadErrorImage(data: DataToBeLoaded): Boolean {
        val url = data.errorImagePath as? String ?: return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    override fun loadErrorImage(data: DataToBeLoaded): AnimatedGif? {
        val url = data.errorImagePath as? String
        return URL(url).openStream().use { urlStream ->
            ImageIO.createImageInputStream(urlStream).use {
                return try {
                    AnimatedGif(it)
                } catch (e: Exception) {
                    EmptyAnimatedGifProvider.instance
                }
            }
        }
    }
}