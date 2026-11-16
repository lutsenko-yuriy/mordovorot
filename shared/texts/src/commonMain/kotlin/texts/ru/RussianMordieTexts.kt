package texts.ru

import texts.MordieTexts

object RussianMordieTexts : MordieTexts {
    override val opening = "Отлично! Давай разберёмся с этой!"
    override fun resumed(moves: Int) = "Так, мы прервались, когда я сделал ${russianMoves(moves)}! На чём я остановился?"
    override fun friendSolving(moves: Int) = "Так, твой друг сделал ${russianMoves(moves)} и пока не решил её. Поможем?"
    override fun friendSolved(moves: Int) = "Так, твой друг решил её за ${russianMoves(moves)}. Сможешь быстрее?"
    override val friendGaveUp = "Так, твой друг сдался, не найдя решения. Найдём его?"
    override fun friendGaveUpAfter(moves: Int) =
        "Так, твой друг сдался после ${russianMovesGenitive(moves)}, не найдя решения. Найдём его?"
    override fun progress(moves: Int) = "Я уже сделал ${russianMoves(moves)} и всё ещё не решил её!"
    override fun progressHard(moves: Int) = "Эта сложная! " + progress(moves)
    override fun found(moves: Int) = "Есть! Я нашёл решение за ${russianMoves(moves)}! Сможешь быстрее?"
    override fun foundUnproven(moves: Int) =
        "Нашёл решение за ${russianMoves(moves)}, но, может, есть и короче! Сможешь быстрее?"
    override fun shorter(moves: Int) = "Стоп, я нашёл короче: теперь ${russianMoves(moves)}! Сможешь быстрее?"

    override fun endingPlayerWon(mine: Int) = "Ты меня обогнал! Я успел сделать только ${russianMoves(mine)}."
    override fun endingMatched(mine: Int) = "Круто, ничья! Мы оба справились за ${russianMoves(mine)}!"
    override fun endingMordieWon(mine: Int, yours: Int) =
        "Извини, в этот раз я быстрее: я справился за ${russianMoves(mine)}, а ты за ${russianMoves(yours)}."
    override fun endingBeatUnproven(mine: Int, yours: Int) =
        "Ого, ты нашёл короче: ты за ${russianMoves(yours)}, я за ${russianMoves(mine)}! Моё могло быть не самым коротким."
    override fun endingMachine(mine: Int, yours: Int) =
        "Я сделал ${russianMoves(mine)}, а ты ${russianMoves(yours)}! Ты, случайно, не машина?"

    override fun solutionSolved(moves: Int) = "Головоломка решена за ${russianMoves(moves)}."
    override val solutionNotStarted = "Эта головоломка ещё не тронута."
    override fun solutionStopped(moves: Int) = "Остановка после ${russianMovesGenitive(moves)}."
    override fun solutionHere(length: Int) = "Вот как я решил её за ${russianMoves(length)}!"
}
