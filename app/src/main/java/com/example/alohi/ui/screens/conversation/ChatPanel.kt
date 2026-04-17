package com.example.alohi.ui.screens.conversation

/**
 * Chat panel states for the conversation bottom area.
 * Controls which panel is visible below the message composer.
 */
enum class ChatPanel {
    NONE,       // No panel — keyboard may be visible
    STICKER,    // Sticker/Emoji grid
    ATTACHMENT, // Attachment options (file, location, etc.)
    GALLERY,    // Camera + recent photos
    VOICE,      // Voice recording
}
