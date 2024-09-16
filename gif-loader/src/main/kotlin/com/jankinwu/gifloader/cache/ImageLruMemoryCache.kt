package com.jankinwu.component.gif.cache

import com.jankinwu.component.gif.AnimatedGif


open class ImageLruMemoryCache(
    private val maxMemorySize: Long = getMemoryWithOnePercent()
) : ImageCache {
    //image lru cache
    private val cacheMap = LinkedHashMap<String, AnimatedGif>(35, 1f, true)

    //image cache byte size sum
    private var cacheSize: Long = 0

    @Synchronized
    override fun saveCache(url: String, t: AnimatedGif) {
        if (t.byteSize > maxMemorySize)
            return
        cacheMap[url] = t
        cacheSize += t.byteSize
        while (cacheSize > maxMemorySize && cacheMap.isNotEmpty()) {
            val byteArray = cacheMap.remove(cacheMap.keys.first())
            cacheSize -= byteArray?.byteSize ?: 0
        }
    }

    @Synchronized
    override fun getCache(url: String): AnimatedGif? {
        return cacheMap[url]
    }
}

//Gets one percent of the total memory
private fun getMemoryWithOnePercent(): Long {
    return maxOf(50L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 100)
}