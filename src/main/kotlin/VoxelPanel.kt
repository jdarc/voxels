import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.util.*
import javax.swing.JPanel
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class VoxelPanel : JPanel() {
    private val colorMap = makePalette(intArrayOf(0x0000FF, 0x00FF00, 0xFFFF00, 0xFFFFFF))
    private val heightMap = HeightMap()
    private val lastY: IntArray
    private val lastC: IntArray
    private val pixels: IntArray
    private val image: BufferedImage

    private var sx = 0.0
    private var sy = 0.0
    private var angle = 0.0

    override fun paintComponent(g: Graphics) = (g as Graphics2D).drawImage(image, null, 0, 0)

    fun render(x: Double, y: Double, aa: Double) {
        Arrays.fill(pixels, -0xfff001)
        Arrays.fill(lastY, height)
        Arrays.fill(lastC, -1)
        val x0 = x.toInt()
        val y0 = y.toInt()
        val a = x0 shr 8 and 255
        val b = y0 shr 8 and 255
        val u0 = x0 shr 16 and 0xff
        val u1 = u0 + 1 and 0xff
        val v0 = y0 shr 8 and 0xff00
        val v1 = v0 + 256 and 0xff00
        var h0 = heightMap[u0 + v0]
        var h2 = heightMap[u0 + v1]
        val h1 = heightMap[u1 + v0]
        val h3 = heightMap[u1 + v1]
        h0 = (h0 shl 8) + a * (h1 - h0)
        h2 = (h2 shl 8) + a * (h3 - h2)
        val h = (h0 shl 8) + b * (h2 - h0) shr 16
        var d = 0
        while (d < 200) {
            val fov = Math.PI / 4.0
            line(
                (x0 + d * 65536.0 * cos(aa - fov)).toInt(),
                (y0 + d * 65536.0 * sin(aa - fov)).toInt(),
                (x0 + d * 65536.0 * cos(aa + fov)).toInt(),
                (y0 + d * 65536.0 * sin(aa + fov)).toInt(),
                h - 30, 300 * 256 / (d + 1)
            )
            d += 1 + (d shr 6)
        }
    }

    private fun line(xa: Int, ya: Int, xb: Int, yb: Int, hy: Int, s: Int) {
        val alpha = 255.shl(24)
        var x0 = xa
        var y0 = ya
        val sx = (xb - x0) / width
        val sy = (yb - y0) / width
        for (i in 0 until width) {
            val u0 = x0 shr 16 and 0xff
            val v0 = y0 shr 8 and 0xff00
            var a = x0 shr 8 and 255
            var b = y0 shr 8 and 255
            val u1 = u0 + 1 and 0xff
            val v1 = v0 + 256 and 0xff00
            var h0 = heightMap[u0 + v0]
            var h2 = heightMap[u0 + v1]
            var h1 = heightMap[u1 + v0]
            var h3 = heightMap[u1 + v1]
            h0 = (h0 shl 8) + a * (h1 - h0)
            h2 = (h2 shl 8) + a * (h3 - h2)
            val h = (h0 shl 8) + b * (h2 - h0) shr 16
            h0 = heightMap.colorAt(u0 + v0)
            h2 = heightMap.colorAt(u0 + v1)
            h1 = heightMap.colorAt(u1 + v0)
            h3 = heightMap.colorAt(u1 + v1)
            h0 = (h0 shl 8) + a * (h1 - h0)
            h2 = (h2 shl 8) + a * (h3 - h2)
            val c = (h0 shl 8) + b * (h2 - h0)
            var y = ((h - hy) * s shr 11) + 100
            if (y < lastY[i].also { a = it }) {
                b = a * width + i
                if (lastC[i] == -1) lastC[i] = c
                val sc = (c - lastC[i]) / (a - y)
                var cc = lastC[i]
                val height = height - 1
                if (a > height) {
                    b -= (a - height) * width
                    cc += (a - height) * sc
                    a = height
                }
                if (y < 0) y = 0
                while (y < a) {
                    val fred = cc shr 16
                    val argb = colorMap[255 - h]
                    val red = fred * (argb shr 16 and 255) shr 8
                    val grn = fred * (argb shr 8 and 255) shr 8
                    val blu = fred * (argb and 255) shr 8
                    pixels[b] = alpha or red.shl(16) or grn.shl(8) or blu
                    cc += sc
                    b -= width
                    a--
                }
                lastY[i] = y
            }
            lastC[i] = c
            x0 += sx
            y0 += sy
        }
    }

    private fun makePalette(markers: IntArray): IntArray {
        val colors = IntArray(256)
        var eR = (0xFF0000 and markers[0] shr 16).toDouble()
        var eG = (0x00FF00 and markers[0] shr 8).toDouble()
        var eB = (0x0000FF and markers[0]).toDouble()
        val step = 256.0 / (markers.size - 1)
        for (j in 1 until markers.size) {
            var sR = eR
            var sG = eG
            var sB = eB
            eR = (0xFF0000 and markers[j] shr 16).toDouble()
            eG = (0x00FF00 and markers[j] shr 8).toDouble()
            eB = (0x0000FF and markers[j]).toDouble()
            val dr = (eR - sR) / step
            val dg = (eG - sG) / step
            val db = (eB - sB) / step
            var i = 0
            while (i < step) {
                val r = floor(sR).toInt().coerceIn(0, 255)
                val g = floor(sG).toInt().coerceIn(0, 255)
                val b = floor(sB).toInt().coerceIn(0, 255)
                colors[(floor((j - 1).toDouble()) * step + i).toInt()] = 255 shl 24 or (r shl 16) or (g shl 8) or b
                sR += dr
                sG += dg
                sB += db
                ++i
            }
        }
        return colors
    }

    init {
        size = Dimension(1280, 800)
        preferredSize = size

        heightMap.generate(20000)
        heightMap.smooth()
        heightMap.smooth()
        heightMap.colorize()

        lastY = IntArray(width)
        lastC = IntArray(width)

        image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE)
        pixels = (image.raster.dataBuffer as DataBufferInt).data

        Toolkit.getDefaultToolkit().systemEventQueue.push(object : EventQueue() {
            override fun dispatchEvent(event: AWTEvent) {
                super.dispatchEvent(event)
                if (peekEvent() == null) {
                    sy += floor(65536 * cos(angle)) * 0.1
                    sx += floor(65536 * sin(angle)) * 0.1
                    angle += 0.0014
                    render(sy, sx, angle)
                    repaint()
                }
            }
        })
    }
}
