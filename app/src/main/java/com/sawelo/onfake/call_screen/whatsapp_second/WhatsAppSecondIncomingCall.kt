package com.sawelo.onfake.call_screen.whatsapp_second

import android.app.Activity
import android.view.animation.Interpolator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.request.RequestOptions
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sawelo.onfake.R
import com.sawelo.onfake.CallScreenActivity
import com.sawelo.onfake.call_screen.CanvasButton
import com.sawelo.onfake.call_screen.NameText
import com.sawelo.onfake.data_class.ContactData
import com.skydoves.landscapist.glide.GlideImage
import kotlin.math.roundToInt
import kotlin.math.sin

class ShakingInterpolator(
    private val tension: Float
) : Interpolator {
    override fun getInterpolation(input: Float): Float {
        return (sin(tension * input) * sin(Math.PI * input)
                + input).toFloat()
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3A
)
@Composable
fun WhatsAppSecondIncomingCall(
    modifier: Modifier = Modifier,
    activity: Activity? = null,
    navController: NavController? = null,
    isStartAnimation: Boolean = false,
    contactData: ContactData = ContactData()
) {
    if (activity != null) {
        val systemUiController = rememberSystemUiController()
        systemUiController.setSystemBarsColor(
            color = Color(0xFF008069)
        )
    }

    Column(
        modifier = modifier.then(
            Modifier.background(color = Color(0xFF008069))
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(
                    vertical = 10.dp,
                    horizontal = 20.dp,
                )
                .weight(4f)
                .fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                elevation = 8.dp,
                modifier = Modifier
                    .padding(5.dp)
                    .size(100.dp),
                color = MaterialTheme.colors.onSurface.copy(alpha = .2f)
            ) {
                if (!LocalView.current.isInEditMode) {
                    GlideImage(
                        imageModel = contactData.photoBitmap,
                        loading = {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        },
                        requestOptions = {
                            RequestOptions().override(500, 500)
                        },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Box(
                Modifier
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                NameText(name = contactData.name)
            }
            Box(
                Modifier
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Whatsapp voice call",
                    fontSize = 17.sp,
                    color = Color.White,
                    fontWeight = FontWeight.W400,
                    )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .padding(30.dp)
                .weight(7f)
                .fillMaxWidth()
        ) {
            BottomButtons(activity, navController, isStartAnimation)
            Spacer(modifier = Modifier.size(20.dp))
            Text(
                text = "Swipe up to accept",
                color = Color.White.copy(alpha = .5f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun BottomButtons(
    activity: Activity? = null,
    navController: NavController? = null,
    isStartAnimation: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val arrowMoving by infiniteTransition.animateFloat(
        initialValue = 150f,
        targetValue = -150f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1000,
                0,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val offsetModifier: Modifier = if (isStartAnimation) {
        Modifier
            .size(24.dp, 135.dp)
            .clipToBounds()
            .offset(x = 0.dp, y = arrowMoving.dp)
    } else {
        Modifier
            .size(24.dp, 135.dp)
            .clipToBounds()
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth()
    ) {
        CanvasButton(
            icon = Icons.Default.CallEnd,
            iconColor = Color.Red,
            backgroundColor = Color(0xFF005244),
            modifier = Modifier
                .clip(CircleShape)
                .scale(.95F)
                .clickable(isStartAnimation) {
                    activity?.finish()
                }
        )
        Box(
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .offset(y = (-70).dp)
                    .scale(.8F),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = offsetModifier
                ) {
                    val gradient = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White, Color.Transparent),
                        startY = 0f,
                        endY = size.height,
                    )
                    drawRect(gradient)
                }

                Icon(
                    painterResource(id = R.drawable.ic_arrows_overlay),
                    "Arrow",
                    tint = Color(0xFF008069),
                )
            }
            MiddleButton(navController, isStartAnimation)
        }
        CanvasButton(
            icon = ImageVector.vectorResource(id = R.drawable.ic_reply),
            iconSize = 24F,
            iconColor = Color.White,
            backgroundColor = Color(0xFF005244),
            modifier = Modifier
                .clip(CircleShape)
                .scale(.95F)
        )
    }
}

@Composable
fun MiddleButton(
    navController: NavController? = null,
    isStartAnimation: Boolean,
) {
    var isAnimated by remember { mutableStateOf(isStartAnimation) }
    var offsetY by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition()
    val shakingEasing = Easing { x ->
        ShakingInterpolator(50f).getInterpolation(x)
    }

    val moveUp by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (-30f),
        animationSpec = infiniteRepeatable(
            animation = tween(
                500,
                800,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse,
        )
    )

    val shake by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                500,
                800,
                easing = shakingEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .draggable(
                enabled = isStartAnimation,
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    val coercedOffset = offsetY + delta
                    offsetY = coercedOffset.coerceIn(-350f, 0f)
                },
                onDragStarted = { isAnimated = false },
                onDragStopped = {
                    if (offsetY == -350f) {
                        navController?.navigate(CallScreenActivity.ONGOING_CALL_ROUTE) {
                            popUpTo(CallScreenActivity.INCOMING_CALL_ROUTE) { inclusive = true }
                        }
                    }
                    isAnimated = true
                    offsetY = 0F
                })
    ) {
        CanvasButton(
            icon = Icons.Filled.Call,
            iconColor = Color.White,
            backgroundColor = Color(0xFF02D65D),
            modifier = Modifier.scale(.95F).then(
                if (isAnimated) Modifier.offset(
                    y = moveUp.dp,
                    x = shake.dp
                ) else Modifier.offset { IntOffset(0, offsetY.roundToInt()) }
            )
        )
    }
}