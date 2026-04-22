package com.growwx.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.growwx.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboarded() }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.growwx.ui.theme.*

data class OnboardSlide(
    val emoji: String,
    val title: String,
    val description: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val slides = listOf(
        OnboardSlide("📈", "Track Your Portfolio", "Monitor all your investments in one place with real-time prices and beautiful performance charts.", GrowwXColor.Green, GrowwXColor.GreenDark),
        OnboardSlide("🎮", "Simulate Without Risk", "Practice investing with ₹1,00,000 virtual money. Learn strategies without losing real capital.", GrowwXColor.Blue, Color(0xFF1D4ED8)),
        OnboardSlide("🧠", "AI-Powered Insights", "Get smart, personalised insights about your portfolio performance and market trends.", GrowwXColor.Purple, Color(0xFF6D28D9)),
        OnboardSlide("🔔", "Smart Price Alerts", "Set price targets and get notified instantly when your stocks hit the right price.", GrowwXColor.Amber, Color(0xFFD97706)),
    )

    var currentPage by remember { mutableStateOf(0) }
    val slide = slides[currentPage]

    // Animate icon scale on page change
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        val bgBrush = Brush.radialGradient(
            colors = listOf(slide.gradientStart.copy(alpha = 0.12f), Color.Transparent),
            radius = 800f
        )
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        Box(modifier = Modifier.fillMaxSize().background(bgBrush))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // ── Emoji Icon ──
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f))
                        .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 1.1f))
                },
                label = "slideIcon"
            ) { page ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .scale(iconScale)
                        .background(
                            Brush.linearGradient(listOf(slides[page].gradientStart.copy(alpha = 0.2f), slides[page].gradientEnd.copy(alpha = 0.1f))),
                            RoundedCornerShape(40.dp)
                        )
                ) {
                    Text(slides[page].emoji, fontSize = 64.sp)
                }
            }

            Spacer(Modifier.height(48.dp))

            // ── Text content ──
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 3 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()).togetherWith(slideOutHorizontally { it / 3 } + fadeOut())
                    }
                },
                label = "slideContent"
            ) { page ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 36.dp)
                ) {
                    Text(
                        slides[page].title,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        slides[page].description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.extendedColors.textMuted,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Progress dots ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                slides.forEachIndexed { i, s ->
                    val isActive = i == currentPage
                    val width by animateDpAsState(if (isActive) 24.dp else 8.dp, tween(300), label = "dot")
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .background(
                                if (isActive) slide.gradientStart else MaterialTheme.extendedColors.border,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            // ── Buttons ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back / Skip
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.extendedColors.border)
                    ) {
                        Text("Back", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.completeOnboarding(); onFinished() },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.extendedColors.textMuted)
                    }
                }

                // Next / Get Started
                Button(
                    onClick = {
                        if (currentPage < slides.size - 1) {
                            currentPage++
                        } else {
                            viewModel.completeOnboarding()
                            onFinished()
                        }
                    },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = slide.gradientStart
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        if (currentPage == slides.size - 1) "Get Started 🚀" else "Next",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}
