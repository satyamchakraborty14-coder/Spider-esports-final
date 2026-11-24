package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.PaymentOrder
import com.example.model.PayoutRequest
import com.example.model.UiState
import com.example.model.Wallet
import com.example.model.WalletConfig
import com.example.model.WalletTransaction
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.components.SpiderBackTopBar
import com.example.ui.components.SpiderEmptyState
import com.example.ui.components.SpiderSectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.SpiderAmber
import com.example.ui.theme.SpiderBlack
import com.example.ui.theme.SpiderCardBorder
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderGold
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletPurpleAccent
import com.example.ui.theme.WalletPurpleEnd
import com.example.ui.theme.WalletPurpleMid
import com.example.ui.theme.WalletPurpleStart
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WalletScreen(
    wallet: Wallet,
    walletConfig: WalletConfig,
    transactionsState: UiState<List<WalletTransaction>>,
    paymentOrders: List<PaymentOrder>,
    payoutRequests: List<PayoutRequest>,
    onSubmitDeposit: (Double, String, (Boolean, String?) -> Unit) -> Unit,
    onSubmitWithdrawal: (Double, String, String, (Boolean, String?) -> Unit) -> Unit,
    onRefreshWallet: () -> Unit = {},
    onBack: () -> Unit
) {
    var showAddCoinsDialog by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    val minWithdraw = walletConfig.minWithdraw.coerceAtLeast(50.0)

    LaunchedEffect(Unit) {
        onRefreshWallet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(
            title = "My Wallet",
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = onRefreshWallet,
                    modifier = Modifier.testTag("refresh_wallet_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh Wallet",
                        tint = TextPrimary
                    )
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Purple Wallet Balance Card with separated Deposit, Winning, Bonus
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, WalletPurpleAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_balance_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(WalletPurpleStart, WalletPurpleMid, WalletPurpleEnd)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TOTAL BALANCE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f Coins", wallet.totalBalance),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = Color.White,
                                        modifier = Modifier.testTag("wallet_total_balance")
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "50 Coins = ₹50  (1 Coin = ₹1)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SpiderGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalanceWallet,
                                        contentDescription = "Wallet",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

                            // 3 Separated Wallet Buckets: Deposit, Winning, Bonus
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WalletBucketCard(
                                    label = "DEPOSIT",
                                    amount = wallet.depositedBalance,
                                    hint = "Entry Only",
                                    accent = Color.White,
                                    modifier = Modifier.weight(1f).testTag("wallet_deposit_bucket")
                                )
                                WalletBucketCard(
                                    label = "WINNING",
                                    amount = wallet.winningBalance,
                                    hint = "Redeemable",
                                    accent = SpiderEmerald,
                                    modifier = Modifier.weight(1f).testTag("wallet_winning_bucket")
                                )
                                WalletBucketCard(
                                    label = "BONUS",
                                    amount = wallet.bonusBalance,
                                    hint = "Rewards",
                                    accent = SpiderGold,
                                    modifier = Modifier.weight(1f).testTag("wallet_bonus_bucket")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Withdrawn: ${wallet.payoutsTotal.toInt()} Coins (₹${wallet.payoutsTotal.toInt()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Min Withdraw: ${minWithdraw.toInt()} Coins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SpiderEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: ADD COINS & REDEEM COINS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showAddCoinsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("add_coins_button")
                    ) {
                        Icon(Icons.Filled.AddCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ADD COINS", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = { showRedeemDialog = true },
                        border = BorderStroke(1.5.dp, WalletPurpleAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("redeem_coins_button")
                    ) {
                        Icon(Icons.Filled.Savings, null, tint = WalletPurpleAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("REDEEM COINS", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }

            // Wallet Exchange Rate & Redemption Rules Summary
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, WalletPurpleAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = SpiderGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Rate: 50 Coins = ₹50  •  Minimum Withdraw = ${minWithdraw.toInt()} Coins",
                                style = MaterialTheme.typography.labelMedium,
                                color = SpiderGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Only Winning Prize balance can be redeemed. Deposit & Bonus coins are for tournament entry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Add Coin Deposit Requests (if any)
            if (paymentOrders.isNotEmpty()) {
                item {
                    SpiderSectionTitle(title = "ADD COIN REQUESTS")
                }
                items(paymentOrders, key = { it.id }) { order ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "+${order.coinAmount.toInt()} Coins (₹${order.coinAmount.toInt()})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    "Ref: ${order.referenceId.ifBlank { order.gateway }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            StatusPill(text = order.status, color = statusColor(order.status))
                        }
                    }
                }
            }

            // Payout Request History (if any)
            if (payoutRequests.isNotEmpty()) {
                item {
                    SpiderSectionTitle(title = "REDEMPTION REQUESTS")
                }
                items(payoutRequests, key = { it.id }) { req ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${req.amount.toInt()} Coins = ₹${req.amount.toInt()} (${req.destinationType})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    req.destinationDetails.ifBlank { "Reviewed by: ${req.reviewedBy.ifBlank { "Pending" }}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            StatusPill(text = req.status, color = statusColor(req.status))
                        }
                    }
                }
            }

            // WALLET HISTORY
            item {
                SpiderSectionTitle(title = "WALLET HISTORY", subtitle = "Verified ledger records")
            }

            when (transactionsState) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpiderRed)
                    }
                }
                is UiState.Error -> item {
                    SpiderEmptyState(title = "Unable to load ledger", subtitle = transactionsState.message)
                }
                is UiState.Success -> {
                    val txList = transactionsState.data
                    if (txList.isEmpty()) {
                        item {
                            SpiderEmptyState(
                                title = "No Wallet Transactions Yet",
                                subtitle = "Deposits, tournament entries, winning prizes, bonuses, and payouts appear here."
                            )
                        }
                    } else {
                        items(txList, key = { it.id }) { tx ->
                            TransactionHistoryCard(tx)
                        }
                    }
                }
            }
        }
    }

    if (showAddCoinsDialog) {
        AddCoinsDialog(
            walletConfig = walletConfig,
            onDismiss = { showAddCoinsDialog = false },
            onSubmit = { amount, refId, done ->
                onSubmitDeposit(amount, refId) { ok, err ->
                    done(ok, err)
                    if (ok) showAddCoinsDialog = false
                }
            }
        )
    }

    if (showRedeemDialog) {
        RedeemWinningDialog(
            winningBalance = wallet.winningBalance,
            minWithdraw = minWithdraw,
            onDismiss = { showRedeemDialog = false },
            onSubmit = { amount, destType, destDetails, done ->
                onSubmitWithdrawal(amount, destType, destDetails) { ok, err ->
                    done(ok, err)
                    if (ok) showRedeemDialog = false
                }
            }
        )
    }
}

