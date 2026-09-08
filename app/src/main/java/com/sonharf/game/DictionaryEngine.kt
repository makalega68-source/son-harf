package com.sonharf.game

import java.util.Locale

object DictionaryEngine {
    private val tr = HashSet<String>()
    private val en = HashSet<String>()

    private val SEED_TR = listOf(
        "KALEM","MASA","ARABA","KITAP","PAMUK","KEDI","MEKTUP","PENCERE","ELMA","ARMUT",
        "TARAK","KAPI","MARTI","IGNE","EKMEK","KUTU","URUN","NAR","RENK","KUS","SU","UZUM",
        "MAKAS","SANDALYE","ESYA","ALTIN","NOTA","ANAHTAR","RUZGAR","RAF","FIRIN","NEHIR",
        "ROMAN","MANDAL","LIMON","NANE","EV","VAZO","OKUL","LALE","EMEK","KEMAN","NOKTA",
        "AYNA","ATES","SABUN","NAKIS","SAAT","TUZ","ZEYTIN","NISAN","NAZAR","RESIM","MISIR",
        "RADYO","ORMAN","NAMLU","UFUK","KEDER","RUYA","AKIL","LIMAN","NEFES","SEHIR","REHBER"
    )
    private val SEED_EN = listOf(
        "APPLE","EAGLE","ELEPHANT","TIGER","ROBOT","TABLE","ENERGY","YELLOW","WINDOW","WATER",
        "RIVER","ROCKET","TOWER","RADIO","OCEAN","NIGHT","TRAIN","NORTH","HOUSE","EARTH",
        "HONEY","YOUTH","HEART","TREE","EMBER","ROSE","EDGE","EAGER","READY","YARD","DREAM",
        "MOUNTAIN","NOISE","ECHO","ORANGE","ENGINE","ENTER","RESULT","TARGET","TIGER","GARDEN"
    )

    init { load("tr", SEED_TR); load("en", SEED_EN) }

    fun load(language: String, words: List<String>) {
        val t = if (language == "tr") tr else en
        words.forEach { t.add(normalize(it, language)) }
    }

    fun size(language: String) = if (language == "tr") tr.size else en.size

    fun isValidWord(word: String, language: String): Boolean {
        val n = normalize(word, language)
        if (n.length < 2) return false
        return (if (language == "tr") tr else en).contains(n)
    }

    fun normalize(word: String, language: String): String {
        val loc = if (language == "tr") Locale("tr", "TR") else Locale.ROOT
        var c = word.trim().uppercase(loc)
        if (language == "tr") c = c.replace("Â", "A").replace("Î", "I").replace("Û", "U")
        return c.filter { it.isLetter() }
    }

    fun anyWordStartingWith(letter: Char, language: String): String? {
        val t = if (language == "tr") tr else en
        val up = letter.uppercaseChar()
        return t.firstOrNull { it.isNotEmpty() && it[0] == up }
    }

    fun calculatePoints(word: String, language: String): Int =
        normalize(word, language).fold(0) { s, c -> s + getLetterPoint(c, language) }

    fun getLetterPoint(letter: Char, language: String): Int = if (language == "tr") {
        when (letter) {
            'A','E','İ','K','L','M','N','R','T' -> 1
            'I','S','U','Y' -> 2
            'B','D','O','Ü' -> 3
            'C','Ç','Ş','Z' -> 4
            'G','H','P' -> 5
            'F','Ö','V' -> 7
            'Ğ' -> 8
            'J' -> 10
            else -> 1
        }
    } else {
        when (letter) {
            'E','A','I','O','N','R','T','L','S','U' -> 1
            'D','G' -> 2
            'B','C','M','P' -> 3
            'F','H','V','W','Y' -> 4
            'K' -> 5
            'J','X' -> 8
            'Q','Z' -> 10
            else -> 1
        }
    }
}
