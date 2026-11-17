package texts.de

import texts.StatusTexts

object GermanStatusTexts : StatusTexts {
    override fun saved(name: String) = "Gespeichert als „$name“."
    override fun saveFailed(name: String) = "„$name“ konnte nicht gespeichert werden. Vielleicht ist der Browser-Speicher voll oder deaktiviert."
    override fun saveNameRejected(name: String) =
        "Ein Speichername darf nicht leer sein und weder Schrägstrich noch „..“ enthalten, deshalb wurde „$name“ nicht gespeichert. Wähle einen anderen Namen."
    override fun loaded(name: String) = "„$name“ geladen."
    override fun loadFailed(name: String) = "„$name“ konnte nicht geladen werden. Versuche es noch einmal."
    override fun saveDamaged(name: String) = "„$name“ ist beschädigt und lässt sich nicht laden. Du kannst es unter „Laden“ löschen."
    override val saveFromNewerVersion = "Dieser Spielstand wurde mit einer neueren Version des Spiels erstellt. Bitte lade die Seite neu, um zu aktualisieren."

    override fun noSaveNamed(name: String, saves: String) = "Es gibt keinen Speicherstand „$name“. $saves"
    override val couldNotListSaves = "Deine Speicherstände konnten nicht angezeigt werden."
    override val noSavesAvailable = "Noch keine Speicherstände."
    override fun availableSaves(list: String) = "Deine Speicherstände: $list"
    override fun lettersDontFit(side: Int) =
        "Die Buchstaben passen nicht auf ein ${side}x$side-Brett (26 Buchstaben, ${germanTiles(side * side)}). Wähle andere Beschriftungen."
    override fun greekLettersDontFit(side: Int) =
        "Die griechischen Buchstaben passen nicht auf ein ${side}x$side-Brett (25 Beschriftungen, ${germanTiles(side * side)}). Wähle andere Beschriftungen."
    override fun cyrillicLettersDontFit(side: Int) =
        "Die kyrillischen Buchstaben passen nicht auf ein ${side}x$side-Brett (28 Buchstaben, ${germanTiles(side * side)}). Wähle andere Beschriftungen."
    override fun cantBeSolved(save: String) = "Keine Folge von Verschiebungen löst „$save“."
    override fun repaired(name: String) = "„$name“ wurde repariert (zwei Kacheln waren vertauscht) und geladen."
    override fun repairedNotUpdated(name: String) = "„$name“ wurde repariert und geladen, aber der Speicherstand ließ sich nicht aktualisieren."

    override fun deleted(name: String) = "„$name“ gelöscht."
    override fun deletedNoneLeft(name: String) = "„$name“ gelöscht. Keine Speicherstände mehr."
    override fun alreadyDeleted(name: String) = "„$name“ ist schon gelöscht."
    override fun couldNotDelete(name: String) = "„$name“ konnte nicht gelöscht werden. Versuche es noch einmal."
    override fun saveNameHasSpaces(name: String) =
        "Ein Speichername darf keine Leerzeichen enthalten, deshalb wurde „$name“ nicht gespeichert. Wähle einen anderen Namen."
    override val linkCopied = "Link kopiert."
    override val copyFailed = "Der Link konnte nicht kopiert werden. Er ist markiert, du kannst ihn von Hand kopieren."
    override fun startedShared(side: Int) = "Geteiltes ${side}x$side-Brett geöffnet."

    override fun linkUnsupportedSize(side: Int, min: Int, max: Int) =
        "Das Brett in diesem Link ist ${side}x$side, das Spiel unterstützt ${min}x$min bis ${max}x$max."
    override val linkUnsolvable = "Keine Folge von Verschiebungen löst das Brett in diesem Link."
    override val linkInvalid = "Der Link ist beschädigt, deshalb lässt sich das Brett nicht öffnen."
}
