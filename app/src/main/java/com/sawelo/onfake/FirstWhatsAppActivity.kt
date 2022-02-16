package com.sawelo.onfake

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.data_class.ContactData
import com.sawelo.onfake.receiver.DeclineReceiver
import com.sawelo.onfake.ui.theme.OnFakeTheme

class FirstWhatsAppActivity : ComponentActivity() {

    override fun onBackPressed() {
        super.onBackPressed()
        this.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Stop all services before starting this activity
        val declineIntent = Intent(this, DeclineReceiver::class.java)
        sendBroadcast(declineIntent)

        val profileData = intent?.getParcelableExtra(MainActivity.PROFILE_EXTRA) ?: CallProfileData()
        val startFromIncoming = intent.getBooleanExtra(MainActivity.START_FROM_INCOMING_EXTRA, true)

        val contactData = ContactData(
            profileData.name,
            profileData.photoUri
        )

        setContent {
            val navController = rememberNavController()
            OnFakeTheme {
                CompositionLocalProvider(
                    LocalContact provides contactData
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (startFromIncoming) {
                            incomingCallRoute
                        } else {
                            ongoingCallRoute
                        }
                    ) {
                        composable(incomingCallRoute) {
                            FirstWhatsAppIncomingCall(
                                activity = this@FirstWhatsAppActivity,
                                navController = navController,
                                inCallScreen = true
                            )
                        }
                        composable(ongoingCallRoute) {
                            FirstWhatsAppOngoingCall(
                                this@FirstWhatsAppActivity
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val incomingCallRoute = "INCOMING_CALL"
        const val ongoingCallRoute = "ONGOING_CALL"
    }
}

@Composable
fun EncryptedText() {
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
        fontSize = 30.sp,
        color = Color.White,
        fontWeight = FontWeight.W300,
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
            modifier = Modifier.size(iconSize.dp),
            tint = iconColor
        )
    }
}