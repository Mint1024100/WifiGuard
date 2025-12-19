package com.wifiguard.core.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text

/**
 * Замена устаревшего ClickableText: рендерим [Text] и обрабатываем клики по StringAnnotation.
 *
 * Важно: аннотации должны быть добавлены через [AnnotatedString.Builder.pushStringAnnotation].
 */
@Composable
fun AnnotatedLinkText(
    text: AnnotatedString,
    tag: String,
    style: TextStyle,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    var layoutResult: TextLayoutResult? by remember { mutableStateOf(null) }

    Text(
        text = text,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
        inlineContent = inlineContent,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(text, tag) {
            detectTapGestures { position ->
                val result = layoutResult ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(position)
                text.getStringAnnotations(tag = tag, start = offset, end = offset)
                    .firstOrNull()
                    ?.let { annotation -> onLinkClick(annotation.item) }
            }
        }
    )
}

