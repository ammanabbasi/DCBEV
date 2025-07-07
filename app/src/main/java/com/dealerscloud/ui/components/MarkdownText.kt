package com.dealerscloud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable that renders markdown-formatted text with support for:
 * - **Bold** text
 * - *Italic* text  
 * - `Inline code`
 * - Code blocks with syntax highlighting
 * - Bullet points
 * - Numbered lists
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    val formattedContent = parseMarkdown(text)
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        formattedContent.forEach { element ->
            when (element) {
                is MarkdownElement.Text -> {
                    SelectionContainer {
                        Text(
                            text = element.annotatedString,
                            color = color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is MarkdownElement.CodeBlock -> {
                    CodeBlock(
                        code = element.code,
                        language = element.language
                    )
                }
                is MarkdownElement.BulletPoint -> {
                    Row(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = "? ",
                            color = color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SelectionContainer {
                            Text(
                                text = element.annotatedString,
                                color = color,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is MarkdownElement.NumberedItem -> {
                    Row(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = "${element.number}. ",
                            color = color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SelectionContainer {
                            Text(
                                text = element.annotatedString,
                                color = color,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(
    code: String,
    language: String
) {
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header with language and copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifEmpty { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Code content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private sealed class MarkdownElement {
    data class Text(val annotatedString: AnnotatedString) : MarkdownElement()
    data class CodeBlock(val code: String, val language: String) : MarkdownElement()
    data class BulletPoint(val annotatedString: AnnotatedString) : MarkdownElement()
    data class NumberedItem(val number: Int, val annotatedString: AnnotatedString) : MarkdownElement()
}

private fun parseMarkdown(text: String): List<MarkdownElement> {
    val elements = mutableListOf<MarkdownElement>()
    val lines = text.lines()
    var i = 0
    
    while (i < lines.size) {
        val line = lines[i]
        
        when {
            // Code block detection
            line.startsWith("```") -> {
                val language = line.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                
                while (i < lines.size && !lines[i].startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                
                if (codeLines.isNotEmpty()) {
                    elements.add(
                        MarkdownElement.CodeBlock(
                            code = codeLines.joinToString("\n"),
                            language = language
                        )
                    )
                }
            }
            
            // Bullet point detection
            line.trimStart().startsWith("? ") || 
            line.trimStart().startsWith("- ") ||
            line.trimStart().startsWith("* ") -> {
                val content = line.trimStart().removePrefix("? ")
                    .removePrefix("- ")
                    .removePrefix("* ")
                elements.add(
                    MarkdownElement.BulletPoint(
                        formatInlineMarkdown(content)
                    )
                )
            }
            
            // Numbered list detection
            line.trimStart().matches(Regex("^\\d+\\.\\s+.*")) -> {
                val match = Regex("^(\\d+)\\.\\s+(.*)").find(line.trimStart())
                if (match != null) {
                    val (_, number, content) = match.groupValues
                    elements.add(
                        MarkdownElement.NumberedItem(
                            number = number.toInt(),
                            annotatedString = formatInlineMarkdown(content)
                        )
                    )
                }
            }
            
            // Regular text
            else -> {
                if (line.isNotBlank()) {
                    elements.add(
                        MarkdownElement.Text(
                            formatInlineMarkdown(line)
                        )
                    )
                }
            }
        }
        
        i++
    }
    
    return elements
}

private fun formatInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        
        // Regex patterns for inline formatting
        val patterns = listOf(
            // Bold
            Regex("\\*\\*([^*]+)\\*\\*") to { match: MatchResult ->
                appendRange(text, currentIndex, match.range.first)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
                currentIndex = match.range.last + 1
            },
            // Italic
            Regex("\\*([^*]+)\\*") to { match: MatchResult ->
                appendRange(text, currentIndex, match.range.first)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[1])
                }
                currentIndex = match.range.last + 1
            },
            // Inline code
            Regex("`([^`]+)`") to { match: MatchResult ->
                appendRange(text, currentIndex, match.range.first)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = Color(0xFFE8E8E8)
                    )
                ) {
                    append(" ${match.groupValues[1]} ")
                }
                currentIndex = match.range.last + 1
            }
        )
        
        // Process all patterns
        val allMatches = patterns.flatMap { (regex, _) ->
            regex.findAll(text).map { it to regex }
        }.sortedBy { it.first.range.first }
        
        for ((match, regex) in allMatches) {
            if (match.range.first >= currentIndex) {
                patterns.find { it.first == regex }?.second?.invoke(match)
            }
        }
        
        // Append remaining text
        if (currentIndex < text.length) {
            appendRange(text, currentIndex, text.length)
        }
    }
}

private fun AnnotatedString.Builder.appendRange(text: String, start: Int, end: Int) {
    if (start < end && start >= 0 && end <= text.length) {
        append(text.substring(start, end))
    }
}