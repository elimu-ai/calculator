package com.android.calculator2.view

import ai.elimu.calculator.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.xlythe.math.Point
import java.lang.Double
import java.text.DecimalFormat
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.math.sqrt
import kotlin.run

class GraphView : View {
    private var mDrawingAlgorithm: Int = LINES
    var mFormat: DecimalFormat = DecimalFormat("#.#####")
    var panListener: PanListener? = null
    var zoomListener: ZoomListener? = null
    private var mBackgroundPaint: Paint? = null
    private var mTextPaint: Paint? = null
    private var mAxisPaint: Paint? = null
    private var mGraphPaint: Paint? = null
    private var mOffsetX = 0
    private var mOffsetY = 0
    private var mLineMargin = 0
    private var mMinLineMargin = 0
    private var mTextPaintSize = 0
    private var mZoomLevel = 1f
    private var mData: MutableList<Point?>? = null
    private var mStartX = 0f
    private var mStartY = 0f
    private var mDragOffsetX = 0
    private var mDragOffsetY = 0
    private var mDragRemainderX = 0
    private var mDragRemainderY = 0
    private var mZoomInitDistance = 0.0
    private var mZoomInitLevel = 0f
    private var mMode = 0
    private var mPointers = 0

    constructor(context: Context?) : super(context) {
        setup()
    }

    private fun setup() {
        val res = getContext().getResources()

        mBackgroundPaint = Paint()
        mBackgroundPaint!!.setColor(res.getColor(R.color.graph_background_color))
        mBackgroundPaint!!.setStyle(Paint.Style.FILL)

        mTextPaintSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            16f,
            getResources().getDisplayMetrics()
        ).toInt()
        mTextPaint = Paint()
        mTextPaint!!.setColor(res.getColor(R.color.graph_labels_color))
        mTextPaint!!.setTextSize(mTextPaintSize.toFloat())

        mAxisPaint = Paint()
        mAxisPaint!!.setColor(res.getColor(R.color.graph_grid_color))
        mAxisPaint!!.setStyle(Paint.Style.STROKE)
        mAxisPaint!!.setStrokeWidth(2f)

        mGraphPaint = Paint()
        mGraphPaint!!.setColor(res.getColor(R.color.graph_line_color))
        mGraphPaint!!.setStyle(Paint.Style.STROKE)
        mGraphPaint!!.setStrokeWidth(6f)

        zoomReset()

