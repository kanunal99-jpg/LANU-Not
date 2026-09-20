package com.lanu.notdefteri.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

object RichTextHelper {

    /**
     * Parses Markdown syntax (**bold**, *italic*, # Header 1, ## Header 2, ### Header 3,
     * - Bullet points, ~~strikethrough~~, `code`) as well as HTML tags (<b>, <i>, <u>)
     * into Jetpack Compose AnnotatedString for rich visual rendering.
     */
    fun parseHtmlToAnnotatedString(
        text: String,
        primaryColor: Color = Color(0xFFC25D23),
        header2Color: Color = Color(0xFF1E293B),
        header3Color: Color = Color(0xFF475569),
        codeBackgroundColor: Color = Color(0xFFF1F5F9),
        codeTextColor: Color = Color(0xFF0F172A)
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val lines = text.split("\n")
        return buildAnnotatedString {
            lines.forEachIndexed { index, line ->
                parseLine(line, primaryColor, header2Color, header3Color, codeBackgroundColor, codeTextColor)
                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun AnnotatedString.Builder.parseLine(
        line: String,
        primaryColor: Color,
        header2Color: Color,
        header3Color: Color,
        codeBackgroundColor: Color,
        codeTextColor: Color
    ) {
        when {
            // Header 1: # Heading
            line.startsWith("# ") -> {
                val headerText = line.removePrefix("# ")
                val start = length
                append(headerText)
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = primaryColor
                    ),
                    start,
                    length
                )
            }
            // Header 2: ## Heading
            line.startsWith("## ") -> {
                val headerText = line.removePrefix("## ")
                val start = length
                append(headerText)
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = header2Color
                    ),
                    start,
                    length
                )
            }
            // Header 3: ### Heading
            line.startsWith("### ") -> {
                val headerText = line.removePrefix("### ")
                val start = length
                append(headerText)
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = header3Color
                    ),
                    start,
                    length
                )
            }
            // Bullet List: - Item or * Item or + Item
            line.matches(Regex("^\\s*[-*+]\\s+.*")) -> {
                val bulletContent = line.replaceFirst(Regex("^\\s*[-*+]\\s+"), "")
                val start = length
                append("• ")
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    ),
                    start,
                    length
                )
                parseInlineMarkdown(bulletContent, codeBackgroundColor, codeTextColor)
            }
            else -> {
                parseInlineMarkdown(line, codeBackgroundColor, codeTextColor)
            }
        }
    }

    private fun AnnotatedString.Builder.parseInlineMarkdown(
        text: String,
        codeBackgroundColor: Color,
        codeTextColor: Color
    ) {
        // Combined regex pattern for inline elements: HTML tags, bold, italic, strikethrough, code
        val pattern = Pattern.compile(
            "<b>(.*?)</b>|" +                    // 1: HTML bold
            "<i>(.*?)</i>|" +                    // 2: HTML italic
            "<u>(.*?)</u>|" +                    // 3: HTML underline
            "\\*\\*(.*?)\\*\\*|__(.*?)__|" +     // 4, 5: Markdown bold **bold** or __bold__
            "\\*(.*?)\\*|_(.*?)_|" +            // 6, 7: Markdown italic *italic* or _italic_
            "~~(.*?)~~|" +                      // 8: Markdown strikethrough ~~strike~~
            "`(.*?)`|" +                        // 9: Markdown inline code `code`
            "([^<*_~`]+|<|\\*|_|~|`)",          // 10: Plain text / fallback chars
            Pattern.DOTALL
        )

        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val htmlBold = matcher.group(1)
            val htmlItalic = matcher.group(2)
            val htmlUnderline = matcher.group(3)
            val mdBold = matcher.group(4) ?: matcher.group(5)
            val mdItalic = matcher.group(6) ?: matcher.group(7)
            val mdStrike = matcher.group(8)
            val mdCode = matcher.group(9)
            val plainText = matcher.group(10)

            when {
                htmlBold != null || mdBold != null -> {
                    val content = htmlBold ?: mdBold!!
                    val start = length
                    append(content)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                }
                htmlItalic != null || mdItalic != null -> {
                    val content = htmlItalic ?: mdItalic!!
                    val start = length
                    append(content)
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                }
                htmlUnderline != null -> {
                    val start = length
                    append(htmlUnderline)
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, length)
                }
                mdStrike != null -> {
                    val start = length
                    append(mdStrike)
                    addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
                }
                mdCode != null -> {
                    val start = length
                    append(mdCode)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackgroundColor,
                            color = codeTextColor
                        ),
                        start,
                        length
                    )
                }
                plainText != null -> {
                    append(plainText)
                }
            }
        }
    }

    /**
     * Toggles markdown wrapping markers (e.g. ** for bold, * for italic, ~~ for strikethrough, ` for code)
     */
    fun toggleMarkdownWrap(value: TextFieldValue, marker: String): TextFieldValue {
        val text = value.text
        val selection = value.selection
        val start = selection.min
        val end = selection.max

        return if (start == end) {
            // Empty selection: Insert e.g. ** ** and place cursor in middle
            val newText = text.substring(0, start) + marker + marker + text.substring(start)
            val newCursorPos = start + marker.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPos, newCursorPos)
            )
        } else {
            // Selection: Wrap selected text
            val selectedText = text.substring(start, end)
            val formatted = "$marker$selectedText$marker"
            val newText = text.substring(0, start) + formatted + text.substring(end)
            val newStart = start + marker.length
            val newEnd = newStart + selectedText.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newStart, newEnd)
            )
        }
    }

    /**
     * Prepends markdown prefix (e.g. "# ", "## ", "- ") to current line or selection
     */
    fun toggleLinePrefix(value: TextFieldValue, prefix: String): TextFieldValue {
        val text = value.text
        val selection = value.selection
        val cursorPos = selection.start

        // Find the start of the current line
        val lineStart = text.lastIndexOf('\n', cursorPos - 1).let { if (it == -1) 0 else it + 1 }

        val lineText = text.substring(lineStart)
        val hasPrefix = lineText.startsWith(prefix)

        val newText = if (hasPrefix) {
            // Remove prefix
            text.substring(0, lineStart) + lineText.removePrefix(prefix)
        } else {
            // Add prefix
            text.substring(0, lineStart) + prefix + lineText
        }

        val cursorOffset = if (hasPrefix) -prefix.length else prefix.length
        val newCursor = (cursorPos + cursorOffset).coerceIn(0, newText.length)

        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor, newCursor)
        )
    }

    /**
     * Legacy HTML formatting compatibility helper
     */
    fun toggleFormat(value: TextFieldValue, tag: String): TextFieldValue {
        val text = value.text
        val selection = value.selection
        val start = selection.min
        val end = selection.max

        return if (start == end) {
            val tagOpen = "<$tag>"
            val tagClose = "</$tag>"
            val newText = text.substring(0, start) + tagOpen + tagClose + text.substring(start)
            val newCursorPos = start + tagOpen.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPos, newCursorPos)
            )
        } else {
            val selectedText = text.substring(start, end)
            val tagOpen = "<$tag>"
            val tagClose = "</$tag>"
            val formatted = "$tagOpen$selectedText$tagClose"
            val newText = text.substring(0, start) + formatted + text.substring(end)
            val newSelectionStart = start + tagOpen.length
            val newSelectionEnd = newSelectionStart + selectedText.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newSelectionStart, newSelectionEnd)
            )
        }
    }
}
