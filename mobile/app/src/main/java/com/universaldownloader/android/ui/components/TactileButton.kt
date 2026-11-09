package com.universaldownloader.android.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universaldownloader.android.ui.theme.FunimatedBorder
import com.universaldownloader.android.ui.theme.FunimatedYellow

@Composable
fun TactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = FunimatedYellow,
    contentColor: Color = FunimatedBorder,
    cornerRadius: Dp = 16.dp,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetY by animateDpAsState(
        targetValue = if (isPressed && enabled) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "press_offset"
    )

    val shadowHeight = 4.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier.padding(bottom = shadowHeight)) {
        // Shadow base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowHeight)
                .background(FunimatedBorder, shape = shape)
        )

        // Button Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offsetY)
                .background(if (enabled) backgroundColor else Color.LightGray, shape = shape)
                .border(2.dp, FunimatedBorder, shape = shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) contentColor else Color.DarkGray,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
