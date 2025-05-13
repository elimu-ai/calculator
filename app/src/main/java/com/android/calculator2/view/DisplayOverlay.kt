package com.android.calculator2.view

import ai.elimu.calculator.R
import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.calculator2.util.AnimationUtil
import com.android.calculator2.view.display.AdvancedDisplay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The display overlay is a container that intercepts touch events on top of:
 * 1. the display, i.e. the formula and result views
 * 2. the history view, which is revealed by dragging down on the display
 *
 * This overlay passes vertical scrolling events down to the history recycler view
 * when applicable.  If the user attempts to scroll up and the recycler is already
 * scrolled all the way up, then we intercept the event and collapse the history.
 */
class DisplayOverlay : FrameLayout {
    enum class DisplayMode {
        FORMULA, GRAPH
    }

    private var mRecyclerView: RecyclerView? = null
    private var mFormula: AdvancedDisplay? = null
    private var mResult: View? = null
    private var mGraphLayout: View? = null
    private var mCloseGraphHandle: View? = null
    private var mMainDisplay: View? = null
    private var mMode: DisplayMode? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mInitialMotionY = 0f
    private var mLastMotionY = 0f
    private var mLastDeltaY = 0f
    private var mTouchSlop = 0
    private var mMaxTranslationInParent = -1
    private var mVelocityTracker: VelocityTracker? = null
    private var mMinVelocity = -1f
    private var mParentHeight = -1

    /**
     * Reports when state changes to expanded or collapsed (partial is ignored)
     */
    interface TranslateStateListener {
        fun onTranslateStateChanged(newState: TranslateState?)
    }

