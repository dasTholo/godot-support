package gdscript.uid

import org.junit.Assert.*
import org.junit.Test

class GdUidGeneratorTest {

    companion object {
        // Godot Base-33 charset: a-y (no z) + 0-8 (no 9)
        private val VALID_CHARS = ('a'..'y').toSet() + ('0'..'7').toSet()
    }

    @Test
    fun `encodeBase33 produces correct encoding for known value`() {
        // Value 0 should encode to "a" (first char in charset)
        assertEquals("a", GdUidGenerator.encodeBase33(0L))
    }

    @Test
    fun `encodeBase33 produces correct encoding for value 32`() {
        // Value 32 = last index in charset = '7'
        assertEquals("7", GdUidGenerator.encodeBase33(32L))
    }

    @Test
    fun `encodeBase33 produces correct encoding for value 33`() {
        // Value 33 = 1*33 + 0 = "ba"
        assertEquals("ba", GdUidGenerator.encodeBase33(33L))
    }

    @Test
    fun `generateUid starts with uid prefix`() {
        val uid = GdUidGenerator.generateUid()
        assertTrue("UID should start with 'uid://'", uid.startsWith("uid://"))
    }

    @Test
    fun `generateUid uses only valid characters`() {
        repeat(100) {
            val uid = GdUidGenerator.generateUid()
            val encoded = uid.removePrefix("uid://")
            assertTrue(
                "UID '$uid' contains invalid characters",
                encoded.all { it in VALID_CHARS }
            )
        }
    }

    @Test
    fun `generateUid produces strings of length 11 to 13 after prefix`() {
        repeat(100) {
            val uid = GdUidGenerator.generateUid()
            val encoded = uid.removePrefix("uid://")
            assertTrue(
                "UID encoded part length ${encoded.length} not in 11..13",
                encoded.length in 11..13
            )
        }
    }

    @Test
    fun `generateUid produces unique values`() {
        val uids = (1..1000).map { GdUidGenerator.generateUid() }.toSet()
        assertEquals("All 1000 UIDs should be unique", 1000, uids.size)
    }

    @Test
    fun `generateUniqueUid avoids collision with existing set`() {
        val first = GdUidGenerator.generateUid()
        val existing = setOf(first)
        val second = GdUidGenerator.generateUniqueUid(existing)
        assertNotEquals("Should generate different UID", first, second)
    }
}