@Composable
private fun WalletBucketCard(
    label: String,
    amount: Double,
    hint: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
            Text(
                String.format(Locale.US, "%.0f", amount),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(hint, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun AddCoinsDialog(
    walletConfig: WalletConfig,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, (Boolean, String?) -> Unit) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var amountText by remember { mutableStateOf("50") }
    var refId by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val validAmount = parsedAmount in 1.0..100000.0
    val fc = walletFieldColors()

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = SpiderCardDark,
        title = {
            Column {
                Text("Add Coins", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text("50 Coins = ₹50  (1 Coin = ₹1)", color = SpiderGold, style = MaterialTheme.typography.labelMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // QR Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpiderSurfaceElevated,
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (walletConfig.qrImageUrl.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.size(180.dp)
                            ) {
                                SpiderArtworkImage(
                                    imageUrl = walletConfig.qrImageUrl,
                                    presetKey = "logo",
                                    contentDescription = "Admin Payment QR Code",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = null,
                                tint = SpiderGold,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Payment QR is being configured by Admin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpiderAmber
                            )
                        }

                        if (walletConfig.upiId.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "UPI ID: ${walletConfig.upiId}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SpiderEmerald
                                )
                                IconButton(
                                    onClick = { clipboard.setText(AnnotatedString(walletConfig.upiId)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, "Copy UPI ID", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Text(
                            walletConfig.instructions.ifBlank { "Pay any amount via QR and submit your UTR / Transaction Reference ID." },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Quick Pick Amount Chips + Custom Input
                Text("Choose Amount (Coins / ₹)", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50, 100, 250, 500, 1000).forEach { preset ->
                        FilterChip(
                            selected = parsedAmount.toInt() == preset,
                            onClick = { amountText = preset.toString(); errorText = null },
                            label = { Text("$preset (₹$preset)", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpiderRed,
                                selectedLabelColor = Color.White,
                                containerColor = SpiderSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() }
                        errorText = null
                    },
                    label = { Text("Enter Amount you want to pay (Coins = ₹)") },
                    leadingIcon = { Icon(Icons.Filled.CurrencyRupee, null, tint = SpiderGold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth().testTag("custom_deposit_amount_input")
                )

                if (validAmount) {
                    Text(
                        "Pay ₹${parsedAmount.toInt()} -> Receive ${parsedAmount.toInt()} Deposit Coins (50 Coins = ₹50)",
                        style = MaterialTheme.typography.labelMedium,
                        color = SpiderEmerald
                    )
                }

                OutlinedTextField(
                    value = refId,
                    onValueChange = { refId = it; errorText = null },
                    label = { Text("UTR / Transaction Ref ID*") },
                    singleLine = true,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth().testTag("deposit_utr_input")
                )

                errorText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = SpiderRedBright)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitting = true
                    errorText = null
                    onSubmit(parsedAmount, refId) { ok, err ->
                        submitting = false
                        if (!ok) errorText = err ?: "Deposit submission failed"
                    }
                },
                enabled = !submitting && validAmount && refId.trim().length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                modifier = Modifier.testTag("submit_deposit_button")
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Deposit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun RedeemWinningDialog(
    winningBalance: Double,
    minWithdraw: Double,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String, (Boolean, String?) -> Unit) -> Unit
) {
    val effectiveMin = minWithdraw.coerceAtLeast(50.0)
    var amountText by remember { mutableStateOf(effectiveMin.toInt().toString()) }
    var destinationType by remember { mutableStateOf("UPI") }
    var destinationDetails by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val canRedeem = winningBalance >= effectiveMin && parsedAmount >= effectiveMin && parsedAmount <= winningBalance
    val fc = walletFieldColors()

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = SpiderCardDark,
        title = {
            Column {
                Text("Redeem Winning Coins", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text("50 Coins = ₹50  •  Min Withdraw = ${effectiveMin.toInt()} Coins", color = SpiderGold, style = MaterialTheme.typography.labelMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SpiderEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SpiderEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, null, tint = SpiderEmerald, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Eligible Winning Balance: ${String.format(Locale.US, "%.2f", winningBalance)} Coins (₹${winningBalance.toInt()})",
                                style = MaterialTheme.typography.labelLarge,
                                color = SpiderEmerald
                            )
                            Text(
                                "Only Winning Prize can be redeemed. Deposit & Bonus are non-withdrawable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (winningBalance < effectiveMin) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SpiderAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SpiderAmber.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Minimum withdrawal is ${effectiveMin.toInt()} Winning Coins. Win matches to grow your Winning balance!",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpiderAmber,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("UPI", "BANK").forEach { mode ->
                        FilterChip(
                            selected = destinationType == mode,
                            onClick = { destinationType = mode },
                            label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpiderRed,
                                selectedLabelColor = Color.White,
                                containerColor = SpiderSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                    if (winningBalance >= effectiveMin) {
                        FilterChip(
                            selected = parsedAmount.toInt() == winningBalance.toInt(),
                            onClick = { amountText = winningBalance.toInt().toString(); errorText = null },
                            label = { Text("MAX (${winningBalance.toInt()})", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WalletPurpleAccent,
                                selectedLabelColor = Color.White,
                                containerColor = SpiderSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() }
                        errorText = null
                    },
                    label = { Text("Withdraw Coins (Min ${effectiveMin.toInt()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
                )

                OutlinedTextField(
                    value = destinationDetails,
                    onValueChange = { destinationDetails = it; errorText = null },
                    label = { Text(if (destinationType == "UPI") "Your UPI ID (e.g. name@upi)*" else "Account No & IFSC*") },
                    singleLine = true,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth().testTag("withdraw_destination_input")
                )

                errorText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = SpiderRedBright)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitting = true
                    errorText = null
                    onSubmit(parsedAmount, destinationType, destinationDetails) { ok, err ->
                        submitting = false
                        if (!ok) errorText = err ?: "Withdrawal request failed"
                    }
                },
                enabled = !submitting && canRedeem && destinationDetails.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                modifier = Modifier.testTag("submit_withdraw_button")
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Withdraw ₹${parsedAmount.toInt()}")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun walletFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SpiderRed,
    unfocusedBorderColor = SpiderCardBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextMuted
)

@Composable
private fun TransactionHistoryCard(tx: WalletTransaction) {
    val isCredit = tx.direction == "CREDIT"
    val dateStr = remember(tx.createdAt) {
        tx.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(it)
        } ?: "Pending timestamp"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SpiderCardDark,
        border = BorderStroke(1.dp, SpiderCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isCredit) SpiderEmerald.copy(alpha = 0.16f) else SpiderRed.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = tx.direction,
                        tint = if (isCredit) SpiderEmerald else SpiderRedBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(text = tx.direction, color = if (isCredit) SpiderEmerald else SpiderRedBright)
                        Text(tx.type, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Text(tx.description, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (tx.matchId.isNotBlank()) {
                        Text("Match ID: ${tx.matchId}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"}${String.format(Locale.US, "%.2f", tx.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCredit) SpiderEmerald else SpiderRedBright,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bal: ${String.format(Locale.US, "%.2f", tx.resultingBalance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                StatusPill(text = tx.status, color = statusColor(tx.status))
            }
        }
    }
}
