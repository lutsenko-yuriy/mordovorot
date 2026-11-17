package texts.ru

import texts.PageTexts

object RussianPageTexts : PageTexts {
    override val title = "Мордоворот"
    override val language = "Язык"

    override val toolbarNew = "Новая"
    override val toolbarSave = "Сохранить"
    override val toolbarLoad = "Загрузить"
    override val toolbarLabels = "Метки"
    override val toolbarShare = "Поделиться"
    override val toolbarControls = "Управление"
    override val giveUp = "Сдаться"
    override val mordiesSolution = "Решение Морди"
    override val tutorial = "Обучение"
    override val tutorialControls = "Управление"
    override val tutorialLabels = "Метки"
    override val tutorialSkip = "Пропустить"
    override val replayPrev = "◀ Назад"
    override val replayNext = "Далее ▶"
    override val replayRestart = "↻ Заново"
    override fun movesCounter(moves: Int) = "Ходы: $moves"

    override val mordieName = "Морди:"
    override val solved = "Поздравляю ✓"

    override fun footerBasedOn(version: String) = "v$version · По мотивам Loopover, автор "
    override val footerOriginal = "оригинал"
    override fun footerUpdateNotice(version: String) = "v$version · Вышла новая версия. "
    override val footerUpdate = "Обновить"

    override val labelNumbers = "Числа"
    override val labelNumbersFrom0 = "Числа с 0"
    override val labelPairs = "Пары"
    override val labelPairsFrom0 = "Пары с 0"
    override val labelLetters = "Латинские буквы"
    override val labelLetterPairs = "Пары латинских букв"
    override val labelCyrillicLetters = "Кириллические буквы"
    override val labelCyrillicLetterPairs = "Кириллические пары"
    override val labelGroupNumbers = "Числа"
    override val labelGroupLatin = "Латинские буквы"
    override fun labelGroupMarked(group: String, row: String) = "$group, выбрано: $row"
    override val labelRowLetters = "Буквы"
    override val labelGroupCyrillic = "Кириллические буквы"
    override fun labelLimit(side: Int) = "до ${side}x$side"
    override val layoutRightHanded = "Для правши"
    override val layoutLeftHanded = "Для левши"

    override val unsolvable = "не решается"
    override val stepRow = "Строка"
    override val stepColumn = "Столбец"
}
