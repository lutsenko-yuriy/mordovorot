# ADR-0004 — store a save as a marker character plus base64url of a bit-packed stream, with Mordie's solution last

## Status
`accepted`

## Context
A save is plain text: nine lines, with the board as numbers and Mordie's solution as `r0:1 c2:3 …` tokens (`SaveFileCodec`). Since Mordie plays every size (GH-225), a 9x9 save is about 2.1 KB, 72% of it the solution. The user wants saves small, with under 1 KB as a Should, and accepts that a save may stop being human-readable. The only store is the browser's `localStorage`: text only, 5 MiB per origin ([MDN](https://developer.mozilla.org/en-US/docs/Web/API/Storage_API/Storage_quotas_and_eviction_criteria)), so bits need a character encoding, and common Kotlin has `kotlin.io.encoding.Base64.UrlSafe` since 2.2 ([Kotlin API](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io.encoding/-base64/)). The measurements are in `docs/knowledge/notes/GH-220.md`.

## Decision
A save is written as one marker character that is not a digit, then base64url of a bit stream. The stream holds a fixed header first (the side in 4 bits, labels in 3 bits with an escape for an unknown one, status in 2 bits, a solution-screen flag, a has-solution flag), the move count as a varint, the current board and Mordie's original as Lehmer codes (a permutation as digits of shrinking range, [Lehmer code](https://en.wikipedia.org/wiki/Lehmer_code)), N and, when unproven, R as varints ([varint](https://protobuf.dev/programming-guides/encoding/)), and last Mordie's solution, one number per move (axis, index and shift together). The sizes depend on the side, so the side is read first. The old text format stays readable: a save whose first character is a digit is decoded as before. It is deprecated (GH-241, `@Deprecated`), with no removal date. Every save measured is under about 610 characters, 9x9 included. A Lehmer digit can be out of range when its range is not a power of two, so the decoder checks each digit.

## Alternatives considered
| Option | Why not chosen |
|---|---|
| Keep the text format | A 9x9 save is about 2.1 KB, over the 1 KB Should, and the user doesn't need it readable. |
| Denser text (one character per tile, two per move) | About 850 characters on 9x9, only 2.6x smaller; it keeps readability the user doesn't need. |
| Deflate on top of the packed bytes | No gain (591 against 584 characters on 9x9), and needs `expect`/`actual` code since `java.util.zip` is JVM-only and the browser's `CompressionStream` is asynchronous. |
| Entropy coding of the moves (Huffman, arithmetic) | At most 15% of the solution part (about 60 characters on 9x9) for a lot of code. |
| Drop the solution and recompute it | 156 characters on 9x9, but only the fast solver's answer can be recomputed; the exact search's shorter answer cannot (GH-224), and the fast solver's output would have to stay frozen. A Could for later. |
| Side minus 3 in 3 bits | Would put the 2x2 boards of the codec tests out of range; the side itself in 4 bits costs one bit and leaves room to grow (decided in the GH-237 plan). |
| Plain fixed-width tiles instead of Lehmer codes | 700 characters on 9x9 still meets the Should; kept as the fallback if the Lehmer code proves a burden, since it costs about 6% only. |

## Related ticket
GH-220

## Date
2026-11-02
