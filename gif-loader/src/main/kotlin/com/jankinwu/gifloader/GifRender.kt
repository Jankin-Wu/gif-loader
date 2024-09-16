package com.jankinwu.component.gif

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

@Composable
fun AnimatedGif(gif: AnimatedGif?, modifier: Modifier = Modifier) {
    if (gif == null || gif.frames.isEmpty()) return

    val transition = rememberInfiniteTransition()
    val frameIndex by transition.animateValue(
        initialValue = 0,
        targetValue = gif.frames.lastIndex,
        Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 0
                for ((index, frame) in gif.frames.withIndex()) {
                    val frameDuration = frame.graphicControlExtension!!.delayTime
                    index at durationMillis
                    durationMillis += frameDuration
                }
            }
        )
    )

    val gifSize = gif.logicalScreenDescriptor.let { Size(it.width.toFloat(), it.height.toFloat()) }

    // Use the actual size of the GIF image and apply the passed modifier
    Canvas(modifier.size(gifSize.width.dp, gifSize.height.dp)) {
        scale(size.width / gifSize.width, size.height / gifSize.height, Offset.Zero) {
            for (i in gif.frames.indices) {
                val frame = gif.frames[i]
                val descriptor = frame.imageDescriptor

                if (i > frameIndex) continue
                if (i < frameIndex) {
                    val dispose = frame.graphicControlExtension!!.disposalMethod
                    if (dispose != AnimatedGif.DisposalMethod.DO_NOT_DISPOSE) {
                        continue
                    }
                }

                translate(descriptor.imageLeftPosition.toFloat(), descriptor.imageTopPosition.toFloat()) {
                    drawImage(frame.image)
                }
            }
        }
    }
}

class AnimatedGif {
    val logicalScreenDescriptor: LogicalScreenDescriptor
    val globalColorTable: GlobalColorTable?
    val frames: List<ImageFrame>
    val byteSize: Long

    constructor(
        logicalScreenDescriptor: LogicalScreenDescriptor,
        globalColorTable: GlobalColorTable?,
        frames: List<ImageFrame>,
        byteSize: Long
    ) {
        this.logicalScreenDescriptor = logicalScreenDescriptor
        this.globalColorTable = globalColorTable
        this.frames = frames
        this.byteSize = byteSize
    }

    constructor(stream: ImageInputStream) {
        val readers = ImageIO.getImageReaders(stream)
        if (!readers.hasNext()) throw RuntimeException("No image reader found")

        val reader = readers.next()
        reader.input = stream
        if (reader.streamMetadata == null) {
            throw RuntimeException("No image stream metadata found")
        }
        val headTree = reader.streamMetadata.getAsTree("javax_imageio_gif_stream_1.0")
        var logicalScreenDescriptor: LogicalScreenDescriptor? = null
        var globalColorTable: GlobalColorTable? = null

        for (node in headTree.childrenSequence()) {
            val attr = node.attributes
            when (node.nodeName) {
                "LogicalScreenDescriptor" -> {
                    logicalScreenDescriptor = LogicalScreenDescriptor(
                        attr.getNamedItem("logicalScreenWidth").intValue,
                        attr.getNamedItem("logicalScreenHeight").intValue,
                        attr.getNamedItem("colorResolution").intValue,
                        attr.getNamedItem("pixelAspectRatio").intValue
                    )
                }
                "GlobalColorTable" -> {
                    val size = attr.getNamedItem("sizeOfGlobalColorTable").intValue
                    globalColorTable = GlobalColorTable(
                        attr.getNamedItem("backgroundColorIndex").intValue,
                        node.childNodes.loadColors(size)
                    )
                }
            }
        }

        this.logicalScreenDescriptor = logicalScreenDescriptor!!
        this.globalColorTable = globalColorTable

        val numImages = reader.getNumImages(true)
        var totalByteSize = 0L
        frames = List(numImages) { imageIndex ->
            val image = reader.read(imageIndex)
            val imd = reader.getImageMetadata(imageIndex)
            val tree = imd.getAsTree("javax_imageio_gif_image_1.0")

            var imageDescriptor: ImageDescriptor? = null
            var localColorTable: List<Color>? = null
            var graphicControlExtension: GraphicControlExtension? = null

            for (node in tree.childrenSequence()) {
                val attr = node.attributes
                when (node.nodeName) {
                    "ImageDescriptor" -> {
                        imageDescriptor = ImageDescriptor(
                            attr.getNamedItem("imageLeftPosition").intValue,
                            attr.getNamedItem("imageTopPosition").intValue,
                            attr.getNamedItem("imageWidth").intValue,
                            attr.getNamedItem("imageHeight").intValue,
                            attr.getNamedItem("interlaceFlag").booleanValue
                        )
                    }
                    "LocalColorTable" -> {
                        val size = attr.getNamedItem("sizeOfLocalColorTable").intValue
                        localColorTable = node.childNodes.loadColors(size)
                    }
                    "GraphicControlExtension" -> {
                        graphicControlExtension = GraphicControlExtension(
                            DisposalMethod.find(attr.getNamedItem("disposalMethod")?.nodeValue),
                            attr.getNamedItem("userInputFlag").booleanValue,
                            attr.getNamedItem("transparentColorFlag").booleanValue,
                            attr.getNamedItem("delayTime").intValue * 10,
                            attr.getNamedItem("transparentColorIndex").intValue
                        )
                    }
                }
            }

            val frameImage = assetFromBufferedImage(image)
            totalByteSize += frameImage.byteCount
            ImageFrame(
                frameImage,
                imageDescriptor!!,
                localColorTable,
                graphicControlExtension
            )
        }
        this.byteSize = totalByteSize
    }

