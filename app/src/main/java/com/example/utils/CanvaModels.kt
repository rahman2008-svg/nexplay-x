package com.example.utils

import java.util.UUID

sealed class CanvaElement {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float
    abstract val scale: Float
    abstract val rotation: Float

    abstract fun updatePosition(newX: Float, newY: Float): CanvaElement
    abstract fun updateScale(newScale: Float): CanvaElement
    abstract fun updateRotation(newRotation: Float): CanvaElement

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float,
        override val y: Float,
        override val scale: Float = 1f,
        override val rotation: Float = 0f,
        val text: String,
        val colorHex: String = "#FFFFFF",
        val fontSizeSp: Float = 28f,
        val fontFamily: String = "Display Bold",
        val hasShadow: Boolean = true,
        val hasGradient: Boolean = false,
        val gradientEndHex: String = "#FF1F7E",
        val strokeWidth: Float = 0f
    ) : CanvaElement() {
        override fun updatePosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun updateScale(newScale: Float) = copy(scale = newScale)
        override fun updateRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    data class Sticker(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float,
        override val y: Float,
        override val scale: Float = 1f,
        override val rotation: Float = 0f,
        val emoji: String
    ) : CanvaElement() {
        override fun updatePosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun updateScale(newScale: Float) = copy(scale = newScale)
        override fun updateRotation(newRotation: Float) = copy(rotation = newRotation)
    }

    data class Shape(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float,
        override val y: Float,
        override val scale: Float = 1f,
        override val rotation: Float = 0f,
        val shapeType: String, // "Circle", "Rectangle", "Triangle", "Star", "Card"
        val colorHex: String = "#00E5FF",
        val isOutline: Boolean = false,
        val width: Float = 120f,
        val height: Float = 80f
    ) : CanvaElement() {
        override fun updatePosition(newX: Float, newY: Float) = copy(x = newX, y = newY)
        override fun updateScale(newScale: Float) = copy(scale = newScale)
        override fun updateRotation(newRotation: Float) = copy(rotation = newRotation)
    }
}

data class CanvaTemplate(
    val name: String,
    val category: String, // "YouTube", "Facebook", "Instagram", "Flyer", "Poster"
    val backgroundHex: String,
    val elements: List<CanvaElement>
)

object CanvaTemplates {
    val predefinedFonts = listOf(
        "Display Bold",
        "Space Grotesk",
        "JetBrains Mono",
        "Playful Serif",
        "Elegant Cursive",
        "Brutalist Impact",
        "Helvetica Neo",
        "Neon Digital"
    )

    val predefinedEmojis = listOf(
        "✨", "🔥", "📸", "🎨", "🌟", "📱", "🚀", "💡", "🌈", "💥", "❤️", "🎯", "🎵", "📌", "🍔", "🍿", "🎉", "👑", "🍕", "🦾"
    )

    val predefinedColors = listOf(
        "#FFFFFF", "#000000", "#FF1F7E", "#00E5FF", "#FFEB3B", "#4CAF50", "#9C27B0", "#FF5722", "#3F51B5", "#00BCD4"
    )

