package texts

import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A translation is finished (GH-275): no text still reads as English, none is a placeholder, and a
 * Russian text is in Cyrillic. The snapshots pin the words; this pins that they were translated at all.
 */
class TranslationsTest {

    /** Texts that read the same in every language: the same word, a key's letters, or a name. */
    private val sameAsEnglish = setOf(
        "dialogs.ok", "boardSize", "reader.dragged", "reader.phonetic", "reader.cyrillicPhonetic", "page.title", "page.mordieName", "page.tutorial",
    )

    private val translated = Language.entries - Language.EN

    @Test
    fun `no translated text equals the English one, but the listed ones`() {
        translated.forEach { language ->
            val same = texts(language).filter { (key, value) -> value == english.getValue(key) && key !in sameAsEnglish }.keys
            assertEquals(emptySet(), same, "${language.code} still reads as English")
        }
    }

    @Test
    fun `no text is a placeholder`() {
        translated.forEach { language ->
            val bad = texts(language).filter { (_, value) -> listOf("TODO", "FIXME", "???").any { it in value } }.keys
            assertEquals(emptySet(), bad, language.code)
        }
    }

    @Test
    fun `a Russian text is in Cyrillic, but the listed ones`() {
        val latinOnly = texts(Language.RU).filter { (key, value) -> value.none { it in 'А'..'я' || it == 'ё' || it == 'Ё' } && key !in sameAsEnglish }.keys
        assertEquals(emptySet(), latinOnly)
    }

    private val english by lazy { texts(Language.EN) }

    /** Every key's text with sample arguments, as `group.name` to the text (lists joined). */
    private fun texts(language: Language): Map<String, String> {
        val root = language.texts
        return Texts::class.java.methods.flatMap { rootMethod ->
            val group = if (rootMethod.parameterCount == 0 && rootMethod.returnType.name.startsWith("texts.") && rootMethod.returnType != String::class.java) rootMethod.invoke(root) else null
            if (group != null) group.javaClass.interfaces.flatMap { it.methods.toList() }.map { "${name(rootMethod)}.${name(it)}" to call(group, it) }
            else listOf(name(rootMethod) to call(root, rootMethod))
        }.toMap()
    }

    private fun name(method: Method) =
        if (method.parameterCount == 0 && method.name.startsWith("get")) method.name.removePrefix("get").replaceFirstChar { it.lowercase() } else method.name

    private fun call(target: Any, method: Method): String {
        val args: List<Any> = method.parameterTypes.map { type ->
            when (type) {
                String::class.java -> "foo"
                Boolean::class.java -> true
                else -> 5
            }
        }
        val value = method.invoke(target, *args.toTypedArray())
        return if (value is List<*>) value.joinToString(" | ") else value.toString()
    }

    @Test
    fun `the texts of a language are found`() {
        assertTrue(texts(Language.RU).size > 100)
    }
}