    class LogicalScreenDescriptor(
        val width: Int,
        val height: Int,
        val backgroundColorIndex: Int,
        val pixelAspectRatio: Int
    )

    class GlobalColorTable(
        val backgroundColorIndex: Int,
        val colors: List<Color>
    )

    class ImageFrame(
        val image: ImageBitmap,
        val imageDescriptor: ImageDescriptor,
        val localColorTable: List<Color>? = null,
        val graphicControlExtension: GraphicControlExtension? = null
    )

    class ImageDescriptor(
        val imageLeftPosition: Int,
        val imageTopPosition: Int,
        val imageHeight: Int,
        val imageWidth: Int,
        val isInterlaced: Boolean
    )

    enum class DisposalMethod {
        UNSPECIFIED,
        DO_NOT_DISPOSE,
        RESTORE_TO_BACKGROUND_COLOR,
        RESTORE_TO_PREVIOUS;

        companion object {
            fun find(text: String?): DisposalMethod {
                return when (text) {
                    "restoreToBackgroundColor" -> RESTORE_TO_BACKGROUND_COLOR
                    "doNotDispose" -> DO_NOT_DISPOSE
                    else -> UNSPECIFIED
                }
            }
        }
    }

    class GraphicControlExtension(
        val disposalMethod: DisposalMethod,
        val isUserInputFlag: Boolean,
        val isTransparentColorFlag: Boolean,
        val delayTime: Int,
        val transparentColorIndex: Int
    )

    companion object {

        private fun NodeList.asSequence(): Sequence<Node> {
            return (0 until length).asSequence().map { item(it) }
        }

        private fun Node.childrenSequence(): Sequence<Node> {
            return childNodes.asSequence()
        }

        private val Node?.intValue: Int get() = this?.nodeValue?.toInt() ?: 0

        private val Node?.booleanValue: Boolean get() = this?.nodeValue.toBoolean()

        private fun NodeList.loadColors(size: Int): List<Color> {
            val colors = Array<Color?>(size) { null }
            for (entry in asSequence()) {
                check(entry.nodeName == "ColorTableEntry")
                val index = entry.attributes.getNamedItem("index").intValue
                val red = entry.attributes.getNamedItem("red").intValue
                val green = entry.attributes.getNamedItem("green").intValue
                val blue = entry.attributes.getNamedItem("blue").intValue
                colors[index] = Color(red, green, blue)
            }
            return colors.map { it ?: Color.Unspecified /* Need to investigate */ }
        }

        private fun assetFromBufferedImage(image: BufferedImage): ImageBitmap {
            val output = ByteArrayOutputStream(image.width * image.height * 4 /* Calm down it's just a hint */)
            ImageIO.write(image, "PNG", output)
            return Image.makeFromEncoded(output.toByteArray()).asImageBitmap()
        }

        private val ImageBitmap.byteCount: Long
            get() = this.width * this.height * 4L // Assuming 4 bytes per pixel
    }
}