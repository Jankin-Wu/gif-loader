package com.jankinwu.component.gif


import androidx.compose.runtime.Stable
import com.jankinwu.component.gif.AnimatedGif.LogicalScreenDescriptor
import com.jankinwu.component.gif.cache.ImageCache
import com.jankinwu.component.gif.cache.ImageLruMemoryCache
import com.jankinwu.component.gif.loader.GifHttpLoader
import com.jankinwu.component.gif.loader.GifLoader
import com.jankinwu.component.gif.loader.GifResourceLoader


object GifLoadManager {

    /**
     * The path of the picture displayed when an exception occurred while loading the picture
     */
    var defaultErrorImagePath = ""

    /**
     * Set is use the memory cache
     */
    var memoryCache: ImageCache = ImageLruMemoryCache()

    /**
     * Handler load image
     */
    var loadTheImage: MutableList<GifLoader> = mutableListOf(
        GifResourceLoader(),
        GifHttpLoader(),
    )

    /**
     * Load the image
     */
    fun load(data: DataToBeLoaded): AnimatedGif {
        val loader = loadTheImage.find { it.canLoad(data) }
        if (loader != null)
            return loader.load(data)
                ?: kotlin.run {
                    println("Load the gif image error: Exception loading URL, data=${data.data}")
                    createErrorPainter(data)
                }
        println("Load the gif image error: No suitable gifLoader found, data=${data.data}")
        return createErrorPainter(data)
    }

    fun createErrorPainter(data: DataToBeLoaded): AnimatedGif {
        val errorImagePath = data.errorImagePath
        if (errorImagePath.isBlank())
            return EmptyAnimatedGifProvider.instance
        val loader = loadTheImage.find { it.canLoadErrorImage(data) }
        if (loader != null) {
            return loader.loadErrorImage(data) ?: kotlin.run {
                println("Load the gif image error: Exception loading URL, data=${data.data}")
                EmptyAnimatedGifProvider.instance
            }
        }
        return EmptyAnimatedGifProvider.instance
    }

    fun load(imagePath: String): AnimatedGif? {
        return if (memoryCache.getCache(imagePath) != null) {
            memoryCache.getCache(imagePath)
        } else {
            val animatedGif = load(rememberDataToBeLoaded(imagePath))
            memoryCache.saveCache(imagePath, animatedGif)
            animatedGif
        }
    }

    fun load(imagePath: String, errorImagePath: String): AnimatedGif? {
        val key = imagePath + "_" + errorImagePath
        return if (memoryCache.getCache(key) != null) {
            memoryCache.getCache(key)
        } else {
            val dataToBeLoaded = rememberDataToBeLoaded(imagePath)
            dataToBeLoaded.errorImagePath = errorImagePath
            val animatedGif = load(dataToBeLoaded)
            memoryCache.saveCache(key, animatedGif)
            animatedGif
        }
    }
}

/**
 * user single instance
 */
object EmptyAnimatedGifProvider {
    val instance: AnimatedGif by lazy {
        AnimatedGif(
            logicalScreenDescriptor = LogicalScreenDescriptor(1, 1, 1, 1),
            globalColorTable = null,
            frames = emptyList(),
            byteSize = 0
        )
    }
}

@Stable
open class DataToBeLoaded(val data: Any) {
    /**
     * The path of the picture displayed when an exception occurred while loading the picture
     */
    var errorImagePath = GifLoadManager.defaultErrorImagePath
}

/**
 * Remember the DataToBeLoaded
 */
//@Composable
fun rememberDataToBeLoaded(data: Any) = DataToBeLoaded(data)
