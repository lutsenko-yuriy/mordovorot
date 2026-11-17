package texts.ru

import texts.KeyTexts

object RussianKeyTexts : KeyTexts {
    override val hintSelect = "выбор"
    override val hintRow = "строка"
    override val hintColumn = "столбец"
    override val hintPrevNext = "назад/далее"
    override val hintNext = "далее"
    override val hintRestart = "заново"
    override val hintScroll = "прокрутка"

    override fun letterOrArrow(letter: String, arrow: String) = "$letter или $arrow"

    override val rightHandedSelect = "Выбор плитки   W A S D"
    override val rightHandedShiftRow = "Сдвиг строки   ← →  H L"
    override val rightHandedShiftColumn = "Сдвиг столбца  ↑ ↓  K J"
    override val leftHandedSelect = "Выбор плитки   ← ↑ ↓ →  H J K L"
    override val leftHandedShiftRow = "Сдвиг строки   A D"
    override val leftHandedShiftColumn = "Сдвиг столбца  W S"
    override val toolbarLine1 = "Новая 1 · Сохранить 2 · Загрузить 3 · Метки 4"
    override val toolbarLine2 = "Сдаться 5 · Подсказка 8 · Обучение 9"
    override val toolbarLine3 = "0 Озвучить доску (скринридер)"
    override val toolbarLine4 = "Управление 6 · Поделиться 7"
    override val solutionLine1 = "Решение: ← → A D H L назад/далее ·"
    override val solutionLine2 = "  Пробел/Enter далее · R заново ·"
    override val solutionLine3 = "  ↑ ↓ W S K J прокрутка"
}
