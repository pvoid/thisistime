package com.github.pvoid.okwatchface

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.Gravity
import android.view.SurfaceHolder

import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.TimeZone

private const val TYPEFACE_FILE_NAME = "children.ttf"

private const val INTERACTIVE_UPDATE_RATE_MS = 30000

private const val TIME_STROKE_WIDTH = 1f

private const val MSG_UPDATE_TIME = 0

private const val TIME_RECT_SCALE_WIDTH = 320f
private const val TIME_RECT_LEFT = 91f
private const val TIME_RECT_TOP = 3f
private const val TIME_RECT_WIDTH = 188f
private const val TIME_RECT_HEIGHT = 66f

class ThisIsWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: ThisIsWatchFace.Engine) : Handler(Looper.myLooper()!!) {
        private val mWeakReference: WeakReference<ThisIsWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var mCalendar: Calendar

        private var mRegisteredTimeZoneReceiver = false

        private var mTimeRect = RectF()

        private lateinit var mTimePaint: Paint
        private lateinit var mTitlePaint: Paint

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mBackgroundBitmap: Bitmap
        private lateinit var mGrayBackgroundBitmap: Bitmap

        private var mAmbient: Boolean = false

        private var mHoursRight: Float = 0f
        private var mMinutesLeft: Float = 0f
        private var mPadding: Float = 0f

        private val mUpdateTimeHandler = EngineHandler(this)

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        @Suppress("DEPRECATION")
        @SuppressLint("RtlHardcoded")
        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@ThisIsWatchFace)
                    .setAccentColor(Color.BLACK)
                    .setHideHotwordIndicator(true)
                    .setViewProtectionMode(WatchFaceStyle.PROTECT_STATUS_BAR)
                    .setStatusBarGravity(Gravity.RIGHT or Gravity.CENTER_VERTICAL)
                    .build())

            mCalendar = Calendar.getInstance()

            initialize()
        }

        @Suppress("DEPRECATION")
        private fun initialize() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            mBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.watch_background)
            mGrayBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.watch_background_ambient)

            val font = Typeface.createFromAsset(assets, TYPEFACE_FILE_NAME)

            mTimePaint = Paint().apply {
                color = resources.getColor(R.color.time_color)
                strokeWidth = TIME_STROKE_WIDTH
                typeface = font
                textSize = resources.getDimension(R.dimen.time_size)
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            mTitlePaint = Paint().apply {
                color = resources.getColor(R.color.title_color)
                strokeWidth = TIME_STROKE_WIDTH
                typeface = font
                textSize = resources.getDimension(R.dimen.title_size)
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            mPadding = resources.getDimension(R.dimen.title_padding)
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode
            updateTimer()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            val point = width / TIME_RECT_SCALE_WIDTH
            mTimeRect = RectF(TIME_RECT_LEFT * point, TIME_RECT_TOP * point,
                    (TIME_RECT_LEFT + TIME_RECT_WIDTH) * point, (TIME_RECT_TOP + TIME_RECT_HEIGHT) * point)

            val separatorWidth = mTitlePaint.measureText(":")
            mHoursRight = mTimeRect.centerX() - separatorWidth
            mMinutesLeft = mTimeRect.centerX() + separatorWidth

            val scale = width.toFloat() / mBackgroundBitmap.width.toFloat()

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (mBackgroundBitmap.width * scale).toInt(),
                    (mBackgroundBitmap.height * scale).toInt(), true)

            mGrayBackgroundBitmap = Bitmap.createScaledBitmap(mGrayBackgroundBitmap,
                    (mGrayBackgroundBitmap.width * scale).toInt(),
                    (mGrayBackgroundBitmap.height * scale).toInt(), true)
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            }
        }

        private fun drawWatchFace(canvas: Canvas) {
            if (mHoursRight == 0f || mMinutesLeft == 0f) {
                return
            }

            var baseLine = mTimeRect.bottom // - mTimePaint.fontMetricsInt.bottom
            canvas.drawText(":", mHoursRight, baseLine, mTimePaint)

            val hours = mCalendar[Calendar.HOUR_OF_DAY].toString()
            var left = mHoursRight - mTimePaint.measureText(hours)
            canvas.drawText(hours, left, baseLine, mTimePaint)

            val minutes = mCalendar[Calendar.MINUTE].toString().padStart(2, '0')
            canvas.drawText(minutes, mMinutesLeft, baseLine, mTimePaint)

            baseLine += mTimePaint.fontMetricsInt.top
            baseLine -= mPadding

            val weekDay = resources.getStringArray(R.array.week_day)[mCalendar[Calendar.DAY_OF_WEEK] - 1]
            val title = resources.getString(R.string.title, weekDay, mCalendar[Calendar.DAY_OF_MONTH], mCalendar[Calendar.MONTH] + 1)
            left = mTimeRect.centerX() - mTitlePaint.measureText(title) / 2
            canvas.drawText(title, left, baseLine, mTitlePaint)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@ThisIsWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@ThisIsWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !mAmbient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}


