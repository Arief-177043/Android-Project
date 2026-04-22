package com.growwx.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.growwx.data.local.UserPreferences
import com.growwx.data.model.*
import com.growwx.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── State ────────────────────────────────────────────────────────────────────

data class DashboardUiState(
    val userName: String = "",
    val summary: PortfolioSummary = PortfolioSummary(),
    val topGainers: List<Stock> = emptyList(),
    val chartData: List<PricePoint> = emptyList(),
    val insights: List<Insight> = emptyList(),
    val isLoading: Boolean = true
)

data class Insight(val icon: String, val message: String, val colorHex: String)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stockRepo: StockRepository,
    private val portfolioRepo: PortfolioRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val allSymbols = (StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO).map { it.symbol }

    init {
        loadDashboard()
        observeLivePrices()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            // Collect user name
            prefs.userFlow.collect { (_, name, _) ->
                _state.update { it.copy(userName = name) }
            }
        }

        viewModelScope.launch {
            // Generate mock portfolio chart
            val chartData = stockRepo.generateChartData("PORTFOLIO", 30)
            _state.update { it.copy(chartData = chartData, isLoading = false) }
        }

        // Combine holdings + cash into portfolio summary
        viewModelScope.launch {
            combine(
                portfolioRepo.holdings,
                portfolioRepo.cashBalance
            ) { holdings, cash -> Pair(holdings, cash) }
                .collect { (holdings, cash) ->
                    val livePrices = StockRepository.DUMMY_STOCKS.associateBy { it.symbol } +
                            StockRepository.DUMMY_CRYPTO.associateBy { it.symbol }
                    val summary = portfolioRepo.computeSummary(holdings, livePrices.mapValues { it.value }, cash)
                    val insights = generateInsights(summary)
                    _state.update { it.copy(summary = summary, insights = insights) }
                }
        }
    }

    private fun observeLivePrices() {
        viewModelScope.launch {
            stockRepo.observeQuotes(allSymbols.take(12)).collect { prices ->
                val gainers = prices.values.sortedByDescending { it.changePct }.take(5)
                _state.update { it.copy(topGainers = gainers) }
            }
        }
    }

    private fun generateInsights(summary: PortfolioSummary): List<Insight> {
        val list = mutableListOf<Insight>()
        val pnl = summary.pnl
        list += if (pnl >= 0)
            Insight("🧠", "Your portfolio is up ₹${String.format("%,.0f", pnl)} overall. Consistent investing pays off!", "#3B82F6")
        else
            Insight("📉", "Portfolio down ₹${String.format("%,.0f", -pnl)}. Consider diversifying across sectors.", "#F59E0B")

        if (summary.cashBalance > 10_000) {
            list += Insight("💡", "You have ₹${String.format("%,.0f", summary.cashBalance)} idle. Put it to work!", "#8B5CF6")
        }

        if (summary.holdings.isNotEmpty()) {
            val best = summary.holdings.maxByOrNull { it.pnlPercent }
            if (best != null && best.pnlPercent > 1) {
                list += Insight("🔥", "${best.holding.symbol} is your best performer at +${String.format("%.1f", best.pnlPercent)}% return.", "#00C896")
            }
        }

        list += Insight("🎯", "You hold ${summary.holdings.size} positions. Aim for 8–12 for good diversification.", "#00C896")
        return list
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.growwx.ui.components.*
import com.growwx.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToSimulator: () -> Unit,
    onNavigateToWatchlist: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val summary = state.summary
    val pnlColor = if (summary.pnl >= 0) GrowwXColor.Green else GrowwXColor.Red

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── Header ──
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(GrowwXColor.Green.copy(alpha = 0.08f), Color.Transparent)))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Good morning,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.extendedColors.textMuted)
                        Text(state.userName.ifEmpty { "Investor" } + " 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Surface(shape = RoundedCornerShape(50), color = GrowwXColor.Green, modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(state.userName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── Portfolio Value Card ──
        item {
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TOTAL PORTFOLIO VALUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted)
                    Spacer(Modifier.height(4.dp))

                    if (state.isLoading) {
                        SkeletonBox(Modifier.fillMaxWidth(0.6f).height(38.dp), RoundedCornerShape(10.dp))
                    } else {
                        PriceText(summary.totalValue, style = MaterialTheme.typography.displayLarge)
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChangeBadge(changePct = summary.pnlPercent)
                        Text("All time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted)
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = MaterialTheme.extendedColors.border)
                    Spacer(Modifier.height(14.dp))

                    Row {
                        listOf(
                            Triple("Invested", "₹${String.format("%,.0f", summary.totalInvested)}", MaterialTheme.colorScheme.onSurface),
                            Triple("Cash", "₹${String.format("%,.0f", summary.cashBalance)}", GrowwXColor.Blue),
                            Triple("P&L", "${if (summary.pnl >= 0) "+" else ""}₹${String.format("%,.0f", summary.pnl)}", pnlColor),
                        ).forEachIndexed { i, (label, value, color) ->
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = if (i == 2) Alignment.End else if (i == 1) Alignment.CenterHorizontally else Alignment.Start) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted)
                                Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ── Quick Actions ──
        item {
            Row(modifier = Modifier.padding(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onNavigateToSimulator,
                    shape = RoundedCornerShape(16.dp),
                    color = GrowwXColor.GreenLight,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💹", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Simulate", style = MaterialTheme.typography.labelLarge, color = GrowwXColor.Green)
                    }
                }
                Surface(
                    onClick = onNavigateToWatchlist,
                    shape = RoundedCornerShape(16.dp),
                    color = GrowwXColor.BlueLight,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Watchlist", style = MaterialTheme.typography.labelLarge, color = GrowwXColor.Blue)
                    }
                }
                Surface(
                    onClick = { },
                    shape = RoundedCornerShape(16.dp),
                    color = GrowwXColor.AmberLight,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔔", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Alerts", style = MaterialTheme.typography.labelLarge, color = GrowwXColor.Amber)
                    }
                }
            }
        }

        // ── AI Insights ──
        item { SectionHeader("🧠 AI Insights") }
        items(state.insights) { insight ->
            InsightCard(
                icon = insight.icon,
                message = insight.message,
                accentColor = Color(android.graphics.Color.parseColor(insight.colorHex)),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // ── Top Gainers ──
        item { SectionHeader("🔥 Top Gainers Today", "See All") {} }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.topGainers) { stock ->
                    Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(modifier = Modifier.padding(14.dp).width(120.dp)) {
                            Text(stock.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Text(stock.sector, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted)
                            Spacer(Modifier.height(8.dp))
                            SparklineChart(
                                prices = (1..10).map { stock.price * (0.98 + Math.random() * 0.04) },
                                color = if (stock.changePct >= 0) GrowwXColor.Green else GrowwXColor.Red,
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            ChangeBadge(changePct = stock.changePct)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
