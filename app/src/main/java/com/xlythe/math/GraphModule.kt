package com.xlythe.math

import android.os.AsyncTask
import org.javia.arity.SyntaxException
import java.util.LinkedList

class GraphModule(solver: Solver?) : Module(solver) {
    private var mGraphTask: GraphTask? = null
    private var mMinY = 0.0
    private var mMaxY = 0.0
    private var mMinX = 0.0
    private var mMaxX = 0.0
    private var mZoomLevel = 0f

    fun setRange(min: Float, max: Float) {
        mMinY = min.toDouble()
        mMaxY = max.toDouble()
    }

    fun setDomain(min: Float, max: Float) {
        mMinX = min.toDouble()
        mMaxX = max.toDouble()
    }

    fun setZoomLevel(level: Float) {
        mZoomLevel = level
    }

    fun updateGraph(text: String, l: OnGraphUpdatedListener) {
        val endsWithOperator = text.length != 0 &&
                (Solver.isOperator(text.get(text.length - 1)) || text.endsWith("("))
        val containsMatrices = solver!!.displayContainsMatrices(text)
        if (endsWithOperator || containsMatrices) {
            return
        }

        if (mGraphTask != null) mGraphTask!!.cancel(true)
        mGraphTask = GraphTask(solver, mMinY, mMaxY, mMinX, mMaxX, mZoomLevel, l)
        mGraphTask!!.execute(text)
    }

    internal inner class GraphTask(
        private val mSolver: Solver,
        private val mMinY: Double,
        private val mMaxY: Double,
        private val mMinX: Double,
        private val mMaxX: Double,
        private val mZoomLevel: Float,
        private val mListener: OnGraphUpdatedListener
    ) : AsyncTask<String?, String?, MutableList<Point?>?>() {
        override fun doInBackground(vararg eq: String?): MutableList<Point?>? {
            try {
                return graph(
                    mSolver.getBaseModule().updateTextToNewMode(
                        eq[0],
                        mSolver.getBaseModule().getBase(), Base.DECIMAL
                    )
                )
            } catch (e: SyntaxException) {
                cancel(true)
                return null
            }
        }

        fun graph(equation: String?): MutableList<Point?>? {
            val series = LinkedList<Point?>()

            mSolver.mSymbols.pushFrame()
            var x = mMinX
            while (x <= mMaxX) {
                if (isCancelled()) {
                    return null
                }

                try {
                    mSolver.mSymbols.define("X", x)
                    val y = mSolver.mSymbols.eval(equation)
                    series.add(Point(x, y))
                } catch (e: SyntaxException) {
                }
                x += 0.01 * mZoomLevel
            }
            mSolver.mSymbols.popFrame()

            return series
        }

        override fun onPostExecute(result: MutableList<Point?>?) {
            mListener.onGraphUpdated(result)
        }
    }

    interface OnGraphUpdatedListener {
        fun onGraphUpdated(result: MutableList<Point?>?)
    }
}
