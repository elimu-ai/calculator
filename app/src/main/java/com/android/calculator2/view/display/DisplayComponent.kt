package com.android.calculator2.view.display

import android.content.Context
import android.view.View
import com.xlythe.math.Solver

/**
 * Declare a View as a component for AdvancedDisplay.
 *
 * A component is a custom view for math equations (Like matrices).
 * Register components with AdvancedDisplay to create a better UI when showing equations.
 */
interface DisplayComponent {
    /**
     * The view to display.
     *
     * Includes the equation to display.
     *
     * Includes a copy of the solver being used,
     * because the base can change (from decimal to binary for instance).
     * Useful for adding comas, or whatever else you need.
     */
    fun <T : View?, AdvancedDisplayControls> getView(
        context: Context?,
        solver: Solver?,
        equation: String?,
        listener: EventListener?
    ): T?

    /**
     * Return the text you claim is yours, but only if the equation starts with it.
     *
     * For instance, [[0],[1]]+[[1],[0]] represents 2 matrices. A MatrixView would return
     * [[0],[1]] because that's 1 matrix.
     */
    fun parse(equation: String?): String?
}
