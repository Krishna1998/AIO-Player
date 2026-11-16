/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MpvScriptEditor(
  content: String,
  onContentChange: (String) -> Unit,
  language: String,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val latestOnContentChange by rememberUpdatedState(onContentChange)

  var textFieldValue by remember {
    mutableStateOf(TextFieldValue(content, selection = TextRange(content.length)))
  }

  LaunchedEffect(content) {
    if (content != textFieldValue.text) {
      textFieldValue = textFieldValue.copy(
        text = content,
        selection = TextRange(content.length.coerceAtMost(textFieldValue.selection.end)),
      )
    }
  }

  var textSizeSp by rememberSaveable { mutableStateOf(14) }
  var wordWrap by rememberSaveable { mutableStateOf(true) }
  var showSearch by rememberSaveable { mutableStateOf(false) }
  var findText by rememberSaveable { mutableStateOf("") }
  var replaceText by rememberSaveable { mutableStateOf("") }
  var showGoToLine by rememberSaveable { mutableStateOf(false) }
  var targetLine by rememberSaveable { mutableStateOf("") }

  val verticalScrollState = rememberScrollState()
  val horizontalScrollState = rememberScrollState()

  val lines = remember(textFieldValue.text) {
    val count = textFieldValue.text.count { it == '\n' } + 1
    (1..count).joinToString("\n")
  }

  Column(modifier = modifier.fillMaxSize()) {
    // Editor body with line numbers
    Row(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(colors.surface)
        .verticalScroll(verticalScrollState),
    ) {
      // Line numbers column
      Text(
        text = lines,
        fontFamily = FontFamily.Monospace,
        fontSize = textSizeSp.sp,
        lineHeight = (textSizeSp * 1.4f).sp,
        color = colors.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
          .background(colors.surfaceContainerLow)
          .padding(horizontal = 8.dp, vertical = 8.dp)
          .widthIn(min = 36.dp),
      )

      // Text input area
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .then(
            if (!wordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier,
          )
          .padding(8.dp),
      ) {
        BasicTextField(
          value = textFieldValue,
          onValueChange = { newVal ->
            val textChanged = newVal.text != textFieldValue.text
            textFieldValue = newVal
            if (textChanged) {
              latestOnContentChange(newVal.text)
            }
          },
          textStyle = TextStyle(
            color = colors.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = textSizeSp.sp,
            lineHeight = (textSizeSp * 1.4f).sp,
          ),
          cursorBrush = SolidColor(colors.primary),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    HorizontalDivider(color = colors.outlineVariant)

    // Bottom toolbars: Search, Go To Line, Quick Actions
    Surface(
      color = colors.surfaceContainerLow,
      tonalElevation = 1.dp,
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (showSearch) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = findText,
              onValueChange = { findText = it },
              placeholder = { Text("Find") },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            TextButton(
              onClick = {
                if (findText.isNotBlank()) {
                  val currentPos = textFieldValue.selection.start
                  val idx = textFieldValue.text.lastIndexOf(findText, (currentPos - 1).coerceAtLeast(0), ignoreCase = true)
                  val target = if (idx >= 0) idx else textFieldValue.text.lastIndexOf(findText, ignoreCase = true)
                  if (target >= 0) {
                    textFieldValue = textFieldValue.copy(selection = TextRange(target, target + findText.length))
                  }
                }
              },
              contentPadding = PaddingValues(horizontal = 6.dp),
            ) { Text("↑") }
            TextButton(
              onClick = {
                if (findText.isNotBlank()) {
                  val currentPos = textFieldValue.selection.end
                  val idx = textFieldValue.text.indexOf(findText, currentPos, ignoreCase = true)
                  val target = if (idx >= 0) idx else textFieldValue.text.indexOf(findText, ignoreCase = true)
                  if (target >= 0) {
                    textFieldValue = textFieldValue.copy(selection = TextRange(target, target + findText.length))
                  }
                }
              },
              contentPadding = PaddingValues(horizontal = 6.dp),
            ) { Text("↓") }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = replaceText,
              onValueChange = { replaceText = it },
              placeholder = { Text("Replace with") },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            TextButton(
              onClick = {
                if (findText.isNotBlank()) {
                  val sel = textFieldValue.selection
                  if (!sel.collapsed && textFieldValue.text.substring(sel.min, sel.max).equals(findText, ignoreCase = true)) {
                    val newText = textFieldValue.text.replaceRange(sel.min, sel.max, replaceText)
                    textFieldValue = TextFieldValue(newText, selection = TextRange(sel.min + replaceText.length))
                    latestOnContentChange(newText)
                  } else {
                    val idx = textFieldValue.text.indexOf(findText, sel.end, ignoreCase = true)
                    val target = if (idx >= 0) idx else textFieldValue.text.indexOf(findText, ignoreCase = true)
                    if (target >= 0) {
                      textFieldValue = textFieldValue.copy(selection = TextRange(target, target + findText.length))
                    }
                  }
                }
              },
              contentPadding = PaddingValues(horizontal = 6.dp),
            ) { Text("Replace", style = MaterialTheme.typography.labelMedium) }
            TextButton(
              onClick = {
                if (findText.isNotBlank()) {
                  val newText = textFieldValue.text.replace(findText, replaceText, ignoreCase = true)
                  textFieldValue = TextFieldValue(newText, selection = TextRange(0))
                  latestOnContentChange(newText)
                }
              },
              contentPadding = PaddingValues(horizontal = 6.dp),
            ) { Text("All", style = MaterialTheme.typography.labelMedium) }
          }
        }
        if (showGoToLine) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = targetLine,
              onValueChange = { targetLine = it.filter(Char::isDigit).take(7) },
              placeholder = { Text("Line number") },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(
              onClick = {
                targetLine.toIntOrNull()?.let { lineNum ->
                  val lineList = textFieldValue.text.lines()
                  val targetIdx = (lineNum - 1).coerceIn(0, (lineList.size - 1).coerceAtLeast(0))
                  var charOffset = 0
                  for (i in 0 until targetIdx) {
                    charOffset += lineList[i].length + 1
                  }
                  textFieldValue = textFieldValue.copy(selection = TextRange(charOffset))
                }
                showGoToLine = false
              },
            ) { Text("Go") }
          }
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          TextButton(onClick = { showSearch = !showSearch }) { Text(if (showSearch) "Close" else "Find") }
          TextButton(onClick = { wordWrap = !wordWrap }) { Text(if (wordWrap) "No wrap" else "Wrap") }
          TextButton(
            onClick = {
              textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length))
            },
          ) { Text("Select all") }
          TextButton(
            onClick = {
              val sel = textFieldValue.selection
              val newText = textFieldValue.text.replaceRange(sel.min, sel.max, "    ")
              textFieldValue = TextFieldValue(newText, selection = TextRange(sel.min + 4))
              latestOnContentChange(newText)
            },
          ) { Text("Tab") }
          TextButton(
            onClick = {
              val sel = textFieldValue.selection
              val newText = textFieldValue.text.replaceRange(sel.min, sel.max, "#")
              textFieldValue = TextFieldValue(newText, selection = TextRange(sel.min + 1))
              latestOnContentChange(newText)
            },
          ) { Text("#") }
          TextButton(onClick = { showGoToLine = !showGoToLine }) { Text("Line") }
          TextButton(onClick = { textSizeSp = (textSizeSp - 1).coerceAtLeast(10) }) { Text("A−") }
          TextButton(onClick = { textSizeSp = (textSizeSp + 1).coerceAtMost(24) }) { Text("A+") }
        }
      }
    }
  }
}
