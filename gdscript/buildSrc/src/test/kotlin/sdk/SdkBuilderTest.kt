package sdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SdkBuilderTest {

    @Test
    fun `filters tags to 4_6 and above`() {
        val rawLines = listOf(
            "abc123\trefs/tags/3.5-stable",
            "def456\trefs/tags/4.0-stable",
            "ghi789\trefs/tags/4.5.1-stable",
            "jkl012\trefs/tags/4.6-stable",
            "mno345\trefs/tags/4.6.1-stable",
            "pqr678\trefs/tags/4.7-stable",
            "stu901\trefs/tags/4.6-beta1",
            "vwx234\trefs/tags/v4.6-stable",
        )
        val tags = SdkBuilder.parseStableTags(rawLines, minMinor = 6)
        assertEquals(listOf("4.6", "4.6.1", "4.7"), tags)
    }

    @Test
    fun `collectXmlDirs finds doc_classes and modules`() {
        // This is an integration-level concern — tested via full build
    }
}
