package texts

import kotlin.test.Test
import kotlin.test.assertEquals

/** The Controls list pads each line's action to a column of keys (GH-275): every language keeps its keys aligned. */
class ControlsColumnsTest {

    private fun KeyTexts.layouts() = mapOf(
        "right-handed" to listOf(rightHandedSelect, rightHandedShiftRow, rightHandedShiftColumn),
        "left-handed" to listOf(leftHandedSelect, leftHandedShiftRow, leftHandedShiftColumn),
    )

    /** Where the keys start: after the action and the padding of two or more spaces. */
    private fun keyColumn(line: String) = Regex("\\S  +(?=\\S)").find(line)!!.range.last + 1

    @Test
    fun `each layout's lines start their keys in one column`() {
        Language.entries.forEach { language ->
            language.texts.keys.layouts().forEach { (layout, lines) ->
                assertEquals(1, lines.map(::keyColumn).distinct().size, "${language.code} $layout: $lines")
            }
        }
    }
}
