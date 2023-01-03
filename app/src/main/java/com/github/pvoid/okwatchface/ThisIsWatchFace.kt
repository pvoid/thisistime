package com.github.pvoid.okwatchface

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Icon
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
import kotlinx.coroutines.launch

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
                icon = Icon.createWithResource(applicationContext, info.icon),
                overlays
            )
        }

        val settings = mutableListOf<UserStyleSetting>(
            UserStyleSetting.BooleanUserStyleSetting(
                UserStyleSetting.Id(STYLE_KEY_AMBIENT),
                resources.getString(R.string.option_ambient_type_name),
                resources.getString(R.string.option_ambient_type_desc),
                icon = null,
                listOf(),
                false
            ),
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

        return UserStyleSchema(settings)
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
    var ambientBackgroundBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    lateinit var backgroundRect: Rect
    lateinit var timeRect: Rect
    var hoursRight: Float = 0f
    var minutesLeft: Float = 0f
    lateinit var timePaint: Paint
    lateinit var timeAmbientPaint: Paint
    lateinit var titlePaint: Paint
    lateinit var titleAmbientPaint: Paint
    lateinit var complicationPaint: Paint
    lateinit var weekDaysNames: Array<String>
    var lineSpace = 0f
    var ratio = 1f
    lateinit var ambientModeType: AmbientModeType

    override fun onDestroy() {
        backgroundBitmap.recycle()
        ambientBackgroundBitmap.recycle()
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

    fun updateAmbientModeType(context: Context, trueAmbient: AmbientModeType) {
        this.ambientModeType = trueAmbient
        createAmbientBackground(context)
        createAmbientPaint(context)
    }

    private fun createAmbientBackground(context: Context) {
        ambientBackgroundBitmap.recycle()
        if (ambientModeType == AmbientModeType.Minimalistic) {
            ambientBackgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ambient_background)
        } else {
            ambientBackgroundBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(ambientBackgroundBitmap)
            val paint = Paint()
            val cm = ColorMatrix().apply {
                setSaturation(0f)
            }
            val rect = Rect(0, 0, backgroundBitmap.width, backgroundBitmap.height)
            val f = ColorMatrixColorFilter(cm)
            paint.colorFilter = f;
            c.drawBitmap(backgroundBitmap, rect, rect, paint)
        }
    }

    private fun createAmbientPaint(context: Context) {
        if (ambientModeType == AmbientModeType.Minimalistic) {
            titleAmbientPaint = Paint().apply {
                color = context.resources.getColor(R.color.title_ambient_color)
                strokeWidth = context.resources.getDimension(R.dimen.title_stroke)
                typeface = titlePaint.typeface
                textSize = context.resources.getDimension(R.dimen.title_size)
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }
            timeAmbientPaint = Paint().apply {
                color = context.resources.getColor(R.color.time_ambient_color)
                strokeWidth = context.resources.getDimension(R.dimen.time_stroke)
                typeface = timePaint.typeface
                textSize = context.resources.getDimension(R.dimen.time_size)
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }
        } else {
            titleAmbientPaint = titlePaint
            timeAmbientPaint = timePaint
        }
    }
}

private class WatchfaceConfig(
    var ambientModeType: AmbientModeType = AmbientModeType.Minimalistic
)

class ThisIsCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    private val watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    private val currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<ThisIsSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    48,
    false
) {

    private val config = WatchfaceConfig()

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                for (option in userStyle) {
                    when(option.key.id.toString()) {
                        STYLE_KEY_AMBIENT -> {
                            val op = option.value as? UserStyleSetting.BooleanUserStyleSetting.BooleanOption
                            config.ambientModeType = if (op?.value == true) AmbientModeType.Background else AmbientModeType.Minimalistic
                        }
                    }
                }
            }
        }
    }

    override suspend fun createSharedAssets(): ThisIsSharedAssets = ThisIsSharedAssets().apply {
        backgroundPaint = Paint().apply {
            color = Color.BLACK
        }
        backgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.watch_background)
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

        updateAmbientModeType(context, config.ambientModeType)
    }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: ThisIsSharedAssets) {
        val ratio = bounds.width().toFloat() / sharedAssets.backgroundRect.width()
        if (ratio != sharedAssets.ratio) {
            sharedAssets.updateRatio(context, ratio)
        }

        if (config.ambientModeType != sharedAssets.ambientModeType) {
            sharedAssets.updateAmbientModeType(context, config.ambientModeType)
        }

        val isAmbient = watchState.isAmbient.value == true
        val bitmap = if (isAmbient) {
            sharedAssets.ambientBackgroundBitmap
        } else {
            sharedAssets.backgroundBitmap
        }
        val timePaint = if (isAmbient) {
            sharedAssets.timeAmbientPaint
        } else {
            sharedAssets.timePaint
        }
        val titlePaint = if (isAmbient) {
            sharedAssets.titleAmbientPaint
        } else {
            sharedAssets.titlePaint
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
        canvas.drawText(":", sharedAssets.hoursRight, baseLine, timePaint)
        val hours = zonedDateTime.hour.toString()
        var left = sharedAssets.hoursRight - timePaint.measureText(hours)
        canvas.drawText(hours, left, baseLine, timePaint)
        val minutes = zonedDateTime.minute.toString().padStart(2, '0')
        canvas.drawText(minutes, sharedAssets.minutesLeft, baseLine, timePaint)

        // Draw date and title
        baseLine += timePaint.fontMetricsInt.top
        baseLine -= sharedAssets.lineSpace
        val weekDay = sharedAssets.weekDaysNames[zonedDateTime.dayOfWeek.ordinal]
        val title = context.resources.getString(R.string.title, weekDay, zonedDateTime.dayOfMonth, zonedDateTime.month.value)
        left = sharedAssets.timeRect.centerX() - titlePaint.measureText(title) / 2
        canvas.drawText(title, left, baseLine, titlePaint)
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: ThisIsSharedAssets) {
    }
}