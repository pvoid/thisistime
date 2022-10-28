package com.github.pvoid.okwatchface

import android.graphics.RectF
import androidx.annotation.StringRes
import androidx.wear.watchface.complications.data.ComplicationType

const val STYLE_KEY_LAYOUT = "complication_layout_setting"

enum class ComplicationSlotInfo(
    val id: Int,
    val types: List<ComplicationType>,
    val bounds: RectF
) {
    RIGHT_TOP(
        id = 100,
        types = listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        ),
        bounds = RectF(0.76f, 0.40f, 0.98f, 0.62f)
    ),
    RIGHT_BOTTOM(
        id = 300,
        types = listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        ),
        bounds = RectF(0.68f, 0.62f, 0.90f, 0.84f)
    ),
    RIGHT_CENTER(
        id = 200,
        types = listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        ),
        RectF(0.72f, 0.48f, 0.96f, 0.72f)
    ),
    LEFT_TOP(
        id = 400,
        types = listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        ),
        bounds = RectF(0.02f, 0.40f, 0.24f, 0.62f)
    );
}

enum class ComplicationLayout(
    val id: String,
    @StringRes val title: Int,
    val complications: List<ComplicationSlotInfo>
) {
    COMPAT_RIGHT(
        id = "compat_right",
        title = R.string.layout_compat_right,
        complications = listOf(ComplicationSlotInfo.RIGHT_CENTER)
    ),
    COMPAT_LEFT(
        id = "compat_left",
        title = R.string.layout_compat_left,
        complications = listOf(ComplicationSlotInfo.LEFT_TOP)
    ),
    FULL_RIGHT(
        id = "full_right",
        title = R.string.layout_full_right,
        complications = listOf(ComplicationSlotInfo.RIGHT_TOP, ComplicationSlotInfo.RIGHT_BOTTOM)
    ),
    FULL(
        id = "full",
        title = R.string.layout_full,
        complications = listOf(ComplicationSlotInfo.RIGHT_TOP, ComplicationSlotInfo.RIGHT_BOTTOM, ComplicationSlotInfo.LEFT_TOP)
    ),
    CLEAR(
        id = "clear",
        title = R.string.layout_clear,
        complications = listOf()
    );
}
