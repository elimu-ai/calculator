package com.xlythe.math

import java.util.LinkedList
import java.util.Locale
import java.util.regex.Pattern

/**
 * Parse spoken text (eg. sign as sin) or speak spoken text (eg. *  as multiplied by)
 */
object Voice {
    fun parseSpokenText(text: String): String {
        var text = text
        if (Locale.getDefault() == Locale.ENGLISH) {
            val formatter = EquationFormatter()
            val exceptions: MutableList<String> = LinkedList<String>()
            text = text.lowercase()
            text = text.replace("percent", "%")
            text = text.replace("point", ".")
            text = text.replace("minus", "-")
            text = text.replace("plus", "+")
            text = text.replace("divided", "/")
            text = text.replace("over", "/")
            text = text.replace("times", "*")
            text = text.replace("x", "*")
            text = text.replace("multiplied", "*")
            text = text.replace("raise", "^")
            text = text.replace("square root", "sqrt(")
            exceptions.add("sqrt")
            text = text.replace("sign", "sin(")
            exceptions.add("sin")
            text = text.replace("cosine", "cos(")
            exceptions.add("cos")
            text = text.replace("tangent", "tan(")
            exceptions.add("tan")
            text = text.replace("pie", "\u03C0")
            text = text.replace("pi", "\u03C0")
            text = text.replace(" ", "")
            text = SpellContext.replaceAllWithNumbers(text)
            text = removeChars(text, exceptions)
            text = formatter.appendParenthesis(text)
            return text
        } else {
            return text
        }
    }

    private fun removeChars(input: String, exceptions: MutableList<String>): String {
        val pattern = Pattern.compile("[a-z']")
        var text = ""
        var i = 0
        while (i < input.length) {
            for (ex in exceptions) {
                if (input.substring(i).startsWith(ex)) {
                    text += input.substring(i, i + ex.length)
                    i += ex.length
                    continue
                }
            }

            // Check for characters that don't belong
            val matcher = pattern.matcher(input.substring(i, i + 1))
            if (!matcher.matches()) text += input.substring(i, i + 1)
            i++
        }
        return text
    }

    fun createSpokenText(text: String): String {
        var text = text
        if (Locale.getDefault() == Locale.ENGLISH) {
            if (text.startsWith(Constants.MINUS.toString())) {
                // Speech can't say "-1". It says "1" instead.
                text = "Negative " + text.substring(1)
            }
            text = text.replace("-", " minus ")
            text = text.replace("*", " times ")
            text = text.replace("*", " times ")
            text = text.replace("sin", " sine of ")
            text = text.replace("cos", " cosine of ")
            text = text.replace("tan", " tangent of ")
            text = text.replace("sqrt", " square root of ")
            text = text.replace("^", " raised to ")
            return text
        } else {
            return text
        }
    }
}
