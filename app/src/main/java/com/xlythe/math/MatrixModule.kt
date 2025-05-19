package com.xlythe.math

import org.ejml.simple.SimpleEVD
import org.ejml.simple.SimpleMatrix
import org.ejml.simple.SimpleSVD
import org.javia.arity.SyntaxException
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class MatrixModule internal constructor(solver: Solver?) : Module(solver) {
    @Throws(SyntaxException::class)
    fun gatherScalar(text: String): Double {
        if (!Character.isDigit(text[1])) throw SyntaxException()
        return text.substring(1).toDouble()
    }

    // The following have a lot of repeated boilerplate code.
    // Condensing it down would require language features/properties
    // that Java does not have.
    // In short, Java is not F#.
    @Throws(SyntaxException::class)
    private fun calculate(input: String): String {
        // I never realized negative numbers could be so difficult.
        var input = input
        input = input.replace(Constants.MINUS, '-')

        // All remaining instances of U+2212 will be on negative numbers.
        // They will be counted as whole tokens.

        // Instantiate matrices first.
        var m = Pattern.compile("\\[\\[.+?\\]\\]").matcher(input)
        while (m.find()) {
            val temp = parseMatrix(m.group())
            input = input.replace(m.group(), printMatrix(temp))
        }

        // Get percentage.
        input = input.replace("(?<=\\d)%(?!\\d)".toRegex(), "\u00d70.01")

        // Might as well get factorial too.
        m = Pattern.compile("(?<!\\.)([0-9]+)\\!").matcher(input)
        while (m.find()) {
            val temp = m.group(1).toInt()
            input = input.replace(m.group(), fact(temp))
        }

        var open = 0
        for (i in 0..<input.length) {
            if (input[i] == '(') open++
            else if (input[i] == ')') open--
        }
        if (open == 1) input = "$input)" // Auto-balance if possible
        else if (open != 0) throw SyntaxException() // Unbalanced


        val pat = Pattern.compile("\\(([^\\(\\)]+?)\\)")
        while (input.contains("(")) {
            val mch = pat.matcher(input)
            while (mch.find()) {
                input = input.replace(mch.group(), calculate(mch.group(1)))
            }
        }

        // Process transpositions.
        var match = Pattern.compile("(\\[.+\\])\\^T").matcher(input)
        while (match.find()) {
            val temp = parseMatrix(match.group(1)).transpose()
            input = input.replace(match.group(), printMatrix(temp))
        }

        // Process inverses
        match = Pattern.compile("(\\[.+\\])\uFEFF\\^-1").matcher(input)
        while (match.find()) {
            val temp = parseMatrix(match.group(1)).pseudoInverse()
            input = input.replace(match.group(), printMatrix(temp))
        }

        // Handle functions.
        match =
            Pattern.compile("(\u221a|cbrt|log|ln|asin|acos|atan|sind|cosd|tand|asind|acosd|atand|sin|cos|tan|det)(\u2212?\\d+(?:\\.\\d+)?|\\[\\[.+\\]\\])")
                .matcher(input)
        while (match.find()) {
            val res = applyFunc(match.group(1), match.group(2))
            input = input.replace(match.group(), res)
        }

        // Functions might generate NaN. Return error if so.
        if (input.contains("NaN")) throw SyntaxException()

        // Substitute e
        // input = input.replaceAll("(?<!\\d)e", "2.7182818284590452353");
        input = input.replace("(?<!\\d)(e)(?!\\d)".toRegex(), "2.7182818284590452353")
        // Sub pi
        input = input.replace("\u03c0", "3.1415926535897932384626")

        // Split into seperate arrays of operators and operands.
        // Operator 0 applies to operands 0 and 1, and so on
        val parts: Array<String?> =
            input.split("\u00d7|\\+|(?<=\\d|\\])(?<=\\d|\\])-|\u00f7|\\^".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        val ops: CharArray = opSplit(input)

        // This never changes, so no need to keep calling it
        val N = ops.size

        // If there are no ops, there's nothing to do
        // Since we've already made substitutions and parsed parentheses
        if (N == 0) return input

        // Fill in the pieces.
        // Store everything as Object, and cast out later
        val pieces = arrayOfNulls<Any>(parts.size)
        for (i in parts.indices) {
            if (parts[i]!!.startsWith("[[")) {
                pieces[i] = parseMatrix(parts[i]!!)
            } else {
                pieces[i] = solver!!.solve(parts[i]!!)!!.toDouble()
            }
        }

        // Work on the operators in order of their precedence.

        // Go from right to left to make ^ chains right-associative.
        for (i in N - 1 downTo 0) {
            var landr: IntArray? = null
            if (ops[i] == '^') {
                landr = lookAfield(pieces, i)
                val l = landr!![0]
                val r = landr[1]
                val res = applyPow(pieces[l], pieces[r])

                pieces[l] = res // This is arbitrary (I think)
                pieces[r] = null // I could also have put the result in right
                // and null in left
            }
        }

        // Yes, I'm doing a complete loop over all operators several times.
        // Realistically, there will only be a few of them.
        // For the purposes of this app, it's no big deal.
        for (i in 0..<N) {
            var landr: IntArray? = null
            if (ops[i] == Constants.MUL || ops[i] == Constants.DIV) {
                landr = lookAfield(pieces, i)
                val l = landr!![0]
                val r = landr[1]
                var res: Any? = null
                res = if (ops[i] == Constants.MUL) applyMult(pieces[l], pieces[r])
                else applyDiv(pieces[l], pieces[r])

                // else res = applyMod(pieces[l], pieces[r]);
                pieces[l] = res
                pieces[r] = null
            }
        }

        for (i in 0..<N) {
            var landr: IntArray? = null
            if (ops[i] == '+' || ops[i] == '-') {
                landr = lookAfield(pieces, i)
                val l = landr!![0]
                val r = landr[1]
                var res: Any? = null
                res = if (ops[i] == '+') applyPlus(pieces[l], pieces[r])
                else applySub(pieces[l], pieces[r])

                pieces[l] = res
                pieces[r] = null
            }
        }

        for (piece in pieces) if (piece != null) {
            return if (piece is Double) numToString(piece)
            else if (piece is SimpleMatrix) printMatrix(piece)
            else throw SyntaxException() // Neither matrix nor double

            // should never happen
        }
        throw RuntimeException() // Getting here should be impossible
    } // end main

    @Throws(SyntaxException::class)
    fun evaluateMatrices(text: String): String? {
        var text = text
        text = solver!!.convertToDecimal(text)!!
        val result = calculate(text).replace('-', Constants.MINUS)

        return solver.baseModule.updateTextToNewMode(
            result, Base.DECIMAL,
            solver.baseModule.base
        )
    }

    @Throws(SyntaxException::class)
    private fun applyFunc(func: String, arg: String): String {
        var arg = arg
        arg = arg.replace(Constants.MINUS, '-')
        val DEG = Math.PI / 180.0
        if (func == "\u221a")  // sqrt
        {
            if (arg.startsWith("[[")) {
                val matrix = parseMatrix(arg)
                val m = matrix.numRows()
                val n = matrix.numCols()
                if (m != n) throw SyntaxException()
                val decomp = SimpleEVD<SimpleMatrix>(matrix.matrix)
                val evals = DoubleArray(m)
                for (i1 in 0..<m) {
                    evals[i1] = sqrt(decomp.getEigenvalue(i1).magnitude)
                }
                val D = SimpleMatrix.diag(*evals)
                val V = SimpleMatrix(m, n)
                for (k in 0..<m) {
                    val col = decomp.getEigenVector(k)
                    for (l in 0..<n) {
                        V.set(k, l, col.get(l, 0))
                    }
                }
                var temp = V.mult(D)
                temp = temp.mult(V.invert())
                return printMatrix(temp)
            } else return numToString(sqrt(arg.toDouble()))
        } else if (func == "cbrt") {
            if (arg.startsWith("[[")) {
                val matrix = parseMatrix(arg)
                val m = matrix.numRows()
                val n = matrix.numCols()
                if (m != n) throw SyntaxException()
                val decomp = SimpleEVD<SimpleMatrix>(matrix.matrix)
                val evals = DoubleArray(m)
                for (i1 in 0..<m) {
                    evals[i1] = cbrt(decomp.getEigenvalue(i1).magnitude)
                }
                val D = SimpleMatrix.diag(*evals)
                val V = SimpleMatrix(m, n)
                for (k in 0..<m) {
                    val col = decomp.getEigenVector(k)
                    for (l in 0..<n) {
                        V.set(k, l, col.get(l, 0))
                    }
                }
                var temp = V.mult(D)
                temp = temp.mult(V.invert())
                return printMatrix(temp)
            } else return numToString(cbrt(arg.toDouble()))
        } else if (func == "sin") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(i, j, sin(m.get(i, j)))
                return printMatrix(m)
            } else return numToString(sin(arg.toDouble()))
        } else if (func == "cos") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(i, j, cos(m.get(i, j)))
                return printMatrix(m)
            } else return numToString(cos(arg.toDouble()))
        } else if (func == "tan") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(i, j, tan(m.get(i, j)))
                return printMatrix(m)
            } else return numToString(tan(arg.toDouble()))
        } else if (func == "sind") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(
                    i,
                    j,
                    sin(m.get(i, j) * DEG)
                )
                return printMatrix(m)
            } else return numToString(sin(arg.toDouble() * DEG))
        } else if (func == "cosd") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(
                    i,
                    j,
                    cos(m.get(i, j) * DEG)
                )
                return printMatrix(m)
            } else return numToString(cos(arg.toDouble() * DEG))
        } else if (func == "tand") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(
                    i,
                    j,
                    tan(m.get(i, j) * DEG)
                )
                return printMatrix(m)
            } else return numToString(tan(arg.toDouble() * DEG))
        } else if (func == "asind") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(
                    i,
                    j,
                    asin(m.get(i, j) / DEG)
                )
                return printMatrix(m)
            } else return numToString(asin(arg.toDouble()) / DEG)
        } else if (func == "acosd") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(
                    i,
                    j,
                    acos(m.get(i, j)) / DEG
                )
                return printMatrix(m)
            } else return numToString(acos(arg.toDouble()) / DEG)
        } else if (func == "atand") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(
                    i,
                    j,
                    atan(m.get(i, j)) / DEG
                )
                return printMatrix(m)
            } else return numToString(atan(arg.toDouble()) / DEG)
        } else if (func == "log") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(
                    i,
                    j,
                    log10(m.get(i, j))
                )
                return printMatrix(m)
            } else return numToString(log10(arg.toDouble()))
        } else if (func == "ln") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(i, j, ln(m.get(i, j)))
                return printMatrix(m)
            } else return numToString(ln(arg.toDouble()))
        } else if (func == "asin") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(i, j, asin(m.get(i, j)))
                return printMatrix(m)
            } else return numToString(asin(arg.toDouble()))
        } else if (func == "acos") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(i, j, acos(m.get(i, j)))
                return printMatrix(m)
            } else return numToString(acos(arg.toDouble()))
        } else if (func == "atan") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                for (i in 0..<m.numRows()) for (j in 0..<m.numCols()) m.set(i, j, atan(m.get(i, j)))
                return printMatrix(m)
            } else return numToString(atan(arg.toDouble()))
        } else if (func == "det") {
            if (arg.startsWith("[[")) {
                val m = parseMatrix(arg)
                if (m.numCols() != m.numRows()) throw SyntaxException()
                val d = m.determinant()
                return numToString(d)
            } else return arg // Determinant of a scalar is equivalent to det. of

            // 1x1 matrix, which is the matrix' one element
        } else throw SyntaxException()
    }

    @Throws(SyntaxException::class)
    private fun applyPow(l: Any?, r: Any?): Any? {
        if (l is SimpleMatrix && r is SimpleMatrix) throw SyntaxException()
        else if (l is SimpleMatrix) {
            val a = l
            val m = a.numRows()
            val n = a.numCols()
            if (m != n) throw SyntaxException()
            val b = r as Double
            if (b > floor(b)) {
                val decomp = SimpleSVD<SimpleMatrix>(a.matrix, false)
                val S = decomp.getW()
                for (i1 in 0..<m) {
                    for (j in 0..<n) {
                        val arg = S.get(i1, j)
                        S.set(i1, j, arg.pow(b))
                    }
                }
                var matrix = decomp.getU().mult(S)
                matrix = matrix.mult(decomp.getV().transpose())
                return matrix
            } else {
                val equiv = Math.round(b)
                var a1: SimpleMatrix = a
                for (e in 1..<equiv) a1 = a1.mult(a)

                return a1
            }
        } else if (r is SimpleMatrix) {
            val a = r
            val m = a.numRows()
            val n = a.numCols()
            if (m != n) throw SyntaxException()
            val b = l as Double
            if (b > floor(b)) {
                val decomp = SimpleSVD<SimpleMatrix>(a.matrix, false)
                val S = decomp.getW()
                for (i1 in 0..<m) {
                    for (j in 0..<n) {
                        val arg = S.get(i1, j)
                        S.set(i1, j, arg.pow(b))
                    }
                }
                var matrix = decomp.getU().mult(S)
                matrix = matrix.mult(decomp.getV().transpose())
                return matrix
            } else {
                val equiv = Math.round(b)
                var a1: SimpleMatrix = a
                for (e in 1..<equiv) a1 = a1.mult(a)

                return a1
            }
        } else {
            val a = l as Double
            val b = r as Double
            return a.pow(b)
        }
    }

    @Throws(SyntaxException::class)
    private fun applyMult(l: Any?, r: Any?): Any? {
        if (l is SimpleMatrix && r is SimpleMatrix) {
            val a = l
            val b = r
            return a.mult(b)
        } else if (l is SimpleMatrix) {
            val a = l
            val b = r as Double
            return a.scale(b)
        } else if (r is SimpleMatrix) {
            val a = r
            val b = l as Double
            return a.scale(b)
        } else {
            val a = l as Double
            val b = r as Double
            return a * b
        }
    }

    @Throws(SyntaxException::class)
    private fun applyDiv(l: Any?, r: Any?): Any? {
        if (l is SimpleMatrix && r is SimpleMatrix) {
            val a = l
            val b = r
            return a.mult(b.pseudoInverse())
        } else if (l is SimpleMatrix) {
            val a = l
            val b = r as Double
            return a.scale(1.0 / b)
        } else if (r is SimpleMatrix) {
            val a = r
            val b = l as Double
            return a.pseudoInverse().scale(b)
        } else {
            val a = l as Double
            val b = r as Double
            return a / b
        }
    }

    // private Object applyMod(Object object, Object object2) throws
    // SyntaxException {
    // if(object instanceof Double && object2 instanceof Double) {
    // double arg1 = (Double) object;
    // double arg2 = (Double) object2;
    // return arg1 % arg2;
    // }
    // else throw new SyntaxException();
    // }
    @Throws(SyntaxException::class)
    private fun applyPlus(l: Any?, r: Any?): Any? {
        if (l is SimpleMatrix && r is SimpleMatrix) {
            val a = l
            val b = r
            return a.plus(b)
        } else if (l is SimpleMatrix) {
            val a = l
            val b = r as Double
            return addScalar(a, b)
        } else if (r is SimpleMatrix) {
            val a = r
            val b = l as Double
            return addScalar(a, b)
        } else {
            val a = l as Double
            val b = r as Double
            return a + b
        }
    }

    @Throws(SyntaxException::class)
    private fun applySub(l: Any?, r: Any?): Any? {
        if (l is SimpleMatrix && r is SimpleMatrix) {
            val a = l
            val b = r
            return a.minus(b)
        } else if (l is SimpleMatrix) {
            val a = l
            val b = r as Double
            return addScalar(a, -b)
        } else if (r is SimpleMatrix) {
            val a = r
            val b = l as Double
            return addScalar(a, -b)
        } else {
            val a = l as Double
            val b = r as Double
            return a - b
        }
    }

    @Throws(SyntaxException::class)
    private fun parseMatrix(text: String): SimpleMatrix {
        // Count rows & cols
        val interior = text.substring(2, text.length - 2)
        val rows: Array<String?> =
            interior.split("\\]\\[".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val temp = SimpleMatrix(
            rows.size,
            rows[0]!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size
        )

        for (i in rows.indices) {
            val cols: Array<String?> =
                rows[i]!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (cols.size == 0) throw SyntaxException()
            for (j in cols.indices) {
                if (cols[j]!!.isEmpty()) throw SyntaxException()
                temp.set(i, j, calculate(cols[j]!!).toDouble())
            }
        }

        return temp
    }

    fun isMatrix(text: String): Boolean {
        val separator = matrixSeparator.toString() + ""
        val decimal = decimalPoint.toString() + ""
        val validMatrix = (".*\\[(\\[[\u2212-]?[A-F0-9]*(" + Pattern.quote(decimal)
                + "[A-F0-9]*)?(" + Pattern.quote(separator) + "[\u2212-]?[A-F0-9]*("
                + Pattern.quote(decimal) + "[A-F0-9]*)?)*\\])+\\].*")
        return text.matches(validMatrix.toRegex())
    }

    companion object {
        fun addScalar(mat: SimpleMatrix, scalar: Double): SimpleMatrix {
            val temp = mat.copy()
            val M = mat.numRows()
            val N = mat.numCols()
            for (i in 0..<M) {
                for (j in 0..<N) temp.set(i, j, mat.get(i, j) + scalar)
            }
            return temp
        }

        private fun opSplit(str: String): CharArray {
            val buffer = StringBuilder()
            var c: Char
            var prev: Char
            prev = str[0]
            for (i in 0..<str.length) {
                c = str[i]
                if (c == '^' || c == Constants.MUL || c == Constants.DIV || c == '+') buffer.append(
                    c
                )
                else if (c == '-' && (Character.isDigit(prev) || prev == ']') && (prev != 'e')) buffer.append(
                    c
                )
                prev = c
            }

            return buffer.toString().toCharArray()
        }

        // Look for the nearest valid operand
        private fun lookAfield(field: Array<Any?>, orig: Int): IntArray? {
            val left: Int
            val right: Int

            // Start with the original position (of the operator)
            // Left operand is at the same index as its operator
            // But if it's null, look farther left
            var pos = orig
            while (field[pos] == null)  //
                pos--
            left = pos
            // Right operand is one greater than the operator index
            pos = orig + 1
            while (field[pos] == null)  // Look to the right if null
                pos++
            right = pos // Found it

            return intArrayOf(left, right) // Return the indices to allow later
            // sub-in of null
        }

        private fun fact(n: Int): String {
            var m = n.toLong()
            for (i in n - 1 downTo 2) m *= i.toLong()

            return m.toString()
        }

        private fun numToString(arg: Double): String {
            // Cut off very small arguments
            if (abs(arg) < 1.0E-10) return "0"

            var temp = arg.toString().replace('E', 'e')
            if (temp.endsWith(".0")) temp = temp.substring(0, temp.length - 2)
            return temp
        }

        private fun printMatrix(mat: SimpleMatrix): String {
            val buffer = StringBuilder("[")
            val m = mat.numRows()
            val n = mat.numCols()
            for (i in 0..<m) {
                buffer.append('[')
                for (j in 0..<n) {
                    buffer.append(numToString(mat.get(i, j)))
                    if (j != n - 1) buffer.append(',')
                }
                buffer.append(']')
            }
            buffer.append(']')

            return buffer.toString()
        }
    }
}
