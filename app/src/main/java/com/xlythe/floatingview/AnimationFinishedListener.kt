package com.xlythe.floatingview

import android.animation.Animator

abstract class AnimationFinishedListener : Animator.AnimatorListener {
    override fun onAnimationCancel(animation: Animator) {}

    override fun onAnimationRepeat(animation: Animator) {}

    override fun onAnimationStart(animation: Animator) {}

    override fun onAnimationEnd(animation: Animator) {
        onAnimationFinished()
    }

    abstract fun onAnimationFinished()
}
