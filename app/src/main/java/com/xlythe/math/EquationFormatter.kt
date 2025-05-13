package com.xlythe.math

class EquationFormatter {
    /**
     * Append parenthesis at the end of unclosed functions
     *
     * ie. sin(90 becomes sin(90)
     */
    fun appendParenthesis(input: String): String {
        val formattedInput = StringBuilder(input)

        var unclosedParen = 0
        for (i in 0..<formattedInput.length) {
            if (formattedInput.get(i) == Constants.LEFT_PAREN) unclosedParen++
            else if (formattedInput.get(i) == Constants.RIGHT_PAREN) unclosedParen--
        }
        for (i in 0..<unclosedParen) {
            formattedInput.append(Constants.RIGHT_PAREN)
        }
        return formattedInput.toString()
    }

    /**
     * Insert html superscripts so that exponents appear properly.
     *
     * ie. 2^3 becomes 2<sup>3</sup>
     */
    fun insertSupScripts(input: String): String {
        val formattedInput = StringBuilder()

        var sub_open = 0
        var sub_closed = 0
        var paren_open = 0
        var paren_closed = 0
        for (i in 0..<input.length) {
            val c = input.get(i)
            if (c == Constants.POWER) {
                formattedInput.append("<sup>")
                if (sub_open == 0) formattedInput.append("<small>")
                sub_open++
                if (i + 1 == input.length) {
                    formattedInput.append(c)
                    if (sub_closed == 0) formattedInput.append("</small>")
                    formattedInput.append("</sup>")
                    sub_closed++
                } else {
                    formattedInput.append(Constants.PLACEHOLDER)
                }
                continue
            }

            if (sub_open > sub_closed) {
                if (paren_open == paren_closed) {
                    // Decide when to break the <sup> started by ^
                    if (c == Constants.PLUS // 2^3+1
                        || (c == Constants.MINUS && input.get(i - 1) != Constants.POWER) // 2^3-1
                        || c == Constants.MUL // 2^3*1
                        || c == Constants.DIV // 2^3/1
                        || c == Constants.EQUAL // X^3=1
                        || (c == Constants.LEFT_PAREN && (Solver.isDigit(input.get(i - 1)) || input.get(
                            i - 1
                        ) == Constants.RIGHT_PAREN)) // 2^3(1)
                        // or
                        // 2^(3-1)(0)
                        || (Solver.isDigit(c) && input.get(i - 1) == Constants.RIGHT_PAREN) // 2^(3)1
                        || (!Solver.isDigit(c) && Solver.isDigit(input.get(i - 1))) && c != Constants.DECIMAL_POINT
                    ) { // 2^3log(1)
                        while (sub_open > sub_closed) {
                            if (sub_closed == 0) formattedInput.append("</small>")
                            formattedInput.append("</sup>")
                            sub_closed++
                        }
                        sub_open = 0
                        sub_closed = 0
                        paren_open = 0
                        paren_closed = 0
                        if (c == Constants.LEFT_PAREN) {
                            paren_open--
                        } else if (c == Constants.RIGHT_PAREN) {
                            paren_closed--
                        }
                    }
                }
                if (c == Constants.LEFT_PAREN) {
                    paren_open++
                } else if (c == Constants.RIGHT_PAREN) {
                    paren_closed++
                }
            }
            formattedInput.append(c)
        }
        while (sub_open > sub_closed) {
            if (sub_closed == 0) formattedInput.append("</small>")
            formattedInput.append("</sup>")
            sub_closed++
        }
        return formattedInput.toString()
    }

    /**
     * Add comas to an equation or result
     *
     * 12345 becomes 12,345
     *
     * 10101010 becomes 1010 1010
     *
     * ABCDEF becomes AB CD EF
     */
    fun addComas(solver: Solver, text: String): String {
        return addComas(solver, text, -1).replace(BaseModule.SELECTION_HANDLE.toString() + "", "")
    }


    /**
     * Add comas to an equation or result.
     * A temp character (BaseModule.SELECTION_HANDLE) will be added
     * where the selection handle should be.
     *
     * 12345 becomes 12,345
     *
     * 10101010 becomes 1010 1010
     *
     * ABCDEF becomes AB CD EF
     */
    fun addComas(solver: Solver, text: String, selectionHandle: Int): String {
        return solver.baseModule.groupSentence(text, selectionHandle)
    }

    fun format(solver: Solver, text: String): String {
        return appendParenthesis(insertSupScripts(addComas(solver, text)))
    }
}
