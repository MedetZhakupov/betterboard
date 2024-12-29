package dev.medetzhakupov

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen() {
    val board = remember {
        mutableMapOf(
            "TO DO" to listOf("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11", "A12", "A13", "A14", "A15", "A16"),
            "IN PROGRESS" to listOf("B1", "B2", "B3", "B4", "B5", "B6"),
            "DONE" to listOf("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8"),
        )
    }

    val activeBoard = remember { board.keys.first() }

    Row {
        Column(
            modifier = Modifier.wrapContentWidth().verticalScroll(
                rememberScrollState()
            ),
        ) {
            board.forEach { (title, _) ->
                Text(
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .rotateLayout(Rotation.ROT_270)
                        .background(
                            if (title == activeBoard) MaterialTheme.colors.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colors.surface
                        )
                        .padding(16.dp),
                    text = title
                )
            }
        }

        board[activeBoard]?.also {
            DraggableLazyColumn(
                items = it,
                onMove = { fromIndex, toIndex ->
                    board[activeBoard] = it.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                },
                onDropOutside = { item, position ->
                    // Handle dropping outside
                },
                modifier = Modifier.weight(1f).background(MaterialTheme.colors.primary.copy(alpha = 0.5f))
            ) { item, isDragging ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = if (isDragging) 8.dp else 2.dp
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
