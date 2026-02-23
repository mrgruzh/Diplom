package com.example.diplom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs

@Composable
fun IosTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = initialHour.coerceIn(0, 23))
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinute.coerceIn(0, 59))

    val selectedHour by remember {
        derivedStateOf {
            hours.getOrNull(centeredIndex(hourState)) ?: initialHour.coerceIn(0, 23)
        }
    }
    val selectedMinute by remember {
        derivedStateOf {
            minutes.getOrNull(centeredIndex(minuteState)) ?: initialMinute.coerceIn(0, 59)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 380.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* eat clicks */ }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Text(
                        text = "Время",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { onConfirm(selectedHour, selectedMinute) }
                    ) {
                        Text("Готово")
                    }
                }

                Divider()

                TimeWheelRow(
                    hourState = hourState,
                    minuteState = minuteState,
                    selectedHour = selectedHour,
                    selectedMinute = selectedMinute
                )
            }
        }
    }
}

@Composable
private fun TimeWheelRow(
    hourState: LazyListState,
    minuteState: LazyListState,
    selectedHour: Int,
    selectedMinute: Int
) {
    val itemHeight = 40.dp
    val visibleCount = 5
    val fadeColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WheelColumn(
                values = (0..23).toList(),
                state = hourState,
                selectedValue = selectedHour,
                formatter = { v -> v.toString().padStart(2, '0') },
                modifier = Modifier.width(88.dp)
            )

            Text(
                text = ":",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            WheelColumn(
                values = (0..59).toList(),
                state = minuteState,
                selectedValue = selectedMinute,
                formatter = { v -> v.toString().padStart(2, '0') },
                modifier = Modifier.width(88.dp)
            )
        }

        val highlightHeight = itemHeight
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(highlightHeight)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        )

        val fade = Brush.verticalGradient(
            0f to fadeColor,
            0.25f to Color.Transparent,
            0.75f to Color.Transparent,
            1f to fadeColor
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(fade)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    values: List<Int>,
    state: LazyListState,
    selectedValue: Int,
    formatter: (Int) -> String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleCount: Int = 5
) {
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val padding = itemHeight * (visibleCount / 2)

    LazyColumn(
        state = state,
        flingBehavior = fling,
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = padding)
    ) {
        itemsIndexed(values) { _, v ->
            val dist = abs(v - selectedValue)
            val alpha = when (dist) {
                0 -> 1f
                1 -> 0.55f
                2 -> 0.25f
                else -> 0.12f
            }

            Text(
                text = formatter(v),
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                fontSize = if (dist == 0) 22.sp else 18.sp,
                fontWeight = if (dist == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun centeredIndex(state: LazyListState): Int {
    val layout = state.layoutInfo
    val items = layout.visibleItemsInfo
    if (items.isEmpty()) return state.firstVisibleItemIndex

    val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    val closest = items.minBy { info ->
        val itemCenter = info.offset + info.size / 2
        abs(itemCenter - viewportCenter)
    }
    return closest.index
}
