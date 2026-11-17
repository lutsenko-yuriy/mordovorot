package texts

/** The 28 Cyrillic letters as said, in the order of `TileLabels.CYRILLIC_ALPHABET`, in every game language (GH-313). */
internal val RUSSIAN_SPELLING_WORDS = listOf(
    "Анна", "Борис", "Василий", "Григорий", "Дмитрий", "Елена", "Женя", "Зинаида", "Иван", "Константин", "Леонид", "Мария", "Николай", "Ольга",
    "Павел", "Роман", "Семён", "Тамара", "Ульяна", "Фёдор", "Харитон", "Цапля", "Человек", "Шура", "Щука", "Эхо", "Юрий", "Яков",
)

/** The 25 Greek labels as said in English, in the order of `TileLabels.GREEK_ALPHABET` (GH-322). */
internal val GREEK_WORDS_EN = listOf(
    "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda", "mu", "nu", "xi", "omicron",
    "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega", "Greek question mark",
)

/** The same in German. */
internal val GREEK_WORDS_DE = listOf(
    "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "My", "Ny", "Xi", "Omikron",
    "Pi", "Rho", "Sigma", "Tau", "Ypsilon", "Phi", "Chi", "Psi", "Omega", "griechisches Fragezeichen",
)

/** The same in Russian. */
internal val GREEK_WORDS_RU = listOf(
    "альфа", "бета", "гамма", "дельта", "эпсилон", "дзета", "эта", "тета", "йота", "каппа", "лямбда", "мю", "ню", "кси", "омикрон",
    "пи", "ро", "сигма", "тау", "ипсилон", "фи", "хи", "пси", "омега", "греческий вопросительный знак",
)

/** The letters A to Z as said, in every language: tiles are read by these English words (GH-275). */
internal val NATO_WORDS = listOf(
    "alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel", "india", "juliet", "kilo", "lima", "mike",
    "november", "oscar", "papa", "quebec", "romeo", "sierra", "tango", "uniform", "victor", "whiskey", "x-ray", "yankee", "zulu",
)
