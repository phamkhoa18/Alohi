package com.example.alohi.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════
// AloHi Shape System
// Apple-like rounded corners — soft, approachable
// ═══════════════════════════════════════════════════════

val AloHiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),       // Badges, chips
    small = RoundedCornerShape(8.dp),             // Small buttons
    medium = RoundedCornerShape(12.dp),           // Cards, text fields
    large = RoundedCornerShape(16.dp),            // Bottom sheets, dialogs
    extraLarge = RoundedCornerShape(24.dp)        // Modals, large cards
)

// ═══════════════════════════════════════════════════════
// Chat Bubble Shapes
// Zalo-style: tail on bottom corner of each side
// ═══════════════════════════════════════════════════════

// Sender (me) — tail at bottom-right
val SenderBubbleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 18.dp,
    bottomEnd = 4.dp       // Tail at bottom-right
)

// Receiver (other) — tail at bottom-left
val ReceiverBubbleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 4.dp,    // Tail at bottom-left
    bottomEnd = 18.dp
)

// ── Consecutive Bubble Shapes (small corners on the "side") ──

// Sender consecutive — small corners on right side
val SenderBubbleConsecutiveShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 4.dp,
    bottomStart = 18.dp,
    bottomEnd = 4.dp
)

// Receiver consecutive — small corners on left side
val ReceiverBubbleConsecutiveShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 18.dp,
    bottomStart = 4.dp,
    bottomEnd = 18.dp
)

// ── First-in-group shapes (rounded top, flat bottom-side corner) ──

val SenderBubbleFirstShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 18.dp,
    bottomEnd = 4.dp      // Flat on grouped side
)

val ReceiverBubbleFirstShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 4.dp,   // Flat on grouped side
    bottomEnd = 18.dp
)

// ── Middle-in-group shapes (flat on both corners of grouped side) ──

val SenderBubbleMiddleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 4.dp,
    bottomStart = 18.dp,
    bottomEnd = 4.dp
)

val ReceiverBubbleMiddleShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 18.dp,
    bottomStart = 4.dp,
    bottomEnd = 18.dp
)

// ── Last-in-group shapes (flat top-side, tail at bottom) ──

val SenderBubbleLastShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 4.dp,
    bottomStart = 18.dp,
    bottomEnd = 18.dp     // Rounded = end of group
)

val ReceiverBubbleLastShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 18.dp,
    bottomStart = 18.dp,  // Rounded = end of group
    bottomEnd = 18.dp
)

// ── Utility Shapes ──
val FullRoundedShape = RoundedCornerShape(50)    // Avatar, FAB, pills
val SearchBarShape = RoundedCornerShape(10.dp)   // iOS search bar
val BottomSheetShape = RoundedCornerShape(
    topStart = 14.dp,
    topEnd = 14.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
