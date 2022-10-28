package com.github.pvoid.okwatchface

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.*
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.style.UserStyleSetting.Option
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import java.time.ZonedDateTime

private const val TYPEFACE_FILE_NAME = "children.ttf"

class ThisIsWatchFace : WatchFaceService() {
    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = ThisIsCanvasRenderer(
            applicationContext,
            surfaceHolder,
            watchState,
            complicationSlotsManager,
            currentUserStyleRepository,
            CanvasType.HARDWARE
        )
        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }

    override fun createUserStyleSchema(): UserStyleSchema {
        val options = ComplicationLayout.values().map { info ->
            val overlays = ComplicationSlotInfo.values().map {
                it.id to (it in info.complications)
            }.map { (id, enabled) ->
                ComplicationSlotOverlay.Builder(id).setEnabled(enabled).build()
            }
            UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                Option.Id(info.id),
                resources.getString(info.title),
                icon = null,
                overlays
            )
        }

        return UserStyleSchema(
            listOf(
                UserStyleSetting.ComplicationSlotsUserStyleSetting(
                    UserStyleSetting.Id(STYLE_KEY_LAYOUT),
                    resources.getString(R.string.option_layout_name),
                    resources.getString(R.string.option_layout_desc),
                    icon = null,
                    complicationConfig = options,
                    listOf(
                        WatchFaceLayer.COMPLICATIONS,
                        WatchFaceLayer.COMPLICATIONS_OVERLAY
                    ),
                ),
            )
        )
    }

    override fun createComplicationSlotsManager(currentUserStyleRepository: CurrentUserStyleRepository): ComplicationSlotsManager {
        val canvasComplicationFactory = CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(ComplicationDrawable(this), watchState, listener)
        }

        return ComplicationSlotsManager(
            ComplicationSlotInfo.values().map { info ->
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    info.id,
                    canvasComplicationFactory,
                    info.types,
                    DefaultComplicationDataSourcePolicy(
                        SystemDataSources.DATA_SOURCE_STEP_COUNT,
                        ComplicationType.RANGED_VALUE
                    ),
                    ComplicationSlotBounds(info.bounds)
                ).setEnabled(false).build()
            }.toList(),
            currentUserStyleRepository
        )
    }
}

class ThisIsSharedAssets : Renderer.SharedAssets {
    lateinit var backgroundPaint: Paint
    lateinit var backgroundBitmap: Bitmap
    lateinit var grayBackgroundBitmap: Bitmap
    lateinit var backgroundRect: Rect
    lateinit var timeRect: Rect
    var hoursRight: Float = 0f
    var minutesLeft: Float = 0f
    lateinit var timePaint: Paint
    lateinit var titlePaint: Paint
    lateinit var complicationPaint: Paint
    lateinit var weekDaysNames: Array<String>
    var lineSpace = 0f
    var ratio = 1f

    override fun onDestroy() {
        backgroundBitmap.recycle()
        grayBackgroundBitmap.recycle()
    }

    fun updateRatio(context: Context, ratio: Float) {
        val left = (context.resources.getDimensionPixelSize(R.dimen.time_offset_left) * ratio).toInt()
        val top = (context.resources.getDimensionPixelSize(R.dimen.time_offset_top) * ratio).toInt()
        timeRect = Rect(
            left,
            top,
            left + (context.resources.getDimensionPixelSize(R.dimen.time_width) * ratio).toInt(),
            top + (context.resources.getDimensionPixelSize(R.dimen.time_height) * ratio).toInt(),
        )

        val separatorWidth = timePaint.measureText(":")
        hoursRight = timeRect.centerX() - separatorWidth
        minutesLeft = timeRect.centerX().toFloat()

        this.ratio = ratio
    }
}

class ThisIsCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    private val watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<ThisIsSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    48,
    false
) {

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override suspend fun createSharedAssets(): ThisIsSharedAssets = ThisIsSharedAssets().apply {
        backgroundPaint = Paint().apply {
            color = Color.BLACK
        }
        backgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.watch_background)

        grayBackgroundBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(grayBackgroundBitmap)
        val paint = Paint()
        val cm = ColorMatrix().apply {
            setSaturation(0f)
        }
        val rect = Rect(0, 0, backgroundBitmap.width, backgroundBitmap.height)
        val f = ColorMatrixColorFilter(cm)
        paint.colorFilter = f;
        c.drawBitmap(backgroundBitmap, rect, rect, paint)

        backgroundRect = Rect(0, context.resources.getDimensionPixelSize(R.dimen.background_offset_top), backgroundBitmap.width, backgroundBitmap.width)

        val font = Typeface.createFromAsset(context.assets, TYPEFACE_FILE_NAME)
        titlePaint = Paint().apply {
            color = context.resources.getColor(R.color.title_color)
            strokeWidth = context.resources.getDimension(R.dimen.title_stroke)
            typeface = font
            textSize = context.resources.getDimension(R.dimen.title_size)
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        timePaint = Paint().apply {
            color = context.resources.getColor(R.color.time_color)
            strokeWidth = context.resources.getDimension(R.dimen.time_stroke)
            typeface = font
            textSize = context.resources.getDimension(R.dimen.time_size)
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        complicationPaint = Paint().apply {
            color = Color.argb(0.6f, 0f, 0f, 0f)
        }

        updateRatio(context, 1f)

        weekDaysNames = context.resources.getStringArray(R.array.week_day)
        lineSpace = context.resources.getDimension(R.dimen.title_padding)
    }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: ThisIsSharedAssets) {
        val ratio = bounds.width().toFloat() / sharedAssets.backgroundRect.width()
        if (ratio != sharedAssets.ratio) {
            sharedAssets.updateRatio(context, ratio)
        }

        val isAmbient = watchState.isAmbient.value == true
        val bitmap = if (isAmbient) {
            sharedAssets.grayBackgroundBitmap
        } else {
            sharedAssets.backgroundBitmap
        }
        canvas.drawBitmap(bitmap, sharedAssets.backgroundRect, bounds, sharedAssets.backgroundPaint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                val complicationBounds = complication.computeBounds(bounds)
                canvas.drawCircle(complicationBounds.exactCenterX(), complicationBounds.exactCenterY(), complicationBounds.width() / 2f, sharedAssets.complicationPaint)
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }

        // Draw time
        var baseLine = sharedAssets.timeRect.bottom.toFloat()
        canvas.drawText(":", sharedAssets.hoursRight, baseLine, sharedAssets.timePaint)
        val hours = zonedDateTime.hour.toString()
        var left = sharedAssets.hoursRight - sharedAssets.timePaint.measureText(hours)
        canvas.drawText(hours, left, baseLine, sharedAssets.timePaint)
        val minutes = zonedDateTime.minute.toString().padStart(2, '0')
        canvas.drawText(minutes, sharedAssets.minutesLeft, baseLine, sharedAssets.timePaint)

        // Draw date and title
        baseLine += sharedAssets.timePaint.fontMetricsInt.top
        baseLine -= sharedAssets.lineSpace
        val weekDay = sharedAssets.weekDaysNames[zonedDateTime.dayOfWeek.ordinal]
        val title = context.resources.getString(R.string.title, weekDay, zonedDateTime.dayOfMonth, zonedDateTime.month.value)
        left = sharedAssets.timeRect.centerX() - sharedAssets.titlePaint.measureText(title) / 2
        canvas.drawText(title, left, baseLine, sharedAssets.titlePaint)
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: ThisIsSharedAssets) {
    }
}