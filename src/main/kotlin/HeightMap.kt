import kotlin.random.Random

class HeightMap {
    private val data = IntArray(256 * 256)
    private val colors = IntArray(256 * 256)

    operator fun get(offset: Int) = data[offset]

    fun colorAt(offset: Int) = colors[offset]

    fun generate(scale: Int) {
        val rnd = Random(System.nanoTime())
        var p = 256
        while (p > 1) {
            val p2 = p shr 1
            val k = p * 8 + 20
            val k2 = k shr 1
            for (i in 0 until 256 step p) {
                for (j in 0 until 256 step p) {
                    val a = data[(i shl 8) + j]
                    val b = data[(i + p and 255 shl 8) + j]
                    val c = data[(i shl 8) + (j + p and 255)]
                    val d = data[(i + p and 255 shl 8) + (j + p and 255)]
                    data[(i shl 8) + (j + p2 and 255)] = ((a + c shr 1) + (scale * rnd.nextDouble() % k - k2)).toInt().coerceIn(0, 255)
                    data[(i + p2 and 255 shl 8) + (j + p2 and 255)] = ((a + b + c + d shr 2) + (scale * rnd.nextDouble() % k - k2)).toInt().coerceIn(0, 255)
                    data[(i + p2 and 255 shl 8) + j] = ((a + b shr 1) + (scale * rnd.nextDouble() % k - k2)).toInt().coerceIn(0, 255)
                }
            }
            p = p2
        }
    }

    fun smooth() {
        var i = 0
        while (i < 65536) {
            for (j in 0..255) {
                val a = data[(i + 256 and 0xFF00) + j]
                val b = data[i + (j + 1 and 0xFF)]
                val c = data[(i - 256 and 0xFF00) + j]
                val d = data[i + (j - 1 and 0xFF)]
                data[i + j] = a + b + c + d shr 2
            }
            i += 256
        }
    }

    fun colorize() {
        for (y in 0 until 65536 step 256) {
            for (x in 0 until 256) {
                colors[y + x] = (128 + (data[(0xFF00 and y + 256) + (0xFF and x + 1)] - data[y + x]) * 4).coerceIn(0, 255)
            }
        }
    }
}
