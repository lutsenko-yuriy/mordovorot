package testing

import kotlinx.coroutines.runBlocking

/** Runs a suspend test body to completion on a plain `runBlocking` event loop (GH-42) - the
 *  one place in the suite that touches `runBlocking`/`Unit` explicitly, so individual `@Test`
 *  functions never need `(): Unit = runBlocking { ... }`'s own explicit `Unit` annotation.
 *  Without it, `fun x() = runBlocking { ... }`'s return type is *inferred from the block's last
 *  expression* - a test ending in `assertFailsWith<T> { ... }` (which returns the caught `T`,
 *  not `Unit`) silently compiles to a non-`Unit`-returning method, and JUnit5 drops such `@Test`
 *  methods from discovery with no compile error, no test failure, no skip marker - just missing
 *  from the report (found the hard way on PR #43: 338 source `@Test`s, only 335 actually ran).
 *  Routing every test through this one `Unit`-returning function closes that off structurally,
 *  for every test, forever - not just the ones someone remembered to annotate. */
fun runTestBlocking(block: suspend () -> Unit): Unit = runBlocking { block() }
