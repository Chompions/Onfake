package com.sawelo.onfake.call_screen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
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
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.R
import com.sawelo.onfake.call_screen.whatsapp_first.WhatsAppFirstIncomingCall
import com.sawelo.onfake.call_screen.whatsapp_first.WhatsAppFirstOngoingCall
import com.sawelo.onfake.call_screen.whatsapp_second.WhatsAppSecondIncomingCall
import com.sawelo.onfake.call_screen.whatsapp_second.WhatsAppSecondOngoingCall
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.data_class.CallScreen
import com.sawelo.onfake.data_class.ContactData
import com.sawelo.onfake.receiver.DeclineReceiver
import com.sawelo.onfake.ui.theme.OnFakeTheme

class CallScreenActivity : ComponentActivity() {

    private var proximityWakeLock: PowerManager.WakeLock? = null
    private val callScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, receiverIntent: Intent?) {
            if (receiverIntent?.action == DESTROY_ACTIVITY) {
                finish()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(callScreenReceiver, IntentFilter(DESTROY_ACTIVITY))

        val profileData =
            intent?.getParcelableExtra(MainActivity.PROFILE_EXTRA) ?: CallProfileData()
        val isStartFromIncoming =
            intent.getBooleanExtra(MainActivity.IS_START_FROM_INCOMING_EXTRA, true)

        val contactData = ContactData(
            profileData.name,
            profileData.photoUri
        )

        showWhenLocked()
        setWakeLock()

        setContent {
            val navController = rememberNavController()
            OnFakeTheme {
                NavHost(
                    navController = navController,
                    startDestination = if (isStartFromIncoming) {
                        INCOMING_CALL_ROUTE
                    } else {
                        ONGOING_CALL_ROUTE
                    }
                ) {
                    when (profileData.callScreen) {
                        CallScreen.WHATSAPP_FIRST -> {
                            composable(INCOMING_CALL_ROUTE) {
                                WhatsAppFirstIncomingCall(
                                    activity = this@CallScreenActivity,
                                    navController = navController,
                                    isStartAnimation = true,
                                    contactData = contactData
                                )
                            }
                            composable(ONGOING_CALL_ROUTE) {
                                WhatsAppFirstOngoingCall(
                                    this@CallScreenActivity,
                                    contactData = contactData
                                )
                            }
                        }
                        CallScreen.WHATSAPP_SECOND -> {
                            composable(INCOMING_CALL_ROUTE) {
                                WhatsAppSecondIncomingCall(
                                    activity = this@CallScreenActivity,
                                    navController = navController,
                                    isStartAnimation = true,
                                    contactData = contactData
                                )
                            }
                            composable(ONGOING_CALL_ROUTE) {
                                WhatsAppSecondOngoingCall(
                                    this@CallScreenActivity,
                                    contactData = contactData
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showWhenLocked() {
        // Ensure CallActivity will run when the phone is locked or the screen is off
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setTranslucent(true)
            }
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }

        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@CallScreenActivity, null)
            }
        }
    }

    private fun setWakeLock() {
        // Setting up display switch (on/off) during call
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        proximityWakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "infake::proximity_wake_lock"
        )
        proximityWakeLock?.acquire(10 * 60 * 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(callScreenReceiver)

        if (proximityWakeLock?.isHeld == true) proximityWakeLock?.release()

        // Stop all services before after finishing this activity
        val declineIntent = Intent(this, DeclineReceiver::class.java)
        sendBroadcast(declineIntent)

        Log.d("Destroy", "CallScreenActivity is destroyed")
    }

    companion object {
        const val INCOMING_CALL_ROUTE = "incoming_call"
        const val ONGOING_CALL_ROUTE = "ongoing_call"
        const val DESTROY_ACTIVITY = "destroy_activity"
    }
}


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
            modifier = Modifier.size(iconSize.dp),
            tint = iconColor
        )
    }
}