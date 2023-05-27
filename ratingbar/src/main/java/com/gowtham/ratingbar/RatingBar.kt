package com.gowtham.ratingbar

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

sealed interface StepSize {
    object ONE : StepSize
    object HALF : StepSize
}

sealed interface RatingBarStyle {
    object Normal : RatingBarStyle
    object HighLighted : RatingBarStyle
}

//For ui testing
val StarRatingKey = SemanticsPropertyKey<Float>("StarRating")
var SemanticsPropertyReceiver.starRating by StarRatingKey


/**
 * Draws a Rating Bar on the screen according to the [RatingBarConfig] instance passed to the composable
 *
 * @param value is current selected rating count
 * @param config the different configurations applied to the Rating Bar.
 * @param onRatingChanged A function to be called when the click or drag is released and rating value is passed
 * @see [RatingBarConfig]
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RatingBar(
    value: Float,
    modifier: Modifier = Modifier,
    config: RatingBarConfig = RatingBarConfig(),
    painterEmpty: Painter?= null,
    painterFilled: Painter?= null,
    onValueChange: (Float) -> Unit,
    onRatingChanged: (Float) -> Unit
) {
    var rowSize by remember { mutableStateOf(Size.Zero) }
    var lastDraggedValue by remember { mutableStateOf(0f) }
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current


    val paddingInPx = remember(config.horizontalPadding) {
        with(density) { config.horizontalPadding.toPx() }
    }
    val starSizeInPx = remember(config.size) {
        with(density) { config.size.toPx() }
    }

    Row(modifier = modifier
        .onSizeChanged { rowSize = it.toSize() }
        .pointerInput(
            Unit
        ) {
            //handling dragging events
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (config.isIndicator || config.hideInactiveStars)
                        return@detectHorizontalDragGestures
                    onRatingChanged(lastDraggedValue)
                },
                onDragCancel = {

                },
                onDragStart = {

                },
                onHorizontalDrag = { change, _ ->
                    if (config.isIndicator || config.hideInactiveStars)
                        return@detectHorizontalDragGestures
                    change.consume()
                    val dragX = change.position.x.coerceIn(-1f, rowSize.width)
                    var calculatedStars =
                        RatingBarUtils.calculateStars(
                            dragX,
                            paddingInPx,
                            starSizeInPx,
                            config
                        )

                    if (direction == LayoutDirection.Rtl)
                        calculatedStars = config.numStars - calculatedStars
                    onValueChange(calculatedStars)
                    lastDraggedValue = calculatedStars
                }
            )
        }
        .pointerInteropFilter {
            if (config.isIndicator || config.hideInactiveStars)
                return@pointerInteropFilter false
            //handling when click events
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    val dragX = it.x.coerceIn(-1f, rowSize.width)
                    var calculatedStars =
                        RatingBarUtils.calculateStars(
                            dragX,
                            paddingInPx,
                            starSizeInPx,
                            config
                        )
                    if (direction == LayoutDirection.Rtl)
                        calculatedStars = config.numStars - calculatedStars
                    onValueChange(calculatedStars)
                    onRatingChanged(calculatedStars)
                }
            }
            true
        }) {
        ComposeStars(value, config,painterEmpty,painterFilled)
    }
}

@Composable
fun ComposeStars(
    value: Float,
    config: RatingBarConfig,
    painterEmpty: Painter?,
    painterFilled: Painter?
) {

    val ratingPerStar = 1f
    var remainingRating = value

    Row(modifier = Modifier
        .semantics { starRating = value }) {
        for (i in 1..config.numStars) {
            val starRating = when {
                remainingRating == 0f -> {
                    0f
                }
                remainingRating >= ratingPerStar -> {
                    remainingRating -= ratingPerStar
                    1f
                }
                else -> {
                    val fraction = remainingRating / ratingPerStar
                    remainingRating = 0f
                    fraction
                }
            }
            if (config.hideInactiveStars && starRating == 0.0f)
                break

            RatingStar(
                fraction = starRating,
                config = config,
                modifier = Modifier
                    .padding(
                        start = if (i > 1) config.horizontalPadding else 0.dp,
                        end = if (i < config.numStars) config.horizontalPadding else 0.dp
                    )
                    .size(size = config.size)
                    .testTag("RatingStar"),
                painterEmpty, painterFilled
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RatingBarPreview() {
    var rating by remember { mutableStateOf(3.3f) }
    RatingBar(
        value = rating,
        config = RatingBarConfig(),
        onValueChange = {
            rating = it
        }
    ) {
        Log.d("TAG", "RatingBarPreview: $it")
    }
}