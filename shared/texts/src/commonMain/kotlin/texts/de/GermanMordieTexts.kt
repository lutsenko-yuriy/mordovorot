package texts.de

import texts.MordieTexts

object GermanMordieTexts : MordieTexts {
    override val opening = "Super! Knacken wir dieses Rätsel!"
    override fun resumed(moves: Int) = "Okay, wir haben aufgehört, als ich ${germanMoves(moves)} gemacht hatte! Wo war ich stehengeblieben?"
    override fun friendSolving(moves: Int) = "Okay, dein Freund hat ${germanMoves(moves)} gemacht und es noch nicht gelöst. Helfen wir ihm?"
    override fun friendSolved(moves: Int) = "Okay, dein Freund hat es in ${germanMoves(moves, dative = true)} gelöst. Schaffst du es schneller?"
    override val friendGaveUp = "Okay, dein Freund hat aufgegeben, ohne eine Lösung zu finden. Finden wir eine?"
    override fun friendGaveUpAfter(moves: Int) =
        "Okay, dein Freund hat nach ${germanMoves(moves, dative = true)} aufgegeben, ohne eine Lösung zu finden. Finden wir eine?"
    override fun progress(moves: Int) = "Ich habe schon ${germanMoves(moves)} gemacht und es noch nicht gelöst!"
    override fun progressHard(moves: Int) = "Das ist ein kniffliges Rätsel! " + progress(moves)
    override fun found(moves: Int) = "Geschafft! Ich habe eine Lösung in ${germanMoves(moves, dative = true)} gefunden! Schaffst du es schneller?"
    override fun foundUnproven(moves: Int) =
        "Ich habe eine Lösung in ${germanMoves(moves, dative = true)} gefunden, aber vielleicht gibt es eine kürzere! Schaffst du es schneller?"
    override fun shorter(moves: Int) = "Moment, ich habe eine kürzere gefunden: jetzt ${germanMoves(moves)}! Schaffst du es schneller?"

    override fun endingPlayerWon(mine: Int) = "Du warst schneller als ich! Ich war erst bei ${germanMoves(mine, dative = true)}."
    override fun endingMatched(mine: Int) = "Wow, Gleichstand! Wir haben beide ${germanMoves(mine)} gebraucht!"
    override fun endingMordieWon(mine: Int, yours: Int) =
        "Tut mir leid, diesmal war ich schneller: Ich habe ${germanMoves(mine)} gebraucht, du ${germanMoves(yours)}."
    override fun endingBeatUnproven(mine: Int, yours: Int) =
        "Wow, deine Lösung ist kürzer: Du hast ${germanMoves(yours)}, ich ${germanMoves(mine)}! Meine war vielleicht nicht die kürzeste."
    override fun endingMachine(mine: Int, yours: Int) =
        "Ich habe ${germanMoves(mine)} gebraucht und du ${germanMoves(yours)}! Bist du etwa eine Maschine?"

    override fun solutionSolved(moves: Int) = "Rätsel gelöst in ${germanMoves(moves, dative = true)}."
    override val solutionNotStarted = "Dieses Rätsel ist noch unberührt."
    override fun solutionStopped(moves: Int) = "Aufgehört nach ${germanMoves(moves, dative = true)}."
    override fun solutionHere(length: Int) = "So habe ich es in ${germanMoves(length, dative = true)} gelöst!"
}
