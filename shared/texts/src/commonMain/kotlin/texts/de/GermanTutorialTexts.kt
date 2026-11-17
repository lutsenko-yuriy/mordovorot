package texts.de

import texts.TutorialTexts

object GermanTutorialTexts : TutorialTexts {
    override fun step1(computer: Boolean) =
        "Hallo! Ich bin Mordie. Lass uns ein kurzes Spiel spielen. ${verb(computer)} die oberste Zeile um eine Kachel nach links" + lead(computer)

    override fun step2(tile: String, computer: Boolean) =
        "Die Linien laufen im Kreis: Siehst du Kachel $tile oben in der zweiten Spalte? ${verb(computer)} diese Spalte um eine Kachel nach oben, und" +
            " $tile kommt unten wieder herein" + lead(computer)

    override fun step3(computer: Boolean) =
        "Ein Zug verschiebt eine Zeile oder Spalte beliebig weit. ${verb(computer)} die dritte Zeile um zwei Kacheln nach rechts: Das zählt trotzdem als 1 Zug."

    override fun step4(computer: Boolean) =
        "Zuletzt: ${verb(computer).lowercase()} die vierte Spalte um eine Kachel nach unten. Damit ist es gelöst!"

    override val step5 = "Kannst du meine Ergebnisse schlagen?"

    override fun again(moves: Int) = "Noch einmal! Es ist dieselbe Zeile, deshalb bleiben die Züge bei $moves."
    override val blockedRow = "Das kannst du später machen. Verschiebe jetzt bitte diese Zeile!"
    override val blockedColumn = "Das kannst du später machen. Verschiebe jetzt bitte diese Spalte!"

    override fun dragLeft(tiles: Int) = "Ziehe sie " + if (tiles == 1) "nach links" else "um ${tilesBy(tiles)} nach links"
    override fun dragRight(tiles: Int) = "Ziehe sie " + if (tiles == 1) "nach rechts" else "um ${tilesBy(tiles)} nach rechts"
    override fun dragUp(tiles: Int) = "Ziehe sie " + if (tiles == 1) "nach oben" else "um ${tilesBy(tiles)} nach oben"
    override fun dragDown(tiles: Int) = "Ziehe sie " + if (tiles == 1) "nach unten" else "um ${tilesBy(tiles)} nach unten"

    override fun click(arrow: String, times: Int) = "Klicke" + times(times) + " auf $arrow"
    override fun press(keys: String, times: Int) = "Drücke $keys" + times(times)
    override fun pressThen(select: String, selectTimes: Int, shift: String, shiftTimes: Int) =
        "Drücke $select${times(selectTimes)}, dann $shift${times(shiftTimes)}"

    /** A computer shifts however it likes, and its ways follow on their own lines (GH-290); a phone drags. */
    private fun verb(computer: Boolean) = if (computer) "Verschiebe" else "Ziehe"

    private fun lead(computer: Boolean) = if (computer) ":" else "."

    private fun times(count: Int) = when (count) {
        1 -> ""
        2 -> " zweimal"
        3 -> " dreimal"
        else -> " $count-mal"
    }

    /** `um zwei Kacheln`, `um drei Kacheln`, else digits. */
    private fun tilesBy(count: Int) = when (count) {
        2 -> "zwei Kacheln"
        3 -> "drei Kacheln"
        else -> germanTiles(count)
    }
}
