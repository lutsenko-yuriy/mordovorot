package texts

import java.io.File
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import texts.en.EnglishTexts

/**
 * Pins every English text (GH-274): each key, called with sample arguments, against the committed
 * `english.txt`. A changed or new text shows up as a diff of that file, and a key's parameters are
 * limited to `Int`, `String` and `Boolean`, so every language can inflect them itself.
 *
 * After a deliberate change, copy `build/english.actual.txt` over `src/jvmTest/resources/english.txt`.
 */
class EnglishSnapshotTest {

    @Test
    fun `every English text matches the committed english txt`() {
        val actual = snapshot()
        val expected = checkNotNull(javaClass.getResourceAsStream("/english.txt")) { "english.txt is missing" }
            .reader().readText()
        if (actual != expected) {
            val file = File("build/english.actual.txt").apply { parentFile.mkdirs(); writeText(actual) }
            fail("The English texts changed. Review them against english.txt, then copy ${file.path} over it.\n" + firstDifference(expected, actual))
        }
    }

    @Test
    fun `every key takes only numbers, names and flags`() {
        val bad = keys().flatMap { (key, method) ->
            method.parameterTypes.filter { it != Int::class.java && it != String::class.java && it != Boolean::class.java }.map { "$key: ${it.simpleName}" }
        }
        assertEquals(emptyList(), bad)
    }

    private fun snapshot(): String = keys().sortedBy { it.first }.flatMap { (key, method) ->
        val target = if (key.contains('.')) group(key.substringBefore('.')) else EnglishTexts
        argumentSets(method).map { args -> "$key${args.display()} = ${render(method.invoke(target, *args.toTypedArray()))}" }
    }.joinToString("\n", postfix = "\n")

    /** Every public key as `group.name` (or the root's own `name`) with its interface method. */
    private fun keys(): List<Pair<String, Method>> = Texts::class.java.methods.flatMap { root ->
        if (root.parameterCount == 0 && root.returnType.name.startsWith("texts.") && root.returnType != String::class.java) {
            root.returnType.methods.map { "${name(root)}.${name(it)}" to it }
        } else {
            listOf(name(root) to root)
        }
    }

    private fun group(name: String): Any =
        Texts::class.java.methods.first { name(it) == name }.invoke(EnglishTexts)!!

    /** `getFoo` is the key `foo`. */
    private fun name(method: Method) =
        if (method.parameterCount == 0 && method.name.startsWith("get")) method.name.removePrefix("get").replaceFirstChar { it.lowercase() } else method.name

    /** Ints go 0, 1, 2 and 5 one at a time, the others staying 5; strings are `foo`, `bar`, `baz`; flags both ways. */
    private fun argumentSets(method: Method): List<List<Any>> {
        val types = method.parameterTypes
        var sets = listOf(emptyList<Any>())
        val strings = listOf("foo", "bar", "baz")
        var stringIndex = 0
        types.forEach { type ->
            sets = when (type) {
                String::class.java -> sets.map { it + strings[stringIndex] }.also { stringIndex++ }
                Boolean::class.java -> sets.flatMap { listOf(it + false, it + true) }
                else -> sets.map { it + 5 }
            }
        }
        val ints = types.indices.filter { types[it] == Int::class.java }
        val varied = ints.flatMap { at -> listOf(0, 1, 2).flatMap { value -> sets.map { it.toMutableList().apply { this[at] = value } } } }
        return (varied + sets).distinct()
    }

    private fun List<Any>.display() = if (isEmpty()) "" else joinToString(", ", "(", ")")

    private fun render(value: Any?) = when (value) {
        is List<*> -> value.joinToString(" | ")
        else -> value.toString()
    }

    private fun firstDifference(expected: String, actual: String): String {
        val a = expected.lines()
        val b = actual.lines()
        val at = a.indices.firstOrNull { a[it] != b.getOrNull(it) } ?: a.size
        return "First difference at line ${at + 1}:\n  committed: ${a.getOrNull(at)}\n  actual:    ${b.getOrNull(at)}"
    }
}