    val templatesList = listOf(
        CanvaTemplate(
            name = "Tech Vlog Hero",
            category = "YouTube Thumbnail",
            backgroundHex = "#120224",
            elements = listOf(
                CanvaElement.Shape(x = 100f, y = 140f, scale = 1.3f, shapeType = "Card", colorHex = "#FF1F7E", width = 360f, height = 240f),
                CanvaElement.Text(x = 120f, y = 200f, text = "NEXT GEN TECH", fontSizeSp = 36f, colorHex = "#00E5FF", fontFamily = "Brutalist Impact", hasGradient = true),
                CanvaElement.Text(x = 120f, y = 280f, text = "Lensora Studio X Launch", fontSizeSp = 24f, colorHex = "#FFFFFF", fontFamily = "Space Grotesk"),
                CanvaElement.Sticker(x = 350f, y = 120f, scale = 1.6f, emoji = "🚀"),
                CanvaElement.Sticker(x = 100f, y = 350f, scale = 1.2f, emoji = "✨")
            )
        ),
        CanvaTemplate(
            name = "Inspirational Quote",
            category = "Facebook Post",
            backgroundHex = "#1D2D44",
            elements = listOf(
                CanvaElement.Shape(x = 150f, y = 150f, scale = 1f, shapeType = "Circle", colorHex = "#FFEB3B"),
                CanvaElement.Text(x = 120f, y = 220f, text = "“A journey of 1000 miles\nbegins with standard step”", fontSizeSp = 22f, colorHex = "#FFFFFF", fontFamily = "Playful Serif"),
                CanvaElement.Text(x = 120f, y = 350f, text = "- Lao Tzu", fontSizeSp = 16f, colorHex = "#00E5FF", fontFamily = "JetBrains Mono"),
                CanvaElement.Sticker(x = 380f, y = 320f, scale = 1.4f, emoji = "💡")
            )
        ),
        CanvaTemplate(
            name = "Minimal Aesthetic Glow",
            category = "Instagram Story",
            backgroundHex = "#0D1117",
            elements = listOf(
                CanvaElement.Shape(x = 50f, y = 50f, scale = 2f, shapeType = "Rectangle", colorHex = "#9C27B0", isOutline = true, width = 200f, height = 400f),
                CanvaElement.Text(x = 100f, y = 280f, text = "CREATIVE\nMINDSET", fontSizeSp = 38f, colorHex = "#FFFFFF", fontFamily = "Space Grotesk"),
                CanvaElement.Text(x = 100f, y = 420f, text = "Swipe up to create offline", fontSizeSp = 16f, colorHex = "#FF1F7E", fontFamily = "Elegant Cursive"),
                CanvaElement.Sticker(x = 240f, y = 250f, scale = 2f, emoji = "🎨")
            )
        ),
        CanvaTemplate(
            name = "Business Event Flyer",
            category = "Flyer",
            backgroundHex = "#050C1A",
            elements = listOf(
                CanvaElement.Shape(x = 40f, y = 100f, scale = 1f, shapeType = "Card", colorHex = "#3F51B5", width = 420f, height = 300f),
                CanvaElement.Text(x = 80f, y = 150f, text = "SUMMIT 2026", fontSizeSp = 40f, colorHex = "#00E5FF", fontFamily = "Brutalist Impact"),
                CanvaElement.Text(x = 80f, y = 240f, text = "Innovation. Design. offline Security.", fontSizeSp = 18f, colorHex = "#FFFFFF"),
                CanvaElement.Text(x = 80f, y = 310f, text = "Venue: Dhaka, BD | Date: Dec 12", fontSizeSp = 14f, colorHex = "#FFEB3B", fontFamily = "JetBrains Mono"),
                CanvaElement.Sticker(x = 380f, y = 310f, scale = 1.3f, emoji = "🎯")
            )
        ),
        CanvaTemplate(
            name = "Cyber Retro Night",
            category = "Poster",
            backgroundHex = "#000000",
            elements = listOf(
                CanvaElement.Shape(x = 100f, y = 80f, scale = 1.2f, shapeType = "Triangle", colorHex = "#FF1F7E", isOutline = true),
                CanvaElement.Text(x = 60f, y = 180f, text = "METAVERSE", fontSizeSp = 48f, colorHex = "#00E5FF", fontFamily = "Neon Digital", hasGradient = true),
                CanvaElement.Text(x = 60f, y = 280f, text = "No Internet Required", fontSizeSp = 20f, colorHex = "#FFFFFF", fontFamily = "JetBrains Mono"),
                CanvaElement.Sticker(x = 250f, y = 100f, scale = 1.5f, emoji = "🔥")
            )
        )
    )
}
