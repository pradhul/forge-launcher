package com.sduduzog.slimlauncher.icons

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.roundToInt

/**
 * Magenta arbitrary-image-stylization-v1-256 (int8 TFLite).
 * GPU delegate first, CPU threads if the delegate cannot be created.
 */
class StyleTransferEngine(context: Context) : AutoCloseable {

    private val delegates = mutableListOf<GpuDelegate>()
    private val predict: Interpreter
    private val transform: Interpreter
    private val styleSize: Int
    private val contentSize: Int

    init {
        val predictModel = loadModel(context, PREDICT_ASSET)
        val transformModel = loadModel(context, TRANSFORM_ASSET)
        predict = createInterpreter(predictModel)
        transform = createInterpreter(transformModel)
        styleSize = predict.getInputTensor(0).shape()[1]
        contentSize = contentImageSize()
    }

    private fun createInterpreter(model: MappedByteBuffer): Interpreter {
        try {
            CompatibilityList().use { compat ->
                if (compat.isDelegateSupportedOnThisDevice) {
                    val delegate = GpuDelegate(compat.bestOptionsForThisDevice)
                    try {
                        model.rewind()
                        val interpreter = Interpreter(
                            model,
                            Interpreter.Options().addDelegate(delegate)
                        )
                        delegates.add(delegate)
                        return interpreter
                    } catch (error: Exception) {
                        delegate.close()
                        Log.w(TAG, "GPU interpreter failed, using CPU", error)
                    }
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "GPU delegate unavailable, using CPU", error)
        }
        model.rewind()
        return Interpreter(model, Interpreter.Options().setNumThreads(CPU_THREADS))
    }

    private fun contentImageSize(): Int {
        for (index in 0 until transform.inputTensorCount) {
            val shape = transform.getInputTensor(index).shape()
            if (isImageShape(shape)) return shape[1]
        }
        return transform.getInputTensor(0).shape()[1]
    }

    @Synchronized
    fun predictStyle(style: Bitmap): FloatArray {
        val inTensor = predict.getInputTensor(0)
        val outTensor = predict.getOutputTensor(0)
        val input = bitmapToBuffer(style, inTensor)
        val output = emptyBuffer(outTensor)
        predict.run(input, output)
        output.rewind()
        return bufferToFloats(output, outTensor)
    }

    @Synchronized
    fun transform(content: Bitmap, styleBottleneck: FloatArray): Bitmap {
        val inputs = arrayOfNulls<Any>(transform.inputTensorCount)
        for (index in 0 until transform.inputTensorCount) {
            val tensor = transform.getInputTensor(index)
            inputs[index] = if (isImageShape(tensor.shape())) {
                bitmapToBuffer(content, tensor)
            } else {
                floatsToBuffer(styleBottleneck, tensor)
            }
        }
        val outTensor = transform.getOutputTensor(0)
        val output = emptyBuffer(outTensor)
        transform.runForMultipleInputsOutputs(inputs, mapOf(0 to output))
        output.rewind()
        return bufferToBitmap(output, outTensor)
    }

    override fun close() {
        predict.close()
        transform.close()
        delegates.forEach { it.close() }
        delegates.clear()
    }

    private fun bitmapToBuffer(bitmap: Bitmap, tensor: Tensor): ByteBuffer {
        val size = tensor.shape()[1]
        val scaled = if (bitmap.width == size && bitmap.height == size) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, size, size, true)
        }
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        val buffer = emptyBuffer(tensor)
        val scale = tensor.quantizationParams().scale
        val zeroPoint = tensor.quantizationParams().zeroPoint
        when (tensor.dataType()) {
            DataType.UINT8, DataType.INT8 -> {
                for (color in pixels) {
                    buffer.put(quantize(((color ushr 16) and 0xFF) / 255f, scale, zeroPoint))
                    buffer.put(quantize(((color ushr 8) and 0xFF) / 255f, scale, zeroPoint))
                    buffer.put(quantize((color and 0xFF) / 255f, scale, zeroPoint))
                }
            }
            else -> {
                for (color in pixels) {
                    buffer.putFloat(((color ushr 16) and 0xFF) / 255f)
                    buffer.putFloat(((color ushr 8) and 0xFF) / 255f)
                    buffer.putFloat((color and 0xFF) / 255f)
                }
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun floatsToBuffer(values: FloatArray, tensor: Tensor): ByteBuffer {
        val buffer = emptyBuffer(tensor)
        val count = tensor.numElements().coerceAtMost(values.size)
        val scale = tensor.quantizationParams().scale
        val zeroPoint = tensor.quantizationParams().zeroPoint
        when (tensor.dataType()) {
            DataType.UINT8, DataType.INT8 -> {
                for (i in 0 until count) {
                    buffer.put(quantize(values[i], scale, zeroPoint))
                }
            }
            else -> {
                for (i in 0 until count) buffer.putFloat(values[i])
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun bufferToFloats(buffer: ByteBuffer, tensor: Tensor): FloatArray {
        val values = FloatArray(tensor.numElements())
        val scale = tensor.quantizationParams().scale
        val zeroPoint = tensor.quantizationParams().zeroPoint
        when (tensor.dataType()) {
            DataType.UINT8, DataType.INT8 -> {
                for (i in values.indices) {
                    values[i] = dequantize(buffer.get().toInt() and 0xFF, scale, zeroPoint)
                }
            }
            else -> {
                for (i in values.indices) values[i] = buffer.getFloat()
            }
        }
        return values
    }

    private fun bufferToBitmap(buffer: ByteBuffer, tensor: Tensor): Bitmap {
        val shape = tensor.shape()
        val height = if (shape.size >= 3) shape[1] else contentSize
        val width = if (shape.size >= 3) shape[2] else contentSize
        val scale = tensor.quantizationParams().scale
        val zeroPoint = tensor.quantizationParams().zeroPoint
        val pixels = IntArray(width * height)
        var i = 0
        repeat(height * width) {
            val r: Float
            val g: Float
            val b: Float
            if (tensor.dataType() == DataType.UINT8 || tensor.dataType() == DataType.INT8) {
                r = dequantize(buffer.get().toInt() and 0xFF, scale, zeroPoint)
                g = dequantize(buffer.get().toInt() and 0xFF, scale, zeroPoint)
                b = dequantize(buffer.get().toInt() and 0xFF, scale, zeroPoint)
            } else {
                r = buffer.getFloat()
                g = buffer.getFloat()
                b = buffer.getFloat()
            }
            pixels[i++] = (0xFF shl 24) or
                (toByte(r) shl 16) or
                (toByte(g) shl 8) or
                toByte(b)
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    companion object {
        private const val TAG = "StyleTransfer"
        private const val PREDICT_ASSET = "style_transfer/style_predict_int8.tflite"
        private const val TRANSFORM_ASSET = "style_transfer/style_transform_int8.tflite"
        private const val CPU_THREADS = 4

        fun loadModel(context: Context, assetPath: String): MappedByteBuffer {
            context.assets.openFd(assetPath).use { fd ->
                FileInputStream(fd.fileDescriptor).use { input ->
                    return input.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fd.startOffset,
                        fd.declaredLength
                    )
                }
            }
        }

        private fun isImageShape(shape: IntArray): Boolean {
            return shape.size >= 3 && shape[1] > 8
        }

        private fun emptyBuffer(tensor: Tensor): ByteBuffer {
            return ByteBuffer.allocateDirect(tensor.numBytes()).order(ByteOrder.nativeOrder())
        }

        private fun quantize(value: Float, scale: Float, zeroPoint: Int): Byte {
            val q = if (scale > 0f) {
                (value / scale + zeroPoint).roundToInt()
            } else {
                (value * 255f).roundToInt()
            }
            return q.coerceIn(0, 255).toByte()
        }

        private fun dequantize(quantized: Int, scale: Float, zeroPoint: Int): Float {
            return if (scale > 0f) (quantized - zeroPoint) * scale else quantized / 255f
        }

        private fun toByte(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).toInt()
    }
}
