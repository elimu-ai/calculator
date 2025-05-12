package com.android.calculator2

import ai.elimu.calculator.R
import android.view.View
import com.android.calculator2.view.DisplayOverlay
import com.android.calculator2.view.DisplayOverlay.TranslateState
import com.android.calculator2.view.DisplayOverlay.TranslateStateListener
import com.android.calculator2.view.GraphView
import com.android.calculator2.view.GraphView.PanListener
import com.android.calculator2.view.GraphView.ZoomListener
import com.xlythe.math.GraphModule
import com.xlythe.math.GraphModule.OnGraphUpdatedListener
import com.xlythe.math.Point

class GraphController(
    private val mGraphView: GraphView,
    private val mGraphModule: GraphModule,
    private val mDisplayOverlay: DisplayOverlay
) : OnGraphUpdatedListener, TranslateStateListener, PanListener, ZoomListener,
    View.OnClickListener {
    private val mExitControl: View
    private val mZoomInControl: View
    private val mZoomOutControl: View
    private val mZoomResetControl: View

    private var mEquation: String? = null
    private var mSeries: MutableList<Point?>? = null
    private var mIsGraphViewReady = false

    init {
        mGraphView.panListener = this
        mGraphView.zoomListener = this

        mExitControl = mDisplayOverlay.findViewById<View>(R.id.exitGraph)
        mExitControl.setOnClickListener(this)

        mZoomInControl = mDisplayOverlay.findViewById<View>(R.id.minusZoom)
        mZoomInControl.setOnClickListener(this)

        mZoomOutControl = mDisplayOverlay.findViewById<View>(R.id.plusZoom)
        mZoomOutControl.setOnClickListener(this)

        mZoomResetControl = mDisplayOverlay.findViewById<View>(R.id.resetZoom)
        mZoomResetControl.setOnClickListener(this)
    }

    private fun resetState() {
        mEquation = null
        mSeries = null
        mIsGraphViewReady = false
        mGraphView.zoomReset()
    }

    fun startGraph(equation: String) {
        resetState()
        mEquation = equation
        setDomainAndRange()

        // start calculating series now but don't set data on graph view until display
        // overlay has settled in the expanded position.  This prevents jank while the
        // display is opening.
        mGraphModule.updateGraph(equation, this)
        mDisplayOverlay.translateStateListener = this
        mDisplayOverlay.mode = DisplayOverlay.DisplayMode.GRAPH
        mDisplayOverlay.animateModeTransition()
    }

    private fun setDomainAndRange() {
        mGraphModule.setDomain(mGraphView.xAxisMin, mGraphView.xAxisMax)
        mGraphModule.setRange(mGraphView.yAxisMin, mGraphView.yAxisMax)
        mGraphModule.setZoomLevel(mGraphView.zoomLevel)
    }

    private fun setGraphDataIfReady() {
        if (mIsGraphViewReady) {
            mSeries?.let { series ->
                mGraphView.setData(series)
                mGraphView.invalidate()
            }
        }
    }

    fun exitGraphMode() {
        mGraphView.setData(ArrayList<Point?>())
        mGraphView.invalidate()
        mDisplayOverlay.mode = DisplayOverlay.DisplayMode.FORMULA
        mDisplayOverlay.animateModeTransition()
        resetState()
    }

    override fun onGraphUpdated(result: MutableList<Point?>?) {
        mSeries = result
        setGraphDataIfReady()
    }

    override fun onTranslateStateChanged(newState: TranslateState?) {
        if (mDisplayOverlay.mode == DisplayOverlay.DisplayMode.GRAPH) {
            if (newState == TranslateState.EXPANDED) {
                mIsGraphViewReady = true
                setGraphDataIfReady()
            } else if (newState == TranslateState.COLLAPSED) {
                exitGraphMode()
            }
        }
    }

    override fun panApplied() {
        updateForPanOrZoom()
    }

    override fun zoomApplied(level: Float) {
        updateForPanOrZoom()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.exitGraph -> exitGraphMode()
            R.id.minusZoom -> mGraphView.zoomOut()
            R.id.plusZoom -> mGraphView.zoomIn()
            R.id.resetZoom -> mGraphView.zoomReset()
        }
    }

    private fun updateForPanOrZoom() {
        if (mEquation != null) {
            setDomainAndRange()
            mIsGraphViewReady = true
            mGraphModule.updateGraph(mEquation, this)
        }
    }
}