    var translateStateListener: TranslateStateListener? = null

    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        setup()
    }

    private fun setup() {
        val vc = ViewConfiguration.get(getContext())
        mTouchSlop = vc.getScaledTouchSlop()
    }

    enum class TranslateState {
        EXPANDED, COLLAPSED, PARTIAL
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mRecyclerView = findViewById<RecyclerView>(R.id.historyRecycler)
        mLayoutManager = LinearLayoutManager(context)
        mLayoutManager!!.setOrientation(LinearLayoutManager.VERTICAL)
        mLayoutManager!!.setStackFromEnd(true)
        mRecyclerView!!.setLayoutManager(mLayoutManager)

        mFormula = findViewById<AdvancedDisplay>(R.id.formula)
        mResult = findViewById(R.id.result)
        mGraphLayout = findViewById(R.id.graphLayout)
        mCloseGraphHandle = findViewById(R.id.closeGraphHandle)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = MotionEventCompat.getActionMasked(ev)
        val y = ev.getRawY()
        val state = this.translateState

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mInitialMotionY = y
                mLastMotionY = y
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = y - mInitialMotionY
                if (abs(dy.toDouble()) < mTouchSlop) {
                    return false
                }

                // in graph mode let move events apply to the graph,
                // unless the touch is on the "close handle"
                if (mMode == DisplayMode.GRAPH) {
                    return isInBounds(ev.getX(), ev.getY(), mCloseGraphHandle!!)
                }

                if (dy < 0) {
                    return this.isScrolledToEnd && state != TranslateState.COLLAPSED
                } else if (dy > 0) {
                    return state != TranslateState.EXPANDED
                }
            }
        }

        return false
    }

    private val isScrolledToEnd: Boolean
        get() = mLayoutManager!!.findLastCompletelyVisibleItemPosition() ==
                mRecyclerView!!.getAdapter()!!.getItemCount() - 1

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = MotionEventCompat.getActionMasked(event)
        initVelocityTrackerIfNotExists()
        mVelocityTracker!!.addMovement(event)

        when (action) {
            MotionEvent.ACTION_DOWN -> {}
            MotionEvent.ACTION_MOVE -> handleMove(event)
            MotionEvent.ACTION_UP -> {
                handleUp(event)
                recycleVelocityTracker()
            }

            MotionEvent.ACTION_CANCEL -> recycleVelocityTracker()
        }

        return true
    }

    private fun handleMove(event: MotionEvent) {
        val state = this.translateState
        val y = event.getRawY()
        val dy = y - mLastMotionY
        if (DEBUG) {
            Log.v(TAG, "handleMove y=" + y + ", dy=" + dy)
        }

        if (dy < 0 && state != TranslateState.COLLAPSED) {
            updateTranslation(dy)
        } else if (dy > 0 && state != TranslateState.EXPANDED) {
            updateTranslation(dy)
        }
        mLastMotionY = y
        mLastDeltaY = dy
    }

    private fun handleUp(event: MotionEvent?) {
        mVelocityTracker!!.computeCurrentVelocity(1)
        val yvel = mVelocityTracker!!.getYVelocity()
        if (DEBUG) {
            Log.v(TAG, "handleUp yvel=" + yvel + ", mLastDeltaY=" + mLastDeltaY)
        }

        val curState = this.translateState
        if (curState != TranslateState.PARTIAL) {
            // already settled
            if (this.translateStateListener != null) {
                translateStateListener!!.onTranslateStateChanged(curState)
            }
        } else if (abs(yvel.toDouble()) > VELOCITY_SLOP) {
            // the sign on velocity seems unreliable, so use last delta to determine direction
            val destTx = (if (mLastDeltaY > 0) this.maxTranslation else 0).toFloat()
            val velocity = max(abs(yvel.toDouble()), abs(mMinVelocity.toDouble())).toFloat()
            settleAt(destTx, velocity)
        }
    }

    fun expandHistory() {
        settleAt(this.maxTranslation.toFloat(), mMinVelocity)
    }

    fun collapseHistory() {
        settleAt(0f, mMinVelocity)
    }

    val displayHeight: Int
        get() = mFormula!!.getHeight() + mResult!!.getHeight()

    /**
     * Smoothly translates the display overlay to the given target
     *
     * @param destTx target translation
     * @param yvel velocity at point of release
     */
    private fun settleAt(destTx: Float, yvel: Float) {
        if (yvel != 0f) {
            val dist = destTx - getTranslationY()
            val dt = abs((dist / yvel).toDouble()).toFloat()
            if (DEBUG) {
                Log.v(
                    TAG, "settle display overlay yvel=" + yvel +
                            ", dt = " + dt
                )
            }

            val anim =
                ObjectAnimator.ofFloat(
                    this, "translationY",
                    getTranslationY(), destTx
                )
            anim.setDuration(dt.toLong())
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    if (translateStateListener != null) {
                        translateStateListener!!.onTranslateStateChanged(translateState)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            anim.start()
        }
    }

    private val maxTranslation: Int
        /**
         * The distance that we are able to pull down the display to reveal history.
         */
        get() {
            if (mMaxTranslationInParent < 0) {
                val bottomPadding = getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.history_view_bottom_margin)
                mMaxTranslationInParent = this.parentHeight - this.displayHeight - bottomPadding
                if (DEBUG) {
                    Log.v(
                        TAG,
                        "mMaxTranslationInParent = " + mMaxTranslationInParent
                    )
                }
            }
            return mMaxTranslationInParent
        }

    private fun updateTranslation(dy: Float) {
        val txY = getTranslationY() + dy
        val clampedY = min(max(txY.toDouble(), 0.0), this.maxTranslation.toDouble()).toFloat()
        setTranslationY(clampedY)
    }

    private val translateState: TranslateState
        get() {
            val txY = getTranslationY()
            if (txY <= 0) {
                return TranslateState.COLLAPSED
            } else if (txY >= this.maxTranslation) {
                return TranslateState.EXPANDED
            } else {
                return TranslateState.PARTIAL
            }
        }

    val historyView: RecyclerView
        get() = mRecyclerView!!

    private fun initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    private val parentHeight: Int
        get() {
            if (mParentHeight < 0) {
                val parent = getParent() as ViewGroup
                mParentHeight = parent.getHeight()
            }
            return mParentHeight
        }

    /**
     * Set the size and offset of the history view / graph view
     *
     * We want the display+history to take up the full height of the parent minus some
     * predefined padding.  The normal way to do this would be to give the overlay a height
     * of match_parent minus some margin, and set an initial translation.  The issue with
     * this is that the display has a height of wrap content and the keypad fills the
     * remaining space, so we cannot determine the proper height for the history view until
     * after layout completes.
     *
     * To account for this, we make this method available to setup the history and graph
     * views after layout completes.
     */
    fun initializeHistoryAndGraphView() {
        val maxTx = this.maxTranslation
        if (mRecyclerView!!.getLayoutParams().height <= 0
            || mGraphLayout!!.getLayoutParams().height <= 0
        ) {
            val historyParams = mRecyclerView!!.getLayoutParams() as MarginLayoutParams
            historyParams.height = maxTx

            val graphParams = mGraphLayout!!.getLayoutParams() as MarginLayoutParams
            graphParams.height = maxTx + this.displayHeight
            if (DEBUG) {
                Log.v(
                    TAG, ("Set history height to " + maxTx
                            + ", graph height to " + graphParams.height)
                )
            }

            val overlayParams =
                getLayoutParams() as MarginLayoutParams
            overlayParams.topMargin = -maxTx
            requestLayout()
            scrollToMostRecent()
        }

        if (mMinVelocity < 0) {
            val txDist = this.maxTranslation
            mMinVelocity = txDist / MIN_SETTLE_DURATION
        }
    }

    fun scrollToMostRecent() {
        mRecyclerView!!.scrollToPosition(mRecyclerView!!.getAdapter()!!.getItemCount() - 1)
    }

    private fun isInBounds(x: Float, y: Float, v: View): Boolean {
        return y >= v.getTop() && y <= v.getBottom() && x >= v.getLeft() && x <= v.getRight()
    }

    fun animateModeTransition() {
        when (mMode) {
            DisplayMode.GRAPH -> {
                expandHistory()
                AnimationUtil.fadeOut(mMainDisplay!!)
                AnimationUtil.fadeIn(mGraphLayout!!)
            }

            DisplayMode.FORMULA -> {
                collapseHistory()
                AnimationUtil.fadeIn(mMainDisplay!!)
                AnimationUtil.fadeOut(mGraphLayout!!)
            }

            null -> TODO()
        }
    }

    var mode: DisplayMode
        get() = mMode!!
        set(mode) {
            mMode = mode
        }

    companion object {
        /**
         * Closing the history with a fling will finish at least this fast (ms)
         */
        private const val MIN_SETTLE_DURATION = 200f

        /**
         * Do not settle overlay if velocity is less than this
         */
        private const val VELOCITY_SLOP = 0.1f

        private const val DEBUG = false
        private const val TAG = "DisplayOverlay"
    }
}
