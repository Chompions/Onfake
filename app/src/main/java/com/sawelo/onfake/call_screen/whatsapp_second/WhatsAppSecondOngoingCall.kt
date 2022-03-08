package com.sawelo.onfake.call_screen.whatsapp_second

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.request.RequestOptions
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sawelo.onfake.R
import com.sawelo.onfake.call_screen.CanvasButton
import com.sawelo.onfake.call_screen.EncryptedText
import com.sawelo.onfake.call_screen.NameText
import com.sawelo.onfake.data_class.ContactData
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterialApi::class)
@Preview(
    showBackground = true,
    device = Devices.PIXEL_3A
)
@Composable
fun WhatsAppSecondOngoingCall(
    activity: Activity? = null,
    contactData: ContactData = ContactData(),
    callback: (() -> Unit)? = null,
) {
    if (callback != null) {
        callback()
    }

    if (activity != null) {
        val systemUiController = rememberSystemUiController()
        systemUiController.apply {
            setNavigationBarColor(
                color = Color(0xFF125C4D)
            )
            setStatusBarColor(
                color = Color(0xFF008069)
            )
        }
    }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )

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
    
    BottomSheetScaffold(
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetGesturesEnabled = true,
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = 100.dp,
        sheetBackgroundColor = Color(0xFF125C4D),
        sheetContent = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 30.dp, end = 20.dp)
            ) {
                Box(
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_expand_up),
                        "Expand",
                        tint = Color(0xFF598C83),
                        modifier = Modifier
                            .size(34.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .offset(y = (30).dp)
                            .fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            "Speaker",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Icon(
                            Icons.Default.Videocam,
                            "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Icon(
                            Icons.Default.MicOff,
                            "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        CanvasButton(
                            icon = Icons.Default.CallEnd,
                            iconColor = Color.White,
                            iconSize = 35F,
                            backgroundColor = Color.Red,
                            modifier = Modifier
                                .scale(.8F)
                                .clickable {
                                    activity?.finish()
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
                Divider(thickness = 1.dp, color = Color(0xFF186152))

                Column(
                    modifier = Modifier.offset(x = (-25).dp)
                ) {
                    ListItem(
                        modifier = Modifier
                            .padding(vertical = 5.dp),
                        icon = {
                            CanvasButton(
                                modifier = Modifier.size(45.dp),
                                icon = Icons.Default.PersonAdd,
                                iconColor = Color.White,
                                backgroundColor = Color(0xFF008069))
                        }
                    ) {
                        Text(
                            "Add Participant",
                            color = Color.White,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    ListItem(
                        modifier = Modifier
                            .padding(vertical = 5.dp),
                        icon = {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier.size(45.dp),
                                color = MaterialTheme.colors.onSurface.copy(alpha = .2f)
                            ) {
                                if (!LocalView.current.isInEditMode) {
                                    GlideImage(
                                        imageModel = contactData.photoBitmap,
                                        requestOptions = {
                                            RequestOptions().override(500, 500)
                                        },
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            contactData.name,
                            color = Color.White,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        color = Color(0xFF008069)
                    )
                    .fillMaxWidth()
                    .weight(8f)
                    .padding(
                        top = 8.dp,
                        bottom = 10.dp,
                        start = 10.dp,
                        end = 10.dp,
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        "Expand",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(size = 40.dp)
                    )
                    EncryptedText()
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        "Expand",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(8.dp)
                            .size(size = 30.dp)
                    )
                }
                Box(
                    Modifier
                        .weight(3f)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NameText(
                        name = contactData.name,
                    )
                }
                Box(
                    Modifier
                        .weight(2f)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${stringMin}:${stringSec}",
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
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
                    if (!LocalView.current.isInEditMode) {
                        GlideImage(
                            imageModel = contactData.photoBitmap,
                            requestOptions = {
                                RequestOptions().override(1000 , 1000)
                            },
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .background(
                        color = Color.Black
                    )
                    .fillMaxWidth()
                    .weight(4f)
            )
        }
    }

    
}
