/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.calculator2

import ai.elimu.calculator.R
import com.xlythe.math.Base
import com.xlythe.math.Solver
import org.javia.arity.SyntaxException

class CalculatorExpressionEvaluator(tokenizer: CalculatorExpressionTokenizer) {
    val solver: Solver = Solver()
    private val mTokenizer: CalculatorExpressionTokenizer = tokenizer

    fun evaluate(expr: CharSequence, callback: EvaluateCallback) {
        evaluate(expr.toString(), callback)
    }

    fun evaluate(expr: String, callback: EvaluateCallback) {
        var expr = expr
        expr = mTokenizer.getNormalizedExpression(expr)

        // remove any trailing operators
        while (expr.isNotEmpty() && "+-/*".indexOf(expr[expr.length - 1]) != -1) {
            expr = expr.substring(0, expr.length - 1)
        }

        try {
            if (expr.isEmpty() || expr.toDouble() != null) {
                callback.onEvaluate(expr, null, Calculator.INVALID_RES_ID)
                return
            }
        } catch (e: NumberFormatException) {
            // expr is not a simple number
        }

        try {
            val result = solver.solve(expr) ?: ""
            callback.onEvaluate(
                expr,
                mTokenizer.getLocalizedExpression(result),
                Calculator.INVALID_RES_ID
            )
        } catch (e: SyntaxException) {
            callback.onEvaluate(expr, null, R.string.error_syntax)
        }
    }

    fun setBase(expr: String, base: Base, callback: EvaluateCallback) {
        try {
            val result = solver.baseModule.setBase(expr, base)
            callback.onEvaluate(expr, result, Calculator.INVALID_RES_ID)
        } catch (e: SyntaxException) {
            callback.onEvaluate(expr, null, R.string.error_syntax)
        }
    }

    interface EvaluateCallback {
        fun onEvaluate(expr: String?, result: String?, errorResourceId: Int)
    }
}