        mData = ArrayList<Point?>()
    }

    fun zoomReset() {
        this.zoomLevel = 1f
        mOffsetY = 0
        mOffsetX = mOffsetY
        mDragRemainderY = mOffsetX
        mDragRemainderX = mDragRemainderY
        onSizeChanged(getWidth(), getHeight(), 0, 0)
        invalidate()
        if (this.panListener != null) panListener!!.panApplied()
        if (this.zoomListener != null) zoomListener!!.zoomApplied(mZoomLevel)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup()
    }

    private fun average(vararg args: Point): Point {
        var x = 0f
        var y = 0f
        for (p in args) {
            x += p.getX().toFloat()
            y += p.getY().toFloat()
        }
        return Point((x / args.size).toDouble(), (y / args.size).toDouble())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Update mode if pointer count changes
        if (mPointers != event.getPointerCount()) {
            setMode(event)
        }

        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> setMode(event)
            MotionEvent.ACTION_UP -> {}
            MotionEvent.ACTION_MOVE -> if (mMode == DRAG) {
                mOffsetX += mDragOffsetX
                mOffsetY += mDragOffsetY
                mDragOffsetX = (event.getX() - mStartX).toInt() / mLineMargin
                mDragOffsetY = (event.getY() - mStartY).toInt() / mLineMargin
                mDragRemainderX = (event.getX() - mStartX).toInt() % mLineMargin
                mDragRemainderY = (event.getY() - mStartY).toInt() % mLineMargin
                mOffsetX -= mDragOffsetX
                mOffsetY -= mDragOffsetY
                if (this.panListener != null) panListener!!.panApplied()
            } else if (mMode == ZOOM) {
                val distance = getDistance(
                    Point(event.getX(0).toDouble(), event.getY(0).toDouble()),
                    Point(event.getX(1).toDouble(), event.getY(1).toDouble())
                )
                val delta = mZoomInitDistance - distance
                val zoom = (delta / mZoomInitDistance).toFloat()
                this.zoomLevel = mZoomInitLevel + zoom
            }
        }
        invalidate()
        return true
    }

    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)

        mMinLineMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            25f,
            getResources().getDisplayMetrics()
        ).toInt()
        mLineMargin = mMinLineMargin
        // Center the offsets
        mOffsetX += (xOld / mLineMargin) / 2
        mOffsetY += (yOld / mLineMargin) / 2
        mOffsetX -= (xNew / mLineMargin) / 2
        mOffsetY -= (yNew / mLineMargin) / 2
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        canvas.drawPaint(mBackgroundPaint!!)

        // draw bounding box
        mAxisPaint!!.setStrokeWidth(BOX_STROKE.toFloat())
        canvas.drawRect(
            mLineMargin.toFloat(),
            mLineMargin.toFloat(),
            (getWidth() - BOX_STROKE / 2).toFloat(),
            (getHeight() - BOX_STROKE / 2).toFloat(),
            mAxisPaint!!
        )

        // Draw the grid lines
        val bounds = Rect()
        var previousLine = 0
        run {
            var i = 1
            var j = mOffsetX
            while (i * mLineMargin < getWidth()) {
                // Draw vertical lines
                val x = i * mLineMargin + mDragRemainderX
                if (x < mLineMargin || x - previousLine < mMinLineMargin) {
                    i++
                    j++
                    continue
                }
                previousLine = x

                if (j == 0) mAxisPaint!!.setStrokeWidth(6f)
                else mAxisPaint!!.setStrokeWidth(2f)
                canvas.drawLine(
                    x.toFloat(),
                    mLineMargin.toFloat(),
                    x.toFloat(),
                    getHeight().toFloat(),
                    mAxisPaint!!
                )

                // Draw label on left
                val text = mFormat.format((j * mZoomLevel).toDouble())
                val textLength =
                    ((if (text.startsWith("-")) text.length - 1 else text.length) + 1) / 2
                mTextPaint!!.setTextSize((mTextPaintSize / textLength).toFloat())
                mTextPaint!!.getTextBounds(text, 0, text.length, bounds)
                val textWidth = bounds.right - bounds.left
                canvas.drawText(
                    text,
                    (x - textWidth / 2).toFloat(),
                    mLineMargin / 2 + mTextPaint!!.getTextSize() / 2,
                    mTextPaint!!
                )
                i++
                j++
            }
        }
        previousLine = 0
        var i = 1
        var j = mOffsetY
        while (i * mLineMargin < getHeight()) {
            // Draw horizontal lines
            val y = i * mLineMargin + mDragRemainderY
            if (y < mLineMargin || y - previousLine < mMinLineMargin) {
                i++
                j++
                continue
            }
            previousLine = y

            if (j == 0) mAxisPaint!!.setStrokeWidth(6f)
            else mAxisPaint!!.setStrokeWidth(2f)
            canvas.drawLine(
                mLineMargin.toFloat(),
                y.toFloat(),
                getWidth().toFloat(),
                y.toFloat(),
                mAxisPaint!!
            )

            // Draw label on left
            val text = mFormat.format((-j * mZoomLevel).toDouble())
            val textLength = ((if (text.startsWith("-")) text.length - 1 else text.length) + 1) / 2
            mTextPaint!!.setTextSize((mTextPaintSize / textLength).toFloat())
            mTextPaint!!.getTextBounds(text, 0, text.length, bounds)
            val textHeight = bounds.bottom - bounds.top
            val textWidth = bounds.right - bounds.left
            canvas.drawText(
                text,
                (mLineMargin / 2 - textWidth / 2).toFloat(),
                (y + textHeight / 2).toFloat(),
                mTextPaint!!
            )
            i++
            j++
        }

        // Restrict drawing the graph to the grid
        canvas.clipRect(
            mLineMargin, mLineMargin,
            getWidth() - BOX_STROKE, getHeight() - BOX_STROKE
        )

        // Create a path to draw smooth arcs
        if (mDrawingAlgorithm == LINES) {
            if (mData!!.size != 0) {
                drawWithStraightLines(mData!!, canvas)
            }
        } else if (mDrawingAlgorithm == DOTS) {
            drawDots(mData!!, canvas)
        }
    }

    private fun drawWithStraightLines(data: MutableList<Point?>, canvas: Canvas) {
        var previousPoint: Point? = null
        for (currentPoint in data) {
            if (previousPoint == null) {
                previousPoint = currentPoint
                continue
            }

            val aX = getRawX(previousPoint)
            val aY = getRawY(previousPoint)
            val bX = getRawX(currentPoint)
            val bY = getRawY(currentPoint)

            previousPoint = currentPoint

            if (aX == -1 || aY == -1 || bX == -1 || bY == -1 || tooFar(
                    aX.toFloat(),
                    aY.toFloat(),
                    bX.toFloat(),
                    bY.toFloat()
                )
            ) continue

            canvas.drawLine(aX.toFloat(), aY.toFloat(), bX.toFloat(), bY.toFloat(), mGraphPaint!!)
        }
    }

    private fun drawDots(data: MutableList<Point?>, canvas: Canvas) {
        for (p in data) {
            canvas.drawPoint(getRawX(p).toFloat(), getRawY(p).toFloat(), mGraphPaint!!)
        }
    }

    private fun getRawX(p: Point?): Int {
        if (p == null || Double.isNaN(p.getX()) || Double.isInfinite(p.getX())) return -1

        // The left line is at pos
        val leftLine = (mLineMargin + mDragRemainderX).toFloat()
        // And equals
        val `val` = mOffsetX * mZoomLevel
        // And changes at a rate of
        val slope = mLineMargin / mZoomLevel
        // Put it all together
        val pos = (slope * (p.getX() - `val`) + leftLine).toInt()

        return pos
    }

    private fun getRawY(p: Point?): Int {
        if (p == null || Double.isNaN(p.getY()) || Double.isInfinite(p.getY())) return -1

        // The top line is at pos
        val topLine = (mLineMargin + mDragRemainderY).toFloat()
        // And equals
        val `val` = -mOffsetY * mZoomLevel
        // And changes at a rate of
        val slope = mLineMargin / mZoomLevel
        // Put it all together
        val pos = (-slope * (p.getY() - `val`) + topLine).toInt()

        return pos
    }

    private fun tooFar(aX: Float, aY: Float, bX: Float, bY: Float): Boolean {
        val outOfBounds = aX == -1f || aY == -1f || bX == -1f || bY == -1f
        if (outOfBounds) return true

        val horzAsymptote =
            (aX > this.xAxisMax && bX < this.xAxisMin) || (aX < this.xAxisMin && bX > this.xAxisMax)
        val vertAsymptote =
            (aY > this.yAxisMax && bY < this.yAxisMin) || (aY < this.yAxisMin && bY > this.yAxisMax)
        return horzAsymptote || vertAsymptote
    }

    val xAxisMin: Float
        get() = mOffsetX * mZoomLevel

    val xAxisMax: Float
        get() {
            var num = mOffsetX
            var i = 1
            while (i * mLineMargin < getWidth()) {
                i++
                num++
            }
            return num * mZoomLevel
        }

    val yAxisMin: Float
        get() = mOffsetY * mZoomLevel

    val yAxisMax: Float
        get() {
            var num = mOffsetY
            var i = 1
            while (i * mLineMargin < getHeight()) {
                i++
                num++
            }
            return num * mZoomLevel
        }

    override fun setBackgroundColor(color: Int) {
        mBackgroundPaint!!.setColor(color)
    }

    private fun setMode(e: MotionEvent) {
        mPointers = e.getPointerCount()
        when (e.getPointerCount()) {
            1 ->                 // Drag
                setMode(DRAG, e)

            2 ->                 // Zoom
                setMode(ZOOM, e)
        }
    }

    private fun setMode(mode: Int, e: MotionEvent) {
        mMode = mode
        when (mode) {
            DRAG -> {
                mStartX = e.getX()
                mStartY = e.getY()
                mDragOffsetX = 0
                mDragOffsetY = 0
            }

            ZOOM -> {
                mZoomInitDistance = getDistance(
                    Point(e.getX(0).toDouble(), e.getY(0).toDouble()),
                    Point(e.getX(1).toDouble(), e.getY(1).toDouble())
                )
                mZoomInitLevel = mZoomLevel
            }
        }
    }

    var zoomLevel: Float
        get() = mZoomLevel
        set(level) {
            mZoomLevel = level
            invalidate()
            if (this.zoomListener != null) zoomListener!!.zoomApplied(mZoomLevel)
        }

    fun zoomIn() {
        this.zoomLevel = mZoomLevel / 2
    }

    fun zoomOut() {
        this.zoomLevel = mZoomLevel * 2
    }

    fun setData(data: MutableList<Point?>) {
        mData = data
        mDrawingAlgorithm = LINES
    }

    private fun getDistance(a: Point, b: Point): kotlin.Double {
        return sqrt(square(a.getX() - b.getX()) + square(a.getY() - b.getY()))
    }

    private fun square(`val`: kotlin.Double): kotlin.Double {
        return `val` * `val`
    }

    fun setGridColor(color: Int) {
        mAxisPaint!!.setColor(color)
    }

    fun setTextColor(color: Int) {
        mTextPaint!!.setColor(color)
    }

    fun setGraphColor(color: Int) {
        mGraphPaint!!.setColor(color)
    }

    interface PanListener {
        fun panApplied()
    }

    interface ZoomListener {
        fun zoomApplied(level: Float)
    }

    companion object {
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val LINES = 1
        private const val DOTS = 2
        private const val BOX_STROKE = 6
    }
}
