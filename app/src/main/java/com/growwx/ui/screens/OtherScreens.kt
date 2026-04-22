// ══════════════════════════════════════════════════════════════════════════════
// WATCHLIST
// ══════════════════════════════════════════════════════════════════════════════
package com.growwx.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.growwx.data.local.WatchlistDao
import com.growwx.data.model.*
import com.growwx.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val dao: WatchlistDao,
    private val stockRepo: StockRepository
) : ViewModel() {

    private val _livePrices = MutableStateFlow<Map<String, Stock>>(emptyMap())
    private val _search = MutableStateFlow("")

    val watchlist: StateFlow<List<WatchlistEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val livePrices = _livePrices.asStateFlow()
    val search = _search.asStateFlow()

    val searchResults: StateFlow<List<Stock>> = _search
        .debounce(300)
        .map { query ->
            if (query.isBlank()) emptyList()
            else (StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO).filter {
                it.symbol.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val allSymbols = (StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO).map { it.symbol }
        viewModelScope.launch {
            stockRepo.observeQuotes(allSymbols).collect { _livePrices.value = it }
        }
    }

    fun setSearch(q: String) { _search.value = q }

    fun addToWatchlist(stock: Stock) {
        viewModelScope.launch {
            dao.insert(WatchlistEntity(stock.symbol, stock.name, stock.isCrypto))
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch { dao.remove(symbol) }
    }

    fun isWatched(symbol: String): Flow<Boolean> = dao.isWatched(symbol)
}

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.growwx.ui.components.*
import com.growwx.ui.theme.*

@Composable
fun WatchlistScreen(
    onStockClick: (String) -> Unit = {},
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val watchlist by viewModel.watchlist.collectAsState()
    val prices by viewModel.livePrices.collectAsState()
    val search by viewModel.search.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Row(modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Watchlist", style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { showSearch = !showSearch }) {
                    Icon(if (showSearch) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showSearch) "Cancel" else "Add")
                }
            }
        }

        if (showSearch) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { viewModel.setSearch(it) },
                    placeholder = { Text("Search stocks or crypto...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrowwXColor.Green, unfocusedContainerColor = MaterialTheme.extendedColors.inputBg, focusedContainerColor = MaterialTheme.extendedColors.inputBg)
                )
            }
            items(results) { stock ->
                val isWatched by viewModel.isWatched(stock.symbol).collectAsState(false)
                StockListItem(
                    symbol = stock.symbol, name = stock.name,
                    price = prices[stock.symbol]?.price ?: stock.price,
                    changePct = prices[stock.symbol]?.changePct ?: stock.changePct,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    trailingContent = {
                        IconButton(onClick = {
                            if (isWatched) viewModel.removeFromWatchlist(stock.symbol)
                            else viewModel.addToWatchlist(stock)
                        }) {
                            Icon(if (isWatched) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = if (isWatched) GrowwXColor.Red else GrowwXColor.Green)
                        }
                    }
                )
            }
        }

        if (watchlist.isEmpty() && !showSearch) {
            item {
                EmptyState("📋", "Your Watchlist is Empty", "Tap + Add to start tracking your favourite stocks and crypto.", modifier = Modifier.padding(top = 60.dp))
            }
        } else {
            items(watchlist) { w ->
                val live = prices[w.symbol]
                val sparkline = remember(w.symbol) { (1..12).map { StockRepository.DUMMY_STOCKS.find { s -> s.symbol == w.symbol }?.price?.times(0.97 + Math.random() * 0.06) ?: 1000.0 } }
                StockListItem(
                    symbol = w.symbol, name = w.name,
                    price = live?.price ?: 0.0,
                    changePct = live?.changePct ?: 0.0,
                    sparklineData = sparkline,
                    onClick = { onStockClick(w.symbol) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeFromWatchlist(w.symbol) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = GrowwXColor.Red)
                        }
                    }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ALERTS
// ══════════════════════════════════════════════════════════════════════════════
package com.growwx.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.growwx.data.local.PriceAlertDao
import com.growwx.data.model.*
import com.growwx.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val dao: PriceAlertDao,
    private val stockRepo: StockRepository
) : ViewModel() {

    val alerts: StateFlow<List<PriceAlertEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _livePrices = MutableStateFlow<Map<String, Stock>>(emptyMap())
    val livePrices = _livePrices.asStateFlow()

    init {
        val symbols = (StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO).map { it.symbol }
        viewModelScope.launch {
            stockRepo.observeQuotes(symbols).collect { prices ->
                _livePrices.value = prices
                checkAlerts(prices)
            }
        }
    }

    fun addAlert(symbol: String, name: String, targetPrice: Double, condition: String) {
        viewModelScope.launch {
            dao.insert(PriceAlertEntity(symbol = symbol, name = name, targetPrice = targetPrice, condition = condition))
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }

    private suspend fun checkAlerts(prices: Map<String, Stock>) {
        val active = dao.getActiveAlerts()
        active.forEach { alert ->
            val currentPrice = prices[alert.symbol]?.price ?: return@forEach
            val triggered = when (alert.condition) {
                "ABOVE" -> currentPrice >= alert.targetPrice
                "BELOW" -> currentPrice <= alert.targetPrice
                else -> false
            }
            if (triggered) dao.markTriggered(alert.id)
        }
    }
}

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.growwx.ui.components.*
import com.growwx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: AlertsViewModel = hiltViewModel()) {
    val alerts by viewModel.alerts.collectAsState()
    val prices by viewModel.livePrices.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var selectedSymbol by remember { mutableStateOf("") }
    var targetPrice by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("ABOVE") }
    val allAssets = StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Row(modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Price Alerts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = GrowwXColor.Green), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Alert")
                }
            }
        }

        if (alerts.isEmpty()) {
            item { EmptyState("🔔", "No Alerts Set", "Create price alerts to get notified when stocks hit your target price.", modifier = Modifier.padding(top = 60.dp)) }
        } else {
            items(alerts) { alert ->
                val currentPrice = prices[alert.symbol]?.price
                val isNear = currentPrice != null && kotlin.math.abs(currentPrice - alert.targetPrice) / alert.targetPrice < 0.02

                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(alert.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                    if (alert.isTriggered) {
                                        Surface(shape = RoundedCornerShape(20.dp), color = GrowwXColor.GreenLight) {
                                            Text("✓ TRIGGERED", style = MaterialTheme.typography.labelSmall, color = GrowwXColor.Green, modifier = Modifier.padding(6.dp, 2.dp))
                                        }
                                    }
                                    if (isNear && !alert.isTriggered) {
                                        Surface(shape = RoundedCornerShape(20.dp), color = GrowwXColor.AmberLight) {
                                            Text("⚡ NEAR TARGET", style = MaterialTheme.typography.labelSmall, color = GrowwXColor.Amber, modifier = Modifier.padding(6.dp, 2.dp))
                                        }
                                    }
                                }
                                Text(
                                    "Alert when ${if (alert.condition == "ABOVE") "above" else "below"} ₹${String.format("%,.2f", alert.targetPrice)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted
                                )
                                currentPrice?.let {
                                    Text("Current: ₹${String.format("%,.2f", it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteAlert(alert.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GrowwXColor.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp, 0.dp, 24.dp, 40.dp)) {
                Text("New Price Alert", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(20.dp))

                Text("ASSET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted)
                Spacer(Modifier.height(6.dp))
                // Simplified dropdown using ExposedDropdownMenuBox
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedSymbol,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Select asset") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        allAssets.forEach { a ->
                            DropdownMenuItem(text = { Text("${a.symbol} – ${a.name}") }, onClick = { selectedSymbol = a.symbol; expanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("CONDITION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("ABOVE" to "▲ Above", "BELOW" to "▼ Below").forEach { (c, label) ->
                        val isSelected = condition == c
                        val color = if (c == "ABOVE") GrowwXColor.Green else GrowwXColor.Red
                        Surface(
                            onClick = { condition = c },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) (if (c == "ABOVE") MaterialTheme.extendedColors.greenLight else MaterialTheme.extendedColors.redLight) else MaterialTheme.extendedColors.inputBg,
                            border = if (isSelected) BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(14.dp)) {
                                Text(label, style = MaterialTheme.typography.labelLarge, color = if (isSelected) color else MaterialTheme.extendedColors.textMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                GrowwXTextField(value = targetPrice, onValueChange = { targetPrice = it }, label = "Target Price (₹)", placeholder = "e.g. 3000", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(24.dp))
                GrowwXButton(text = "🔔  Set Alert", onClick = {
                    val asset = allAssets.find { it.symbol == selectedSymbol }
                    val price = targetPrice.toDoubleOrNull()
                    if (asset != null && price != null && price > 0) {
                        viewModel.addAlert(asset.symbol, asset.name, price, condition)
                        showAdd = false; targetPrice = ""; selectedSymbol = ""
                    }
                })
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PORTFOLIO
// ══════════════════════════════════════════════════════════════════════════════
package com.growwx.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.growwx.data.model.*
import com.growwx.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val portfolioRepo: PortfolioRepository,
    private val stockRepo: StockRepository
) : ViewModel() {

    private val _prices = MutableStateFlow<Map<String, Stock>>(emptyMap())

    val summary: StateFlow<PortfolioSummary> = combine(
        portfolioRepo.holdings,
        portfolioRepo.cashBalance,
        _prices
    ) { holdings, cash, prices ->
        portfolioRepo.computeSummary(holdings, prices, cash)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioSummary())

    init {
        val symbols = (StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO).map { it.symbol }
        viewModelScope.launch {
            stockRepo.observeQuotes(symbols).collect { _prices.value = it }
        }
    }
}

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.growwx.ui.components.*
import com.growwx.ui.theme.*
import kotlin.math.*

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = hiltViewModel()) {
    val summary by viewModel.summary.collectAsState()
    val pnlPositive = summary.pnl >= 0
    val COLORS = listOf(GrowwXColor.Green, GrowwXColor.Blue, GrowwXColor.Amber, GrowwXColor.Purple, GrowwXColor.Red)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Text("Portfolio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp))
        }

        // Summary card
        item {
            Surface(modifier = Modifier.padding(16.dp, 4.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column { Text("Current Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted); PriceText(summary.totalValue, style = MaterialTheme.typography.headlineLarge) }
                        Column(horizontalAlignment = Alignment.End) { Text("Total P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted); Text("${if (pnlPositive) "+" else ""}₹${String.format("%,.0f", summary.pnl)}", style = MaterialTheme.typography.headlineMedium, color = if (pnlPositive) GrowwXColor.Green else GrowwXColor.Red, fontWeight = FontWeight.ExtraBold) }
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { (0.5f + (summary.pnlPercent / 200).toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (pnlPositive) GrowwXColor.Green else GrowwXColor.Red, trackColor = MaterialTheme.extendedColors.border)
                }
            }
        }

        // Pie chart allocation
        if (summary.holdings.isNotEmpty()) {
            item {
                Surface(modifier = Modifier.padding(16.dp, 8.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Allocation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        val total = summary.holdings.sumOf { it.currentValue }
                        val slices = summary.holdings.mapIndexed { i, h -> Pair(h, h.currentValue / total) }
                        // Pie
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(180.dp)) {
                                var startAngle = -90f
                                slices.forEachIndexed { i, (h, pct) ->
                                    val sweep = (pct * 360).toFloat()
                                    drawArc(color = COLORS[i % COLORS.size], startAngle = startAngle, sweepAngle = sweep, useCenter = true, topLeft = Offset(16f, 16f), size = Size(size.width - 32f, size.height - 32f))
                                    startAngle += sweep
                                }
                                drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = size.minDimension / 4)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        slices.forEachIndexed { i, (h, pct) ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp, 10.dp).background(COLORS[i % COLORS.size], RoundedCornerShape(3.dp)))
                                Spacer(Modifier.width(8.dp))
                                Text(h.holding.symbol, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("${String.format("%.1f", pct * 100)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted)
                            }
                        }
                    }
                }
            }

            item { SectionHeader("HOLDINGS") }

            items(summary.holdings) { h ->
                Surface(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(h.holding.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${h.holding.qty.toInt()} shares · Avg ₹${String.format("%,.2f", h.holding.avgBuyPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${String.format("%,.2f", h.currentValue)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${if (h.pnl >= 0) "+" else ""}₹${String.format("%,.0f", h.pnl)} (${String.format("%.1f", h.pnlPercent)}%)", style = MaterialTheme.typography.bodySmall, color = if (h.pnl >= 0) GrowwXColor.Green else GrowwXColor.Red, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            item { EmptyState("📊", "No Holdings Yet", "Head to the Simulator tab to start virtual investing!", modifier = Modifier.padding(top = 40.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PROFILE
// ══════════════════════════════════════════════════════════════════════════════
package com.growwx.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.growwx.data.local.UserPreferences
import com.growwx.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val prefs: UserPreferences) : ViewModel() {
    val user: StateFlow<User> = prefs.userFlow.map { (uid, name, email) -> User(uid, name, email) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), User("", "", ""))
    val isDarkMode: StateFlow<Boolean> = prefs.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDarkMode(on: Boolean) { viewModelScope.launch { prefs.setDarkMode(on) } }
    fun logout() { viewModelScope.launch { prefs.logout() } }
}

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.growwx.ui.theme.*

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Profile header
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
                    .background(Brush.linearGradient(listOf(GrowwXColor.Green, GrowwXColor.GreenDark)), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Box(modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Text(user.name.firstOrNull()?.uppercase() ?: "U", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(user.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.2f)) {
                        Text("✓  Verified Investor", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.padding(12.dp, 4.dp))
                    }
                }
            }
        }

        // Settings items
        item {
            Surface(modifier = Modifier.padding(16.dp, 8.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp) {
                Column {
                    // Dark mode toggle
                    ListItem(
                        headlineContent = { Text("Dark Mode") },
                        leadingContent = { Icon(Icons.Default.DarkMode, null, tint = GrowwXColor.Purple) },
                        trailingContent = { Switch(checked = isDark, onCheckedChange = { viewModel.setDarkMode(it) }, colors = SwitchDefaults.colors(checkedThumbColor = GrowwXColor.Green, checkedTrackColor = GrowwXColor.GreenLight)) },
                    )
                    Divider(color = MaterialTheme.extendedColors.border)
                    ListItem(headlineContent = { Text("Notifications") }, leadingContent = { Icon(Icons.Default.Notifications, null, tint = GrowwXColor.Amber) }, trailingContent = { Text("On", color = GrowwXColor.Green, fontWeight = FontWeight.Bold) })
                    Divider(color = MaterialTheme.extendedColors.border)
                    ListItem(headlineContent = { Text("Security & PIN") }, leadingContent = { Icon(Icons.Default.Lock, null, tint = GrowwXColor.Blue) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
                    Divider(color = MaterialTheme.extendedColors.border)
                    ListItem(headlineContent = { Text("Help & Support") }, leadingContent = { Icon(Icons.Default.Help, null, tint = GrowwXColor.Green) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.logout(); onLogout() },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, GrowwXColor.Red),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GrowwXColor.Red)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text("GrowwX v1.0.0 · Built with ❤️ for investors", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }
    }
}
