package texts

import java.io.File
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Pins every text of every language (GH-274, GH-275): each key, called with sample arguments, against
 * the committed `<language>.txt` (`english.txt`, `german.txt`, `russian.txt`), which is also the translation review
 * sheet. A changed or new text shows up as a diff of that file, and a key's parameters are limited to
 * `Int`, `String` and `Boolean`, so every language can inflect them itself.
 *
 * After a deliberate change, copy `build/<language>.actual.txt` over `src/jvmTest/resources/<language>.txt`.
 */
class TextsSnapshotTest {

    private val names = mapOf(Language.EN to "english", Language.DE to "german", Language.RU to "russian")

    @Test
    fun `every text of every language matches its committed snapshot`() {
        val failures = Language.entries.mapNotNull { language ->
            val name = names.getValue(language)
            val actual = snapshot(language.texts)
            val expected = checkNotNull(javaClass.getResourceAsStream("/$name.txt")) { "$name.txt is missing" }
                .reader().readText()
            if (actual == expected) return@mapNotNull null
            val file = File("build/$name.actual.txt").apply { parentFile.mkdirs(); writeText(actual) }
            "The $name texts changed. Review them against $name.txt, then copy ${file.path} over it.\n" + firstDifference(expected, actual)
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n\n"))
    }

    @Test
    fun `every language has a name for its snapshot`() {
        assertEquals(Language.entries.toSet(), names.keys)
    }

    @Test
    fun `every key takes only numbers, names and flags`() {
        val bad = keys().flatMap { (key, method) ->
            method.parameterTypes.filter { it != Int::class.java && it != String::class.java && it != Boolean::class.java }.map { "$key: ${it.simpleName}" }
        }
        assertEquals(emptyList(), bad)
    }

    private fun snapshot(texts: Texts): String = keys().sortedBy { it.first }.flatMap { (key, method) ->
        val target = if (key.contains('.')) group(texts, key.substringBefore('.')) else texts
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

    private fun group(texts: Texts, name: String): Any =
        Texts::class.java.methods.first { name(it) == name }.invoke(texts)!!

    /** `getFoo` is the key `foo`. */
    private fun name(method: Method) =
        if (method.parameterCount == 0 && method.name.startsWith("get")) method.name.removePrefix("get").replaceFirstChar { it.lowercase() } else method.name

    /** Ints go 0, 1, 2, 3, 5, 11, 21 and 22 one at a time (Russian's one, few and many, and its teens), the others staying 5; strings are `foo`, `bar`, `baz`; flags both ways. */
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
        val varied = ints.flatMap { at -> listOf(0, 1, 2, 3, 5, 11, 21, 22).flatMap { value -> sets.map { it.toMutableList().apply { this[at] = value } } } }
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
