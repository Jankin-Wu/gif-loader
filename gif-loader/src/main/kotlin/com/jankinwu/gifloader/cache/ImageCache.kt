package com.jankinwu.component.gif.cache

import com.jankinwu.component.gif.AnimatedGif

interface ImageCache {

    /**
     * Save image Cache
     * [url]Unique key
     * [t]The cache
     */
    fun saveCache(url: String, t: AnimatedGif)

    /**
     * Get image Cache
     * [url]Unique key
     */
    fun getCache(url: String): AnimatedGif?
}