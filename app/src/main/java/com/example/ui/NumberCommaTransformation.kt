package com.example.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class NumberCommaTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        
        val isNegative = originalText.startsWith("-")
        val cleanText = originalText.replace("-", "").replace(",", "")
        
        if (cleanText.isEmpty()) {
             return TransformedText(text, OffsetMapping.Identity)
        }

        val parts = cleanText.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""

        val formattedInteger = try {
            val number = integerPart.toLong()
            DecimalFormat("#,###").format(number)
        } catch (e: Exception) {
            integerPart
        }

        val formattedText = (if (isNegative) "-" else "") + formattedInteger + decimalPart
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (originalText.isEmpty()) return 0
                val safeOffset = offset.coerceIn(0, originalText.length)
                
                var transformedOffset = 0
                var originalCount = 0
                
                while (originalCount < safeOffset && transformedOffset < formattedText.length) {
                    if (formattedText[transformedOffset] == ',') {
                        transformedOffset++
                    } else {
                        originalCount++
                        transformedOffset++
                    }
                }
                return transformedOffset.coerceIn(0, formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (formattedText.isEmpty()) return 0
                val safeOffset = offset.coerceIn(0, formattedText.length)
                
                var originalOffset = 0
                var transformedCount = 0
                
                while (transformedCount < safeOffset && originalOffset < originalText.length) {
                    if (formattedText[transformedCount] == ',') {
                        transformedCount++
                    } else {
                        originalOffset++
                        transformedCount++
                    }
                }
                return originalOffset.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
