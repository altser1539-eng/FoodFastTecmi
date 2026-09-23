package com.example.foodfast.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun QRCodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 200
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier.size(sizeDp.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSize = 15
                val cellSize = size.width / gridSize

                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        // QR positioning corner squares (top-left, top-right, bottom-left)
                        val isTopLeftCorner = row in 0..4 && col in 0..4
                        val isTopRightCorner = row in 0..4 && col in (gridSize - 5) until gridSize
                        val isBottomLeftCorner = row in (gridSize - 5) until gridSize && col in 0..4
                        val isCorner = isTopLeftCorner || isTopRightCorner || isBottomLeftCorner

                        val isCornerBorder = (isTopLeftCorner && (row == 0 || row == 4 || col == 0 || col == 4)) ||
                                (isTopRightCorner && (row == 0 || row == 4 || col == gridSize - 5 || col == gridSize - 1)) ||
                                (isBottomLeftCorner && (row == gridSize - 5 || row == gridSize - 1 || col == 0 || col == 4))

                        val isCornerCenter = (isTopLeftCorner && row == 2 && col == 2) ||
                                (isTopRightCorner && row == 2 && col == gridSize - 3) ||
                                (isBottomLeftCorner && row == gridSize - 3 && col == 2)

                        val cellHash = abs((data + "$row-$col").hashCode())
                        val isFilled = isCornerBorder || isCornerCenter || (!isCorner && cellHash % 3 != 0)

                        if (isFilled) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }
        }
    }
}
