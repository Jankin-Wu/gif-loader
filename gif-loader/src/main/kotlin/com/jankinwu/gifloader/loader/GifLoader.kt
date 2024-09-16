package com.jankinwu.component.gif.loader

import com.jankinwu.component.gif.AnimatedGif
import com.jankinwu.component.gif.DataToBeLoaded


interface GifLoader {

    /**
     * Complete process with load image
     */
//    @Composable
    fun load(data: DataToBeLoaded): AnimatedGif?

    /**
     * Return false means it cannot be processed
     */
    fun canLoad(data: DataToBeLoaded): Boolean

    fun canLoadErrorImage(data: DataToBeLoaded): Boolean

    fun loadErrorImage(data: DataToBeLoaded): AnimatedGif?
}