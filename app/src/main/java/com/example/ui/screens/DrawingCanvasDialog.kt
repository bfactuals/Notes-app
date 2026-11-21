package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class PathLine(val points: List<androidx.compose.ui.geometry.Offset>, val color: Color, val strokeWidth: Float)

@Composable
fun DrawingCanvasDialog(
  onDismiss: () -> Unit,
  onSaveDoodle: (String) -> Unit
) {
  var lines by remember { mutableStateOf(listOf<PathLine>()) }
  var currentLine by remember { mutableStateOf<MutableList<androidx.compose.ui.geometry.Offset>>(mutableListOf()) }
  var currentColor by remember { mutableStateOf(Color(0xFF5B5BD6)) }
  var currentWidth by remember { mutableStateOf(8f) }

  val colors = listOf(
    Color(0xFF1B1B2F),
    Color(0xFF5B5BD6),
    Color(0xFFE53935),
    Color(0xFF43A047),
    Color(0xFFFB8C00),
    Color(0xFF8E24AA),
    Color(0xFF00ACC1),
    Color(0xFF795548)
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_drawing_button")) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
          Text("Drawing Canvas", style = MaterialTheme.typography.titleMedium)
          Row {
            IconButton(
              onClick = { lines = listOf(); currentLine = mutableListOf() },
              modifier = Modifier.testTag("clear_canvas_button")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Clear")
            }
            IconButton(
              onClick = {
                onSaveDoodle("[Doodle with ${lines.size} strokes]")
                onDismiss()
              },
              modifier = Modifier.testTag("save_drawing_button")
            ) {
              Icon(Icons.Default.Check, contentDescription = "Save")
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .pointerInput(Unit) {
              detectDragGestures(
                onDragStart = { offset ->
                  currentLine = mutableListOf(offset)
                },
                onDrag = { change, _ ->
                  currentLine.add(change.position)
                },
                onDragEnd = {
                  if (currentLine.isNotEmpty()) {
                    lines = lines + PathLine(currentLine.toList(), currentColor, currentWidth)
                    currentLine = mutableListOf()
                  }
                }
              )
            }
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            for (line in lines) {
              if (line.points.size > 1) {
                val path = Path().apply {
                  moveTo(line.points[0].x, line.points[0].y)
                  for (i in 1 until line.points.size) {
                    lineTo(line.points[i].x, line.points[i].y)
                  }
                }
                drawPath(
                  path = path,
                  color = line.color,
                  style = Stroke(width = line.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
              }
            }
            if (currentLine.size > 1) {
              val path = Path().apply {
                moveTo(currentLine[0].x, currentLine[0].y)
                for (i in 1 until currentLine.size) {
                  lineTo(currentLine[i].x, currentLine[i].y)
                }
              }
              drawPath(
                path = path,
                color = currentColor,
                style = Stroke(width = currentWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
              )
            }
          }
        }

        Surface(
          modifier = Modifier.fillMaxWidth(),
          tonalElevation = 4.dp
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              colors.forEach { col ->
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(col)
                    .border(
                      width = if (currentColor == col) 3.dp else 0.dp,
                      color = MaterialTheme.colorScheme.onSurface,
                      shape = CircleShape
                    )
                    .clickable { currentColor = col }
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(4f, 8f, 16f).forEach { width ->
                Button(
                  onClick = { currentWidth = width },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentWidth == width) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                  ),
                  modifier = Modifier.size(36.dp),
                  contentPadding = PaddingValues(0.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size((width * 1.5f).dp)
                      .clip(CircleShape)
                      .background(if (currentWidth == width) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
