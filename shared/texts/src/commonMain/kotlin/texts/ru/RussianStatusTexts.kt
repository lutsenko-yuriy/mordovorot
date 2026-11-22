package texts.ru

import texts.StatusTexts

object RussianStatusTexts : StatusTexts {
    override fun saved(name: String) = "Сохранено как «$name»."
    override fun saveFailed(name: String) = "Не удалось сохранить «$name». Возможно, хранилище браузера заполнено или отключено."
    override fun saveNameRejected(name: String) =
        "Имя сохранения не может быть пустым, содержать косую черту или «..», поэтому «$name» не сохранено. Выбери другое имя."
    override fun loaded(name: String) = "Загружено «$name»."
    override fun loadFailed(name: String) = "Не удалось загрузить «$name». Попробуй ещё раз."
    override fun saveDamaged(name: String) = "«$name» повреждено и не загружается. Его можно удалить в «Загрузить»."
    override val saveFromNewerVersion = "Это сохранение сделано в более новой версии игры. Перезагрузи страницу, чтобы обновиться."

    override fun noSaveNamed(name: String, saves: String) = "Нет сохранения «$name». $saves"
    override val couldNotListSaves = "Не удалось показать твои сохранения."
    override val noSavesAvailable = "Сохранений пока нет."
    override fun availableSaves(list: String) = "Твои сохранения: $list"
    override fun lettersDontFit(side: Int) =
        "Буквы не помещаются на доску ${side}x$side (26 букв, ${russianTiles(side * side)}). Выбери другие метки."
    override fun greekLettersDontFit(side: Int) =
        "Греческие буквы не помещаются на доску ${side}x$side (25 меток, ${russianTiles(side * side)}). Выбери другие метки."
    override fun prereformLettersDontFit(side: Int) =
        "Дореформенные буквы не помещаются на доску ${side}x$side (36 меток, ${russianTiles(side * side)}). Выбери другие метки."
    override fun cyrillicLettersDontFit(side: Int) =
        "Кириллические буквы не помещаются на доску ${side}x$side (28 букв, ${russianTiles(side * side)}). Выбери другие метки."
    override fun cantBeSolved(save: String) = "Ни одна последовательность сдвигов не решит «$save»."
    override fun repaired(name: String) = "«$name» исправлено (две плитки были поменяны местами) и загружено."
    override fun repairedNotUpdated(name: String) = "«$name» исправлено и загружено, но обновить сохранение не удалось."

    override fun deleted(name: String) = "«$name» удалено."
    override fun deletedNoneLeft(name: String) = "«$name» удалено. Сохранений не осталось."
    override fun alreadyDeleted(name: String) = "«$name» уже удалено."
    override fun couldNotDelete(name: String) = "Не удалось удалить «$name». Попробуй ещё раз."
    override fun saveNameHasSpaces(name: String) =
        "Имя сохранения не может содержать пробелы, поэтому «$name» не сохранено. Выбери другое имя."
    override val linkCopied = "Ссылка скопирована."
    override val copyFailed = "Не удалось скопировать ссылку. Она выделена, так что можно скопировать её вручную."
    override fun startedShared(side: Int) = "Открыта общая доска ${side}x$side."

    override fun linkUnsupportedSize(side: Int, min: Int, max: Int) =
        "Доска по этой ссылке ${side}x$side, а игра поддерживает от ${min}x$min до ${max}x$max."
    override val linkUnsolvable = "Ни одна последовательность сдвигов не решит доску по этой ссылке."
    override val linkInvalid = "Ссылка повреждена, поэтому доску не открыть."
}
