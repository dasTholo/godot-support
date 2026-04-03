package gdscript.uid

import java.security.SecureRandom

object GdUidGenerator {

    private const val UID_PREFIX = "uid://"

    // Godot Base-33 charset: a-y (25 letters, no z) + 0-8 (9 digits, no 9)
    private val CHARSET = charArrayOf(
        'a','b','c','d','e','f','g','h','i','j','k','l','m',
        'n','o','p','q','r','s','t','u','v','w','x','y',
        '0','1','2','3','4','5','6','7'
    )
    private const val BASE = 33

    private val random = SecureRandom()

    /**
     * Generate a Godot-compatible UID string like "uid://bomjbuul48ncl".
     */
    fun generateUid(): String {
        val value = generateRandomValue()
        return "$UID_PREFIX${encodeBase33(value)}"
    }

    /**
     * Generate a UID that does not collide with any in [existingUids].
     */
    fun generateUniqueUid(existingUids: Set<String>): String {
        while (true) {
            val uid = generateUid()
            if (uid !in existingUids) return uid
        }
    }

    /**
     * Encode a non-negative Long as a Base-33 string using Godot's charset.
     * Matches the algorithm in core/io/resource_uid.cpp id_to_text().
     */
    fun encodeBase33(value: Long): String {
        if (value == 0L) return CHARSET[0].toString()

        val chars = mutableListOf<Char>()
        var remaining = value
        while (remaining > 0) {
            chars.add(CHARSET[(remaining % BASE).toInt()])
            remaining /= BASE
        }
        return chars.reversed().joinToString("")
    }

    private fun generateRandomValue(): Long {
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        var value = 0L
        for (b in bytes) {
            value = (value shl 8) or (b.toLong() and 0xFF)
        }
        return value and 0x7FFFFFFFFFFFFFFFL // mask sign bit → 63-bit positive
    }
}
