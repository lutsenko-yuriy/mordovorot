# ADR-0005 — a share link is the page's address plus a `#b=` fragment holding the starting board

## Status
`accepted`

## Context
GH-243 lets a player send a board to someone else. A link lives in chats and printed QR codes, so once one is out its format is a promise: a later build has to keep opening it. Only the board travels (no moves, no labels). The page is static on GitHub Pages, so there is no server to look a board up on. The save format (ADR-0004) already packs a board as a Lehmer code in base64url, and a QR code in byte mode at error-correction level M holds 213 bytes at version 10, far more than a 9x9 board needs.

## Decision
The link is `<origin><path>#b=<board>`. The fragment is never sent to the server, so GitHub's logs never see a board. `<board>` is base64url without padding of a bit stream: the side in 4 bits, then the arrangement as a Lehmer code (`storage.BoardLinkFormat`, built on `SaveBits`). A 9x9 board is 75 characters. The decoder rejects bad base64, a side below 2, a digit out of range, trailing data and non-canonical text (re-encoding must give the input back), and `ShareLink` then rejects a side outside 3..9 and an unsolvable board. Other `&key=value` parts of the fragment are ignored, which leaves room for more later (a Mordie solution, labels) without breaking links already out. Links in the wild are never broken: a change to the board's layout takes a new key, and `b=` keeps decoding as it does now. A link opened in a tab that is already playing arrives as `hashchange`, and the page handles it.

## Alternatives considered
| Option | Why not chosen |
|---|---|
| A query `?b=` | The board would go to the server and its logs, and `replaceState` would still have to clean it. A link opened in a tab would reload the page, which does at least get the browser's Leave site? prompt for free. |
| The text save format, or a whole save, in the link | A 9x9 save is far longer than a board, and a QR code of it would need a version past 10. |
| A short link through a service | Needs a server or a third party; out of scope. |
| Plain fixed-width tiles instead of a Lehmer code | Longer by about 6% on 9x9; the Lehmer code is already written for saves. |
| A checksum | Every bit pattern that decodes is a valid permutation, and the canonical-text check catches stray bits; a mistyped link that still decodes names some other valid board, which is harmless. |

## Related ticket
GH-243

## Date
2026-11-02

## Amendment — 2026-11-14 (GH-267): tile labels in the link
The link may carry the sender's tile labels as a second part, `<origin><path>#b=<board>&l=<labels>`, `<labels>` being `TileLabels.wireName` (`numbers`, `numbers_from_0`, `pairs`, `pairs_from_0`, `letters`, `letter_pairs`), the name saves, the stored preference and analytics already freeze. `b=` is untouched, and a link without `l=` is exactly what it was. The first `l=` part wins, as the first `b=` does. A value that isn't one of those names exactly (empty, other case, unknown), or a scheme too small for the board (Letters on 6x6 and up), is unreadable: the page ignores it without a word and opens the board as a plain link. A board's rejection is checked first and is never changed by `l=`. Builds from before this amendment, including the copy an offline worker keeps until the player updates, read `b=` only and open a labelled link as a plain board, as the Decision above promised; `ShareLinkTest` pins that. A labelled 9x9 link is at most 17 characters longer, which moves its QR code from version 7 to 8. The page never changes the receiver's preference from a link: the receiver is asked, and Yes shows the labels for that game only.

## Amendment — 2026-11-15 (GH-272): the sender's progress in the link
The link may carry the sender's progress as a further part, `<origin><path>#b=<board>&p=<progress>`: one letter and a decimal move count, `n12` (not solved yet), `s12` (solved), `g12` (gave up), or `g` alone (gave up, count unknown: a solution-screen save from before GH-273 kept none). The count is 1 to 9999999 with no leading zero. Anything else (empty, `n0`, `n012`, eight digits, an unknown letter, other case, `s` alone) is unreadable: the page ignores it without a word and opens the board as a plain link. The first `p=` part wins, as with `b=` and `l=`, and a board's rejection is checked first and never changed by `p=`. `b=` and `l=` are untouched, and a link without `p=` is exactly what it was. Builds from before this amendment, including the copy an offline worker keeps until the player updates, read `b=` only and open the board without the friend's line; `ShareLinkTest` pins that. The progress is not checked against the board: a hand-edited link can claim any count. It adds at most 9 characters, which can move a QR code up a version at the margin. The link never holds Mordie's solution or the moves themselves.

## Amendment — 2026-11-17 (GH-322): the Greek labels
The `l=` list gains `greek_letters` and `greek_letter_pairs`, with no change to the format. Greek letters are unreadable on 6x6 and larger (25 labels, 36 tiles); Greek letter pairs are readable on every board the page offers. Older builds read the new names as unknown and open a plain board, as for any unknown name. The wire names are ASCII, so the link never carries a Greek character (and so never the U+037E that normalises to a semicolon).

## Amendment — 2026-11-17 (GH-313): the Cyrillic labels
The `l=` list gains two names, `cyrillic_letters` and `cyrillic_letter_pairs`, with no change to the format. Cyrillic letters, like Letters, are unreadable on 6x6 and larger (28 labels, 36 tiles); Cyrillic letter pairs are readable on every board the page offers. Builds from before this amendment read an unknown `l=` value as unreadable and open the board as a plain link, as they do for any unknown name. The longest labelled link, a 9x9 board with `l=cyrillic_letter_pairs` and a 7-digit progress, is 24 characters of `&l=` and still fits QR version 10; `ShareLinkTest` pins that.
