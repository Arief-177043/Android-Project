package com.growwx.ui.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.growwx.data.model.*
import com.growwx.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── State ────────────────────────────────────────────────────────────────────

data class SimulatorUiState(
    val allAssets: List<Stock> = emptyList(),
    val holdings: List<HoldingEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val cashBalance: Double = 100_000.0,
    val livePrices: Map<String, Stock> = emptyMap(),
    val searchQuery: String = "",
    val tradeAsset: Stock? = null,
    val tradeType: TradeType = TradeType.BUY,
    val tradeQty: String = "",
    val tradeError: String? = null,
    val tradeSuccess: String? = null,
    val isLoading: Boolean = false,
    val showTradeSheet: Boolean = false,
    val showHistory: Boolean = false
)

enum class TradeType { BUY, SELL }

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class SimulatorViewModel @Inject constructor(
    private val stockRepo: StockRepository,
    private val portfolioRepo: PortfolioRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SimulatorUiState())
    val state: StateFlow<SimulatorUiState> = _state.asStateFlow()

    // Derived: filtered asset list based on search
    val filteredAssets: StateFlow<List<Stock>> = combine(
        _state.map { it.allAssets },
        _state.map { it.searchQuery }
    ) { assets, query ->
        if (query.isBlank()) assets
        else assets.filter {
            it.symbol.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load all assets
        _state.update { it.copy(allAssets = StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO) }

        // Observe portfolio
        viewModelScope.launch {
            portfolioRepo.holdings.collect { holdings ->
                _state.update { it.copy(holdings = holdings) }
            }
        }
        viewModelScope.launch {
            portfolioRepo.transactions.collect { txns ->
                _state.update { it.copy(transactions = txns) }
            }
        }
        viewModelScope.launch {
            portfolioRepo.cashBalance.collect { cash ->
                _state.update { it.copy(cashBalance = cash) }
            }
        }

        // Live price polling
        val symbols = (StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO).map { it.symbol }
        viewModelScope.launch {
            stockRepo.observeQuotes(symbols).collect { prices ->
                _state.update { it.copy(livePrices = prices) }
            }
        }
    }

    fun setSearch(query: String) = _state.update { it.copy(searchQuery = query) }

    fun openBuySheet(stock: Stock) {
        val live = _state.value.livePrices[stock.symbol] ?: stock
        _state.update { it.copy(tradeAsset = live, tradeType = TradeType.BUY, tradeQty = "", tradeError = null, showTradeSheet = true) }
    }

    fun openSellSheet(stock: Stock) {
        val live = _state.value.livePrices[stock.symbol] ?: stock
        _state.update { it.copy(tradeAsset = live, tradeType = TradeType.SELL, tradeQty = "", tradeError = null, showTradeSheet = true) }
    }

    fun setTradeQty(qty: String) = _state.update { it.copy(tradeQty = qty, tradeError = null) }

    fun setMaxQty() {
        val s = _state.value
        val asset = s.tradeAsset ?: return
        val livePrice = s.livePrices[asset.symbol]?.price ?: asset.price
        val maxQty = if (s.tradeType == TradeType.BUY) {
            (s.cashBalance / livePrice).toLong()
        } else {
            s.holdings.find { it.symbol == asset.symbol }?.qty?.toLong() ?: 0L
        }
        _state.update { it.copy(tradeQty = maxQty.toString()) }
    }

    fun executeTrade() {
        val s = _state.value
        val asset = s.tradeAsset ?: return
        val qty = s.tradeQty.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _state.update { it.copy(tradeError = "Enter a valid quantity") }
            return
        }

        val livePrice = s.livePrices[asset.symbol]?.price ?: asset.price
        _state.update { it.copy(isLoading = true, tradeError = null) }

        viewModelScope.launch {
            val result = if (s.tradeType == TradeType.BUY) {
                portfolioRepo.buy(asset.symbol, asset.name, qty, livePrice, asset.isCrypto)
            } else {
                portfolioRepo.sell(asset.symbol, asset.name, qty, livePrice)
            }

            when (result) {
                is Result.Success -> _state.update {
                    it.copy(isLoading = false, showTradeSheet = false, tradeSuccess = "${s.tradeType.name} executed!")
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, tradeError = result.message) }
                else -> {}
            }
        }
    }

    fun dismissTradeSheet() = _state.update { it.copy(showTradeSheet = false, tradeError = null) }
    fun toggleHistory() = _state.update { it.copy(showHistory = !it.showHistory) }
    fun clearSuccess() = _state.update { it.copy(tradeSuccess = null) }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.growwx.ui.components.*
