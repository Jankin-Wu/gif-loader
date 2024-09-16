package com.jankinwu.component.gif.loader

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ClassLoaderResourceLoader
import com.jankinwu.component.gif.*
import javax.imageio.ImageIO

class GifResourceLoader : GifLoader {

//    @Composable
    override fun load(data: DataToBeLoaded): AnimatedGif {
        val contextClassLoader = Thread.currentThread().contextClassLoader
        val resourceAsStream = contextClassLoader.getResourceAsStream(data.data as? String ?)
    if (resourceAsStream == null) {
        println("资源未找到: ${data.data}")
        return GifLoadManager.createErrorPainter(rememberDataToBeLoaded(data))
    }
        return resourceAsStream.use { inputStream ->
            ImageIO.createImageInputStream(inputStream).use {
                return try {
                    AnimatedGif(it)
                } catch (e: Exception) {
                    GifLoadManager.createErrorPainter(rememberDataToBeLoaded(data))
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun canLoad(data: DataToBeLoaded): Boolean {
        val url = data.data as? String
        if (url.isNullOrEmpty()) return false
        //Check reference [ClassLoaderResourceLoader]
        val contextClassLoader = Thread.currentThread().contextClassLoader ?: return false
        val resource = try {
            contextClassLoader.getResourceAsStream(url)
                ?: (::ClassLoaderResourceLoader.javaClass).getResourceAsStream(url)
                ?: return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        resource.close()
        return true
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun canLoadErrorImage(data: DataToBeLoaded): Boolean {
        val url = data.errorImagePath as? String
        if (url.isNullOrEmpty()) return false
        //Check reference [ClassLoaderResourceLoader]
        val contextClassLoader = Thread.currentThread().contextClassLoader ?: return false
        val resource = try {
            contextClassLoader.getResourceAsStream(url)
                ?: (::ClassLoaderResourceLoader.javaClass).getResourceAsStream(url)
                ?: return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        resource.close()
        return true
    }

    override fun loadErrorImage(data: DataToBeLoaded): AnimatedGif? {
        val contextClassLoader = Thread.currentThread().contextClassLoader
        val resourceAsStream = contextClassLoader.getResourceAsStream(data.errorImagePath as? String ?)
        if (resourceAsStream == null) {
            println("异常图片未找到: ${data.data}")
            return EmptyAnimatedGifProvider.instance
        }
        return resourceAsStream.use { inputStream ->
            ImageIO.createImageInputStream(inputStream).use {
                return try {
                    AnimatedGif(it)
                } catch (e: Exception) {
                    EmptyAnimatedGifProvider.instance
                }
            }
        }
    }
}