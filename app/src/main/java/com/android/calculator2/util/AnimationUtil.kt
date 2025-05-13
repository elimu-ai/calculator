package com.android.calculator2.util

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.ScaleAnimation
import androidx.core.view.isVisible

/**
 * Utility for transition animations
 */
object AnimationUtil {
    const val DEFAULT_FADE_DURATION: Int = 200
    const val DEFAULT_SHRINK_GROW_DURATION: Int = 200

    /**
     * Makes view visible and transitions alpha from 0 to 1.  Does nothing if view is
     * already visible.
     *
     * @param view
     * @param duration
     */
    /**
     * Fade in with default duration
     *
     * @param view
     */
    @JvmOverloads
    fun fadeIn(view: View, duration: Int = DEFAULT_FADE_DURATION) {
        if (view.isVisible) {
            return
        }
        view.setAlpha(0f)
        view.visibility = View.VISIBLE
        val anim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        anim.setDuration(duration.toLong())
        anim.start()
    }

    /**
     * Transitions alpha from 1 to 0 and then sets visibility to gone
     *
     * @param view
     * @param duration
     */
    /**
     * Fade out with default duration
     *
     * @param view
     */
    @JvmOverloads
    fun fadeOut(view: View, duration: Int = DEFAULT_FADE_DURATION) {
        val anim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        })
        anim.setDuration(DEFAULT_FADE_DURATION.toLong())
        anim.start()
    }

    /**
     * Shrink view1 and then grow view2
     *
     * @param view1 view to shrink
     * @param view2 view to grow
     * @param duration duration for each phase of the animation
     */
    /**
     * Shrink and grow with default duration
     *
     * @param view1
     * @param view2
     */
    @JvmOverloads
    fun shrinkAndGrow(view1: View, view2: View, duration: Int = DEFAULT_SHRINK_GROW_DURATION) {
        val shrinkAnim =
            ScaleAnimation(
                1f,
                0f,
                1f,
                0f,
                (view1.width / 2).toFloat(),
                (view1.height / 2).toFloat()
            )
        val growAnim =
            ScaleAnimation(
                0f,
                1f,
                0f,
                1f,
                (view2.width / 2).toFloat(),
                (view2.height / 2).toFloat()
            )
        shrinkAnim.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                view1.visibility = View.INVISIBLE
                view2.visibility = View.VISIBLE
                view2.startAnimation(growAnim)
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })

        shrinkAnim.setDuration(duration.toLong())
        growAnim.setDuration(duration.toLong())
        view1.startAnimation(shrinkAnim)
    }
}
