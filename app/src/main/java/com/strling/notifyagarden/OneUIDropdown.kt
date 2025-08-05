package com.strling.notifyagarden

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

@Composable
fun OneUIDropdownItem(onClick: () -> Unit, icon: Int, item: String)
{
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(110.dp).padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(10.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = item,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                item,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OneUIDropdown(onDismiss: () -> Unit, currentSelected: MutableState<Categories>, isEventEmpty: Boolean)
{
    val animationSpeed = 100
    val isVisible = remember { mutableStateOf(false) }
    val startAnimation = remember { mutableStateOf(false) }

    val animate: Int by animateIntAsState(if(startAnimation.value) -150 else 0, label="Animation", animationSpec = tween(animationSpeed))
    val alphaAnimated = (animate / -150.0f)
    val alphaBackground = alphaAnimated * 0.5f
    val onDismissExt: () -> Unit = {
        isVisible.value = true
    }

    LaunchedEffect(isVisible.value) {
        if(isVisible.value) {
            startAnimation.value = false
            delay(animationSpeed.toLong())
            onDismiss()
            isVisible.value = false
            return@LaunchedEffect
        }
        startAnimation.value = true
    }

    Popup() {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = alphaBackground)))
    }

    Popup(
        onDismissRequest = onDismissExt,
        properties = PopupProperties(clippingEnabled = false),
        alignment = Alignment.Center,
        offset = IntOffset(0, animate)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .graphicsLayer { alpha = alphaAnimated }
                .height(125.dp)
                .padding(10.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            OneUIDropdownItem(
                onClick = {
                    currentSelected.value = Categories.WEATHER
                    onDismissExt()
                },
                icon = R.drawable.weather,
                item = "Weather"
            )
            if (!isEventEmpty) {
                OneUIDropdownItem(
                    onClick = {
                        currentSelected.value = Categories.EVENTS
                        onDismissExt()
                    },
                    icon = R.drawable.event,
                    item = "Event Shop"
                )
            }
        }
    }
}