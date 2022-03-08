package com.sawelo.onfake.call_screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawelo.onfake.R

@Composable
fun EncryptedText(modifier: Modifier = Modifier) {
    val encryptedId = "ENCRYPTED_ID"
    val encryptedText = buildAnnotatedString {
        appendInlineContent(encryptedId, "[Lock]")
        append("  End-to-end encrypted")
    }
    val encryptedInlineContent = mapOf(
        Pair(
            encryptedId,
            InlineTextContent(
                Placeholder(
                    width = 14.sp,
                    height = 14.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_lock),
                    "Lock",
                    tint = Color.White.copy(alpha = .6f),
                )
            }
        )
    )

    Text(
        modifier = modifier,
        text = encryptedText,
        inlineContent = encryptedInlineContent,
        fontSize = 14.sp,
        color = Color.White.copy(alpha = .6f)
    )
}

@Composable
fun NameText(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        fontSize = 28.sp,
        color = Color.White,
        fontWeight = FontWeight.W400,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun CanvasButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    iconSize: Float = 30F,
    backgroundColor: Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier.size(60.dp),
            onDraw = {
                drawCircle(
                    color = backgroundColor
                )
            }
        )
        Icon(
            icon,
            "Button",
            modifier = Modifier.size(iconSize.dp).align(Alignment.Center),
            tint = iconColor
        )
    }
}