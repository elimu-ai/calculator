package com.xlythe.math

import android.content.Context
import android.util.Log
import java.lang.reflect.Field
import java.util.Locale

/**
 * Translates an equation typed in the default locale back into English
 *
 * This assumes the app has English translations
 */
class Localizer(context: Context, r: Class<*>) {
    private val mMap: MutableMap<String?, String?> = HashMap<String?, String?>()
    private val mUseDegrees = false

    init {
        buildResourceMap(context, r)
    }

    fun buildResourceMap(context: Context, r: Class<*>) {
        try {
            Log.d("Localizer", "Building resource map")
            val color = Class.forName(r.getName() + "\$string")
            for (f in color.getFields()) {
                if (detect(context, f, "asin")) ; else if (detect(
                        context,
                        f,
                        "acos"
                    )
                ) ; else if (detect(context, f, "atan")) ; else if (detect(
                        context,
                        f,
                        "sin"
                    )
                ) ; else if (detect(context, f, "cos")) ; else if (detect(
                        context,
                        f,
                        "tan"
                    )
                ) ; else if (detect(context, f, "log")) ; else if (detect(
                        context,
                        f,
                        "ln"
                    )
                ) ; else if (detect(context, f, "det")) ; else if (detect(
                        context,
                        f,
                        "cbrt"
                    )
                ) ; else if (f.getName().lowercase(Locale.getDefault())
                        .contains("dot") || f.getName().lowercase(Locale.getDefault())
                        .contains("decimal")
                ) {
                    mMap.put(".", context.getString(f.getInt(null)))
                } else if (f.getName().lowercase(Locale.getDefault())
                        .contains("matrix") && f.getName().lowercase(Locale.getDefault())
                        .contains("separator")
                ) {
                    mMap.put(",", context.getString(f.getInt(null)))
                }
            }
            Log.d("Localizer", "strings loaded")
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            // Do nothing. Happens if no Strings are found.
        }
    }

    @Throws(IllegalAccessException::class)
    private fun detect(context: Context, f: Field, phrase: String): Boolean {
        if (f.getName().lowercase(Locale.getDefault()).contains(phrase)) {
            mMap.put(phrase, context.getString(f.getInt(null)))
            return true
        }
        return false
    }

    /**
     * Localize the input into English
     *
     * Used because the math library only understands English.
     */
    fun localize(input: String): String {
        // Delocalize functions (e.g. Spanish localizes "sin" as "sen").
        // Order matters for arc functions
        var input = input
        input = translate(input, "asin")!!
        input = translate(input, "acos")!!
        input = translate(input, "atan")!!
        input = translate(input, "sin")!!
        input = translate(input, "cos")!!
        input = translate(input, "tan")!!
        if (mUseDegrees) {
            input = input.replace("sin", "sind")
            input = input.replace("cos", "cosd")
            input = input.replace("tan", "tand")
        }
        input = translate(input, "log")!!
        input = translate(input, "ln")!!
        input = translate(input, "det")!!
        input = translate(input, "sqrt")!!
        input = translate(input, "cbrt")!!
        input = translate(input, ".")!!
        input = translate(input, ",")!!
        return input
    }

    /**
     * Localize the input to the user's original locale
     *
     * We only care about comas and periods because, by now, the math problem should be solved.
     */
    fun relocalize(input: String): String {
        var input = input
        input = retranslate(input, ",")!!
        input = retranslate(input, ".")!!
        return input
    }

    /**
     * Checks if a word has a translation.
     * If so, replaces the sentence with the English word.
     */
    private fun translate(sentence: String, word: String): String? {
        if (mMap.get(word) != null) {
            return sentence.replace(mMap.get(word)!!, word)
        }
        return sentence
    }

    /**
     * Checks if a word has a translation.
     * If so, replaces the sentence with the localized word.
     */
    private fun retranslate(sentence: String, word: String): String? {
        if (mMap.get(word) != null) {
            return sentence.replace(word, mMap.get(word)!!)
        }
        return sentence
    }
}
