package dev.medetzhakupov

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex

// Data class to hold the drag state
data class DragState<T>(
    val isDragging: Boolean = false,
    val draggedItem: T? = null,
    val draggedItemIndex: Int = -1,
    val draggedItemSize: IntSize = IntSize.Zero,
    val dragPosition: Offset = Offset.Zero,
    val dragOffset: Offset = Offset.Zero,
    val initialTouchOffset: Offset = Offset.Zero  // Added to store touch position within item
)

// CompositionLocal for sharing drag state
val LocalDragDropState = compositionLocalOf<DragState<*>> {
    DragState<String>()
}

class DragDropState<T>(
    private val items: List<T>,
    private val onMove: (Int, Int) -> Unit,
    private val onDropOutside: (T, Offset) -> Unit
) {
    var dragState by mutableStateOf(DragState<T>())
        private set

    private var overScrollJob by mutableStateOf<Long?>(null)

    fun onDragStart(
        itemPosition: Offset,
        touchOffset: Offset,  // Added parameter for touch position
        itemIndex: Int,
        item: T,
        itemSize: IntSize
    ) {
        dragState = DragState(
            isDragging = true,
            draggedItem = item,
            draggedItemIndex = itemIndex,
            draggedItemSize = itemSize,
            dragPosition = itemPosition,
            dragOffset = Offset.Zero,
            initialTouchOffset = touchOffset  // Store the touch offset
        )
    }

    fun onDrag(offset: Offset, listState: LazyListState) {
        dragState = dragState.copy(
            dragOffset = dragState.dragOffset + offset
        )

        // Calculate current position in list
        val currentIndex = dragState.draggedItemIndex
        val dragPos = dragState.dragPosition + dragState.dragOffset

        // Find item under the current drag position
        val itemUnderDrag = findItemIndexUnderDrag(dragPos, listState)

        if (itemUnderDrag != null && itemUnderDrag != currentIndex) {
            onMove(currentIndex, itemUnderDrag)
            dragState = dragState.copy(draggedItemIndex = itemUnderDrag)
        }
    }

    fun onDragEnd() {
        val currentPosition = dragState.dragPosition + dragState.dragOffset
        dragState.draggedItem?.let { item ->
            onDropOutside(item, currentPosition)
        }
        dragState = DragState()
        overScrollJob = null
    }

    private fun findItemIndexUnderDrag(
        dragPos: Offset,
        listState: LazyListState
    ): Int {
        // Implement item detection logic based on position
        // This is a simplified version - you might want to enhance it
        val scrollOffset = listState.firstVisibleItemScrollOffset
        val firstVisibleIndex = listState.firstVisibleItemIndex
        val itemHeight = dragState.draggedItemSize.height

        val relativePos = dragPos.y + scrollOffset
        val estimatedIndex = (relativePos / itemHeight).toInt() + firstVisibleIndex

        return estimatedIndex.coerceIn(0, items.size - 1)
    }
}

@Composable
fun <T> DraggableLazyColumn(
    items: List<T>,
    onMove: (Int, Int) -> Unit,
    onDropOutside: (T, Offset) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    itemContent: @Composable (T, Boolean) -> Unit
) {
    val dragDropState = remember(items) {
        DragDropState(
            items = items,
            onMove = onMove,
            onDropOutside = onDropOutside
        )
    }

    val currentDragPosition = remember { mutableStateOf<Offset?>(null) }

    CompositionLocalProvider(LocalDragDropState provides dragDropState.dragState) {
        Box(modifier = modifier) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { index, item ->
                    val isDragging = index == dragDropState.dragState.draggedItemIndex
                    var itemSize by remember { mutableStateOf(IntSize.Zero) }
                    var itemPosition by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                itemSize = coordinates.size
                                itemPosition = coordinates.positionInParent()
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        currentDragPosition.value = offset
                                        dragDropState.onDragStart(
                                            itemPosition = itemPosition,
                                            touchOffset = offset,  // Pass the touch offset
                                            itemIndex = index,
                                            item = item,
                                            itemSize = itemSize
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentDragPosition.value =
                                            currentDragPosition.value?.plus(dragAmount)
                                        dragDropState.onDrag(dragAmount, listState)
                                    },
                                    onDragEnd = {
                                        currentDragPosition.value = null
                                        dragDropState.onDragEnd()
                                    },
                                    onDragCancel = {
                                        currentDragPosition.value = null
                                        dragDropState.onDragEnd()
                                    }
                                )
                            }
                            .graphicsLayer {
                                if (isDragging) {
                                    alpha = 0f
                                }
                            }
                    ) {
                        itemContent(item, isDragging)
                    }
                }
            }

            // Dragged item overlay
            dragDropState.dragState.draggedItem?.let { draggedItem ->
                val initialTouchOffset = dragDropState.dragState.initialTouchOffset
                Box(
                    modifier = Modifier
                        .offset {
                            val x = (currentDragPosition.value?.x ?: 0f).toInt() +
                                    (dragDropState.dragState.dragPosition.x - initialTouchOffset.x).toInt()
                            val y = (currentDragPosition.value?.y ?: 0f).toInt() +
                                    (dragDropState.dragState.dragPosition.y - initialTouchOffset.y).toInt()
                            IntOffset(x, y)
                        }
                        .zIndex(1f)
                ) {
                    itemContent(draggedItem as T, true)
                }
            }
        }
    }
}

@Composable
fun DropTarget(
    modifier: Modifier = Modifier,
    onDrop: (Any?) -> Unit,
    content: @Composable () -> Unit
) {
    val dragDropState = LocalDragDropState.current
    var isInBounds by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val rect = coordinates.boundsInWindow()
                val dragPos = dragDropState.dragPosition + dragDropState.dragOffset
                isInBounds = rect.contains(dragPos)

                if (isInBounds && !dragDropState.isDragging) {
                    dragDropState.draggedItem?.let { onDrop(it) }
                }
            }
    ) {
        content()
    }
}
