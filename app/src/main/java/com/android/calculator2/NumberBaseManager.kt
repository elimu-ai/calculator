package com.android.calculator2

import ai.elimu.calculator.R
import com.xlythe.math.Base
import java.util.Arrays

/**
 * Keeps track of the application wide number base, and manages the IDs of views to disable
 * when changing base.
 */
class NumberBaseManager(var numberBase: Base) {
    private val mDisabledViewIds: MutableMap<Base?, MutableSet<Int?>?>

    /**
     * @return the set of view resource IDs managed by the enabled/disabled list
     */
    val viewIds: MutableSet<Int> by lazy { hashSetOf() }

    init {
        val hexList =
            Arrays.asList<Int?>(R.id.A, R.id.B, R.id.C, R.id.D, R.id.E, R.id.F)

        val binaryList =
            Arrays.asList<Int?>(
                R.id.digit2, R.id.digit3, R.id.digit4, R.id.digit5, R.id.digit6,
                R.id.digit7, R.id.digit8, R.id.digit9
            )

        mDisabledViewIds = HashMap<Base?, MutableSet<Int?>?>()
        mDisabledViewIds.put(Base.DECIMAL, HashSet<Int?>(hexList))
        val disabledForBinary: MutableSet<Int?> = HashSet<Int?>(binaryList)
        disabledForBinary.addAll(hexList)
        mDisabledViewIds.put(Base.BINARY, disabledForBinary)
        mDisabledViewIds.put(Base.HEXADECIMAL, HashSet<Int?>())

        viewIds.addAll(binaryList)
        viewIds.addAll(hexList)
    }

    /**
     * return true if the given view is disabled based on the current base
     *
     * @param viewResId
     * @return
     */
    fun isViewDisabled(viewResId: Int): Boolean {
        val disabledSet = mDisabledViewIds.get(this.numberBase)
        return disabledSet!!.contains(viewResId)
    }
}
