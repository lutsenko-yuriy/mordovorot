package texts

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * No English text uses a word from the "Known aliases to avoid" table of `docs/GLOSSARY.md` (GH-274).
 * It reads the rendered texts from the committed `english.txt`, which [EnglishSnapshotTest] keeps current.
 */
class AvoidWordsTest {

    @Test
    fun `no English text contains an avoided word`() {
        val avoid = avoidedWords()
        assertTrue(avoid.size >= 10, "the Avoid column was not read: $avoid")
        val texts = checkNotNull(javaClass.getResourceAsStream("/english.txt")) { "english.txt is missing" }
            .reader().readLines().filter { it.isNotBlank() }
        val hits = texts.flatMap { line ->
            val text = line.substringAfter(" = ")
            avoid.filter { contains(text, it) }.map { "'$it' in: $line" }
        }
        assertEquals(emptyList(), hits)
    }

    /** Words match whole and case-insensitive; a term with punctuation, like `?..`, matches as plain text. */
    private fun contains(text: String, term: String): Boolean =
        if (term.all { it.isLetter() || it == ' ' }) Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        else text.contains(term, ignoreCase = true)

    private fun avoidedWords(): List<String> {
        val glossary = File("../../docs/GLOSSARY.md").readLines()
        val table = glossary.dropWhile { !it.startsWith("## Known aliases to avoid") }.drop(1)
            .filter { it.startsWith("|") }.drop(2)
        return table.flatMap { row -> row.split("|")[1].split(",").map { it.trim().trim('`') } }.filter { it.isNotEmpty() }
    }
}
