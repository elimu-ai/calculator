package com.xlythe.math

import java.util.Arrays
import java.util.Locale

object SpellContext {
    private val mySuffixText: Array<String>? = arrayOf<String>(
        "",  // Dummy! no level 0
        "",  // Nothing for level 1
        " Thousand",
        " Million",
        " Billion",
        " Trillion",
        " (Thousand Trillion)",
        " (Million Trillion)",
        " (Billion Trillion)",
    )
    private val myTeenText: Array<String>? = arrayOf<String>(
        "Zero",
        "One",
        "Two",
        "Three",
        "Four",
        "Five",
        "Six",
        "Seven",
        "Eight",
        "Nine",
        "Ten",
        "Eleven",
        "Twelve",
        "Thirteen",
        "Fourteen",
        "Fifteen",
        "Sixteen",
        "Seventeen",
        "Eighteen",
        "Ninteen",
    )

    // used appropriately for under-cent values:
    private val myCentText: Array<String>? = arrayOf<String>(
        "Twenty",
        "Thirty",
        "Forty",
        "Fifty",
        "Sixty",
        "Seventy",
        "Eighty",
        "Ninety"
    )

    // used appropriately for under-mil values.
    private val myMilText: Array<String>? = arrayOf<String>(
        "One Hundred",
        "Two Hundred",
        "Three Hundred",
        "Four Hundred",
        "Five Hundred",
        "Six Hundred",
        "Seven Hundred",
        "Eight Hundred",
        "Nine Hundred"
    )
    private val myBelowThousandWords = arrayOf<String?>(
        "zero",
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
        "eleven",
        "twelve",
        "thirteen",
        "fourteen",
        "fifteen",
        "sixteen",
        "seventeen",
        "eighteen",
        "ninteen",
        "twenty",
        "thirty",
        "forty",
        "fifty",
        "sixty",
        "seventy",
        "eighty",
        "ninety",
        "hundred"
    )
    private val myBelowThousandWordList =
        ArrayList<String?>(Arrays.asList<String?>(*myBelowThousandWords))
    private val myBelowThousandValuess = longArrayOf(
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        17,
        18,
        19,
        20,
        30,
        40,
        50,
        60,
        70,
        80,
        90,
        100
    )
    private val mySuffixWords = arrayOf<String?>("trillion", "billion", "million", "thousand")
    private val mySuffixValues = longArrayOf(1000000000000L, 1000000000L, 1000000L, 1000L)

    fun replaceAllWithNumbers(input: String): String {
        var result = ""
        var i = 0
        goingForward@ while (i < input.length) {
            // Start reading character by character forwards
            val goingForward = input.substring(i)
            for (j in 0..<goingForward.length) {
                // And, in each loop, character by character backwards
                val goingBackward = goingForward.substring(0, goingForward.length - j)

                // Attempt to parse words as numbers (ie: three)
                try {
                    val value = parse(goingBackward)
                    result += value

                    // This worked. Add the length of goingBackward to
                    // the init loop.
                    i += goingBackward.length - 1
                    i++
                    continue@goingForward
                } catch (e: SpellException) {
                }
            }
            result += input[i]
            i++
        }
        return result
    }

    @Throws(SpellException::class)
    fun spell(number: Long): String {
        var text: String
        text = if (number < 0L) {
            "Minus " + spell(-number, 1)
        } else {
            spell(number, 1)
        }

        val index_amp: Int = text.lastIndexOf("$")
        val index_perc: Int = text.lastIndexOf("%")

        if (index_amp >= 0) {
            if (index_perc < 0 || index_amp > index_perc) {
                val text1 = text.substring(0, index_amp)
                val text2 = text.substring(index_amp + 1, text.length)

                text = text1 + " and " + text2
            }
        }

        text = text.replace("\\$".toRegex(), ", ")
        text = text.replace("%".toRegex(), " and ")

        return text
    }

    // WithSeparator () function:
    // It converts a number to string using 1000's separator.
    // It uses a simple recursive algorithm.
    fun withSeparator(number: Long): String {
        if (number < 0) {
            return "-" + withSeparator(-number)
        }

        return if (number / 1000L > 0) {
            withSeparator(number / 1000L) + "," + String.format("%1$03d", number % 1000L)
        } else {
            String.format(Locale.US, "%1\$d", number)
        }
    }

    @Throws(SpellException::class)
    private fun spellBelow1000(number: Long): String {
        if (number < 0 || number >= 1000) throw SpellException("Expecting a number between 0 and 999: " + number)

        if (number < 20L) {
            return myTeenText!![number.toInt()]
        } else if (number < 100L) {
            val div = number.toInt() / 10
            val rem = number.toInt() % 10

            return if (rem == 0) {
                myCentText!![div - 2]
            } else {
                myCentText!![div - 2] + " " + spellBelow1000(rem.toLong())
            }
        } else {
            val div = number.toInt() / 100
            val rem = number.toInt() % 100

            return if (rem == 0) {
                myMilText!![div - 1]
            } else {
                myMilText!![div - 1] + "%" + spellBelow1000(rem.toLong())
            }
        }
    }

    @Throws(SpellException::class)
    private fun spell(number: Long, level: Int): String {
        val div = number / 1000L
        val rem = number % 1000L

        return if (div == 0L) {
            spellBelow1000(rem) + mySuffixText!![level]
        } else {
            if (rem == 0L) {
                spell(div, level + 1)
            } else {
                spell(div, level + 1) + "$" + spellBelow1000(rem) + mySuffixText!![level]
            }
        }
    }

    @Throws(SpellException::class)
    fun parseBelow1000(text: String): Long {
        var value: Long = 0
        val words: Array<String?> = text.replace(" and ".toRegex(), " ").split("\\s".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()

        for (word in words) {
            if (!myBelowThousandWordList.contains(word)) {
                throw SpellException("Unknown token : " + word)
            }

            val subval = getValueOf(word)

            if (subval == 100L) {
                if (value == 0L) value = 100
                else value *= 100
            } else value += subval
        }

        return value
    }

    private fun getValueOf(word: String?): Long {
        return myBelowThousandValuess[myBelowThousandWordList.indexOf(word)]
    }

    @Throws(SpellException::class)
    fun parse(text: String): Long {
        var text = text
        text = text.lowercase().replace("[,]".toRegex(), " ").replace(" and ".toRegex(), " ")

        var totalValue: Long = 0

        var processed = false

        for (n in mySuffixWords.indices) {
            val index = text.indexOf(mySuffixWords[n]!!)

            if (index >= 0) {
                var text1 = text.substring(0, index).trim { it <= ' ' }
                var text2 = text.substring(index + mySuffixWords[n]!!.length).trim { it <= ' ' }

                if (text1 == "") text1 = "one"

                if (text2 == "") text2 = "zero"

                totalValue = parseBelow1000(text1) * mySuffixValues[n] + parse(text2)
                processed = true
                break
            }
        }

        return if (processed) totalValue
        else parseBelow1000(text)
    }
}