import com.growwx.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(viewModel: SimulatorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val filtered by viewModel.filteredAssets.collectAsState()
    val tradePnl = remember(state.tradeQty, state.tradeAsset, state.livePrices) {
        val qty = state.tradeQty.toDoubleOrNull() ?: 0.0
        val price = state.tradeAsset?.let { state.livePrices[it.symbol]?.price ?: it.price } ?: 0.0
        qty * price
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.tradeSuccess) {
        state.tradeSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Header ──
            item {
                Row(
                    modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulator", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Virtual balance:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted)
                            Text("₹${String.format("%,.2f", state.cashBalance)}", style = MaterialTheme.typography.bodySmall, color = GrowwXColor.Green, fontWeight = FontWeight.Bold)
                        }
                    }
                    FilledTonalButton(onClick = { viewModel.toggleHistory() }) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("History")
                    }
                }
            }

            // ── Holdings ──
            if (state.holdings.isNotEmpty()) {
                item { SectionHeader("MY HOLDINGS") }
                items(state.holdings) { holding ->
                    val livePrice = state.livePrices[holding.symbol]?.price ?: holding.avgBuyPrice
                    val pnl = (livePrice - holding.avgBuyPrice) * holding.qty
                    val pnlPct = ((livePrice - holding.avgBuyPrice) / holding.avgBuyPrice) * 100

                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(holding.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                        Surface(shape = RoundedCornerShape(20.dp), color = GrowwXColor.BlueLight) {
                                            Text("${holding.qty.toInt()} shares", style = MaterialTheme.typography.labelSmall, color = GrowwXColor.Blue, modifier = Modifier.padding(6.dp, 2.dp))
                                        }
                                    }
                                    Text("Avg ₹${String.format("%,.2f", holding.avgBuyPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("₹${String.format("%,.2f", livePrice * holding.qty)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${if (pnl >= 0) "+" else ""}₹${String.format("%,.0f", pnl)} (${String.format("%.1f", pnlPct)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (pnl >= 0) GrowwXColor.Green else GrowwXColor.Red,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val asset = state.allAssets.find { it.symbol == holding.symbol }
                                if (asset != null) {
                                    Button(
                                        onClick = { viewModel.openBuySheet(asset) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GrowwXColor.Green),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Buy More") }
                                    Button(
                                        onClick = { viewModel.openSellSheet(asset) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GrowwXColor.Red),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Sell") }
                                }
                            }
                        }
                    }
                }
            }

            // ── Search ──
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearch(it) },
                    placeholder = { Text("Search stocks or crypto...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.setSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrowwXColor.Green,
                        unfocusedBorderColor = MaterialTheme.extendedColors.border,
                        focusedContainerColor = MaterialTheme.extendedColors.inputBg,
                        unfocusedContainerColor = MaterialTheme.extendedColors.inputBg,
                    ),
                    singleLine = true
                )
            }

            item { SectionHeader("BROWSE ASSETS") }

            // ── Asset List ──
            items(filtered) { stock ->
                val live = state.livePrices[stock.symbol] ?: stock
                StockListItem(
                    symbol = live.symbol,
                    name = live.name,
                    price = live.price,
                    changePct = live.changePct,
                    onClick = { viewModel.openBuySheet(live) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { viewModel.openBuySheet(live) },
                                colors = ButtonDefaults.buttonColors(containerColor = GrowwXColor.Green),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(8.dp, 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) { Text("Buy", style = MaterialTheme.typography.labelSmall) }
                            if (state.holdings.any { it.symbol == stock.symbol }) {
                                Button(
                                    onClick = { viewModel.openSellSheet(live) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GrowwXColor.Red),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(8.dp, 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) { Text("Sell", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                )
            }
        }

        // ── Trade Bottom Sheet ──
        if (state.showTradeSheet && state.tradeAsset != null) {
            val asset = state.tradeAsset!!
            val livePrice = state.livePrices[asset.symbol]?.price ?: asset.price
            val isBuy = state.tradeType == TradeType.BUY
            val accentColor = if (isBuy) GrowwXColor.Green else GrowwXColor.Red

            ModalBottomSheet(onDismissRequest = { viewModel.dismissTradeSheet() }) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp, 0.dp, 24.dp, 40.dp)) {
                    Text("${if (isBuy) "Buy" else "Sell"} ${asset.symbol}",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(20.dp))

                    // Price info
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.extendedColors.inputBg, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted)
                                PriceText(livePrice, style = MaterialTheme.typography.headlineLarge)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (isBuy) "Available Cash" else "Your Holdings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted)
                                Text(
                                    if (isBuy) "₹${String.format("%,.2f", state.cashBalance)}"
                                    else "${state.holdings.find { it.symbol == asset.symbol }?.qty?.toInt() ?: 0} shares",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = accentColor, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Qty input
                    Text("QUANTITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.textMuted)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.tradeQty,
                        onValueChange = { viewModel.setTradeQty(it) },
                        placeholder = { Text("Enter number of shares") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        isError = state.tradeError != null,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor),
                        singleLine = true
                    )

                    // Quick qty buttons
                    if (isBuy) {
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1", "5", "10", "Max").forEach { q ->
                                OutlinedButton(
                                    onClick = { if (q == "Max") viewModel.setMaxQty() else viewModel.setTradeQty(q) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.extendedColors.border),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text(q, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }

                    // Error
                    state.tradeError?.let {
                        Surface(shape = RoundedCornerShape(10.dp), color = GrowwXColor.RedLight, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = GrowwXColor.Red, modifier = Modifier.padding(10.dp))
                        }
                    }

                    // Total value display
                    if (tradePnl > 0) {
                        Spacer(Modifier.height(14.dp))
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.extendedColors.inputBg, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Value", style = MaterialTheme.typography.bodyMedium)
                                Text("₹${String.format("%,.2f", tradePnl)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    GrowwXButton(
                        text = "${if (isBuy) "Buy" else "Sell"} ${state.tradeQty.ifEmpty { "–" }} ${asset.symbol}",
                        onClick = { viewModel.executeTrade() },
                        isLoading = state.isLoading,
                        containerColor = accentColor
                    )
                }
            }
        }

        // ── Transaction History Sheet ──
        if (state.showHistory) {
            ModalBottomSheet(onDismissRequest = { viewModel.toggleHistory() }) {
                val sdf = SimpleDateFormat("dd MMM · hh:mm a", Locale.getDefault())
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp, 0.dp, 24.dp, 40.dp)) {
                    Text("Transaction History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(16.dp))
                    if (state.transactions.isEmpty()) {
                        EmptyState("📭", "No transactions yet", "Start simulating trades to see history here.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                            items(state.transactions) { tx ->
                                val isBuy = tx.type == "BUY"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = if (isBuy) GrowwXColor.GreenLight else GrowwXColor.RedLight, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(if (isBuy) "↑" else "↓", fontSize = 18.sp, color = if (isBuy) GrowwXColor.Green else GrowwXColor.Red)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${tx.type} ${tx.symbol}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${tx.qty.toInt()} @ ₹${String.format("%,.2f", tx.price)} · ${sdf.format(Date(tx.timestamp))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted)
                                    }
                                    Text(
                                        "${if (isBuy) "-" else "+"}₹${String.format("%,.0f", tx.total)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isBuy) GrowwXColor.Red else GrowwXColor.Green,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Divider(color = MaterialTheme.extendedColors.border)
                            }
                        }
                    }
                }
            }
        }
    }
}
