package com.growwx.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.growwx.ui.theme.GrowwXColor
import com.growwx.ui.theme.extendedColors
import kotlin.math.*

// ─── Skeleton Loader ──────────────────────────────────────────────────────────

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                val brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(animatedProgress * size.width * 2 - size.width, 0f),
                    end = Offset(animatedProgress * size.width * 2, size.height)
                )
                drawRect(brush = brush)
            }
    )
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isLoading: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted)
            Spacer(Modifier.height(4.dp))
            if (isLoading) {
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(20.dp))
            } else {
                Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Change Badge ─────────────────────────────────────────────────────────────

@Composable
fun ChangeBadge(changePct: Double, modifier: Modifier = Modifier) {
    val isPositive = changePct >= 0
    val color = if (isPositive) GrowwXColor.Green else GrowwXColor.Red
    val bgColor = if (isPositive) MaterialTheme.extendedColors.greenLight else MaterialTheme.extendedColors.redLight
    val arrow = if (isPositive) "▲" else "▼"

    Surface(shape = RoundedCornerShape(20.dp), color = bgColor, modifier = modifier) {
        Text(
            text = "$arrow ${String.format("%.2f", abs(changePct))}%",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ─── Price Text ───────────────────────────────────────────────────────────────

@Composable
fun PriceText(price: Double, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge) {
    val formatted = when {
        price >= 10_000_000 -> "₹${String.format("%.2f", price / 10_000_000)}Cr"
        price >= 100_000 -> "₹${String.format("%.2f", price / 100_000)}L"
        price >= 1_000 -> "₹${String.format("%,.2f", price)}"
        else -> "₹${String.format("%.2f", price)}"
    }
    Text(formatted, style = style, fontWeight = FontWeight.ExtraBold)
}

// ─── Mini Sparkline Chart (Canvas) ───────────────────────────────────────────

@Composable
fun SparklineChart(
    prices: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = GrowwXColor.Green,
    strokeWidth: Float = 3f
) {
    if (prices.size < 2) return

    val minVal = prices.min()
    val maxVal = prices.max()
    val range = (maxVal - minVal).coerceAtLeast(0.001)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val step = w / (prices.size - 1)

        val pts = prices.mapIndexed { i, p ->
            Offset(i * step, h - ((p - minVal) / range * h).toFloat())
        }

        // Fill gradient
        val fillPath = Path().apply {
            moveTo(pts.first().x, h)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, h)
            close()
        }
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))

        // Line
        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val cx = (pts[i - 1].x + pts[i].x) / 2
                cubicTo(cx, pts[i - 1].y, cx, pts[i].y, pts[i].x, pts[i].y)
            }
        }
        drawPath(linePath, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // End dot
        drawCircle(color, radius = strokeWidth * 1.5f, center = pts.last())
    }
}

// ─── Stock List Item ──────────────────────────────────────────────────────────

@Composable
fun StockListItem(
    symbol: String,
    name: String,
    price: Double,
    changePct: Double,
    sparklineData: List<Double> = emptyList(),
    onClick: () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    val isPositive = changePct >= 0
    val accentColor = if (isPositive) GrowwXColor.Green else GrowwXColor.Red

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol Avatar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPositive) MaterialTheme.extendedColors.greenLight else MaterialTheme.extendedColors.redLight,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = symbol.take(2),
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Name + symbol
            Column(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    SkeletonBox(Modifier.fillMaxWidth(0.5f).height(14.dp))
                    Spacer(Modifier.height(4.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.7f).height(11.dp))
                } else {
                    Text(symbol, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Sparkline
            if (sparklineData.size >= 2) {
                SparklineChart(
                    prices = sparklineData,
                    color = accentColor,
                    modifier = Modifier.size(width = 64.dp, height = 32.dp)
                )
                Spacer(Modifier.width(10.dp))
            }

            // Price + change
            Column(horizontalAlignment = Alignment.End) {
                if (isLoading) {
                    SkeletonBox(Modifier.width(70.dp).height(14.dp))
                    Spacer(Modifier.height(4.dp))
                    SkeletonBox(Modifier.width(50.dp).height(11.dp))
                } else {
                    PriceText(price, style = MaterialTheme.typography.titleMedium)
                    ChangeBadge(changePct = changePct)
                }
            }

            trailingContent?.let {
                Spacer(Modifier.width(8.dp))
                it()
            }
        }
    }
}

// ─── Primary Action Button ────────────────────────────────────────────────────

@Composable
fun GrowwXButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = GrowwXColor.Green
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

// ─── Input Field ──────────────────────────────────────────────────────────────

@Composable
fun GrowwXTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String = "",
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.extendedColors.textMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.extendedColors.textMuted) },
            isError = isError,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrowwXColor.Green,
                unfocusedBorderColor = MaterialTheme.extendedColors.border,
                errorBorderColor = GrowwXColor.Red,
                focusedContainerColor = MaterialTheme.extendedColors.inputBg,
                unfocusedContainerColor = MaterialTheme.extendedColors.inputBg,
            )
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = GrowwXColor.Red, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, actionLabel: String = "", onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (actionLabel.isNotEmpty()) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = GrowwXColor.Green, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(icon: String, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.extendedColors.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ─── Insight Card ─────────────────────────────────────────────────────────────

@Composable
fun InsightCard(
    icon: String,
    message: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .drawBehind {
                    drawRect(accentColor, size = androidx.compose.ui.geometry.Size(6f, size.height))
                }
                .padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 18.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        }
    }
}
