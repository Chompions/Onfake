package com.sawelo.onfake

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.request.RequestOptions
import com.sawelo.onfake.receiver.DeclineReceiver
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.delay

@Composable
fun FirstWhatsAppOngoingCall(
    activity: Activity?,
) {
    val context = LocalContext.current

    // Send broadcast to cancel everything
    val declineIntent = Intent(context, DeclineReceiver::class.java)
    context.sendBroadcast(declineIntent)

    var intSec by remember { mutableStateOf(0) }
    var intMin by remember { mutableStateOf(0) }
    var stringSec by remember { mutableStateOf("00") }
    var stringMin by remember { mutableStateOf("0") }

    LaunchedEffect(null) {
        while (true) {
            when {
                (intSec < 9) -> {
                    intSec++
                    stringSec = "0$intSec"
                }
                (intSec == 59) -> {
                    intSec = 0
                    intMin++
                    stringSec = "0$intSec"
                    stringMin = "$intMin"
                }
                else -> {
                    intSec++
                    stringSec = "$intSec"
                }
            }
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    color = Color(0xFF004B44)
                )
                .weight(8f)
                .fillMaxWidth()
                .padding(
                    top = 10.dp,
                    bottom = 2.dp,
                    start = 6.dp,
                    end = 6.dp,
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    "Expand",
                    tint = Color.White,
                    modifier = Modifier.size(size = 40.dp)
                )
                EncryptedText()
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    "Expand",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(size = 28.dp)
                )
            }
            NameText(
                name = LocalContact.current.name,
                Modifier.padding(top = 8.dp),
            )
            Text(
                "${stringMin}:${stringSec}",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .weight(25f)
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                GlideImage(
                    imageModel = LocalContact.current.photoBitmap,
                    requestOptions = {
                        RequestOptions().override(500 , 500)
                    },
                    contentScale = ContentScale.Crop,
                )
            }
            CanvasButton(
                icon = Icons.Default.CallEnd,
                iconColor = Color.White,
                backgroundColor = Color.Red,
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .clickable {
                        activity?.finish()
                    }
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .background(
                    color = Color(0xFF004B44)
                )
                .weight(4f)
                .fillMaxWidth()
        ) {
            Icon(
                Icons.Default.VolumeUp,
                "Speaker",
                tint = Color(0xFFB4CAC7),
                modifier = Modifier.size(30.dp)
            )
            Icon(
                Icons.Default.Videocam,
                "Camera",
                tint = Color(0xFFB4CAC7),
                modifier = Modifier.size(33.dp)
            )
            Icon(
                Icons.Default.MicOff,
                "Mic",
                tint = Color(0xFFB4CAC7),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
