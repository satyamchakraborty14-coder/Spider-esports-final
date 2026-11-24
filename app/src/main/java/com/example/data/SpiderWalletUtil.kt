package com.example.data

import android.util.Log
import com.example.model.Wallet
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class WalletStateValues(
    val deposited: Double,
    val winning: Double,
    val bonus: Double,
    val payoutsTotal: Double
) {
    val total: Double get() = SpiderWalletUtil.roundCoins(deposited + winning + bonus)
}

object SpiderWalletUtil {

    fun roundCoins(value: Double): Double = round(value * 100.0) / 100.0

    fun parseWalletSnapshot(doc: DocumentSnapshot, fallbackUserId: String): Wallet {
        if (!doc.exists()) {
            return Wallet(id = fallbackUserId, userId = fallbackUserId)
        }
        val uid = (doc.getString("userId") ?: "").ifBlank { fallbackUserId }
        val dep = ((doc.get("depositedBalance") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val win = ((doc.get("winningBalance") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val rawBonus = (doc.get("bonusBalance") as? Number)?.toDouble()
        val rawEarnings = (doc.get("earningsBalance") as? Number)?.toDouble()
        val bonus = (rawBonus ?: rawEarnings ?: 0.0).coerceAtLeast(0.0)
        val payouts = ((doc.get("payoutsTotal") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val rawTotal = (doc.get("totalBalance") as? Number)?.toDouble()
        val computedTotal = roundCoins(dep + win + bonus)
        val total = if (rawTotal == null || abs(rawTotal - computedTotal) > 0.01) computedTotal else roundCoins(rawTotal)
        return Wallet(
            id = doc.id.ifBlank { uid },
            userId = uid,
            totalBalance = total,
            depositedBalance = roundCoins(dep),
            winningBalance = roundCoins(win),
            bonusBalance = roundCoins(bonus),
            earningsBalance = roundCoins(bonus),
            payoutsTotal = roundCoins(payouts),
            createdAt = doc.get("createdAt") as? Timestamp,
            updatedAt = doc.get("updatedAt") as? Timestamp
        )
    }

    fun fromDocumentOrZero(doc: DocumentSnapshot): WalletStateValues {
        val dep = ((doc.get("depositedBalance") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val win = ((doc.get("winningBalance") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val rawBonus = (doc.get("bonusBalance") as? Number)?.toDouble()
        val rawEarnings = (doc.get("earningsBalance") as? Number)?.toDouble()
        val bonus = (rawBonus ?: rawEarnings ?: 0.0).coerceAtLeast(0.0)
        val payouts = ((doc.get("payoutsTotal") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        return WalletStateValues(
            deposited = roundCoins(dep),
            winning = roundCoins(win),
            bonus = roundCoins(bonus),
            payoutsTotal = roundCoins(payouts)
        )
    }

    fun computeTournamentJoinDebit(current: WalletStateValues, entryFee: Double): WalletStateValues {
        val fee = roundCoins(entryFee.coerceAtLeast(0.0))
        if (current.total + 0.001 < fee) {
            throw IllegalStateException(
                "Insufficient coins (${current.total.toInt()} Coins available, ${fee.toInt()} Coins required). Add coins in Wallet to join."
            )
        }
        var remaining = fee
        val depUsed = minOf(current.deposited, remaining)
        val nextDep = roundCoins((current.deposited - depUsed).coerceAtLeast(0.0))
        remaining = roundCoins((remaining - depUsed).coerceAtLeast(0.0))

        val bonusUsed = minOf(current.bonus, remaining)
        val nextBonus = roundCoins((current.bonus - bonusUsed).coerceAtLeast(0.0))
        remaining = roundCoins((remaining - bonusUsed).coerceAtLeast(0.0))

        val winUsed = minOf(current.winning, remaining)
        val nextWin = roundCoins((current.winning - winUsed).coerceAtLeast(0.0))

        return WalletStateValues(
            deposited = nextDep,
            winning = nextWin,
            bonus = nextBonus,
            payoutsTotal = current.payoutsTotal
        )
    }

    fun computeAdminAdjustment(
        current: WalletStateValues,
        amountDelta: Double,
        direction: String,
        bucket: String
    ): WalletStateValues {
        val cleanDelta = roundCoins(amountDelta)
        require(cleanDelta > 0.0) { "Amount must be greater than 0" }
        val normalizedDir = direction.uppercase(Locale.US)
        require(normalizedDir == "CREDIT" || normalizedDir == "DEBIT") { "Invalid wallet direction: $direction" }
        val normalizedBucket = bucket.uppercase(Locale.US).let {
            if (it in listOf("WINNING", "BONUS", "DEPOSIT")) it else "BONUS"
        }
        val signed = if (normalizedDir == "CREDIT") cleanDelta else -cleanDelta

        val nextDep = when (normalizedBucket) {
            "DEPOSIT" -> {
                if (normalizedDir == "DEBIT" && current.deposited + 0.001 < cleanDelta) {
                    throw IllegalStateException("Insufficient DEPOSIT balance (${current.deposited.toInt()} Coins available).")
                }
                roundCoins((current.deposited + signed).coerceAtLeast(0.0))
            }
            else -> current.deposited
        }
        val nextWin = when (normalizedBucket) {
            "WINNING" -> {
                if (normalizedDir == "DEBIT" && current.winning + 0.001 < cleanDelta) {
                    throw IllegalStateException("Insufficient WINNING balance (${current.winning.toInt()} Coins available).")
                }
                roundCoins((current.winning + signed).coerceAtLeast(0.0))
            }
            else -> current.winning
        }
        val nextBonus = when (normalizedBucket) {
            "BONUS" -> {
                if (normalizedDir == "DEBIT" && current.bonus + 0.001 < cleanDelta) {
                    throw IllegalStateException("Insufficient BONUS balance (${current.bonus.toInt()} Coins available).")
                }
                roundCoins((current.bonus + signed).coerceAtLeast(0.0))
            }
            else -> current.bonus
        }

        return WalletStateValues(
            deposited = nextDep,
            winning = nextWin,
            bonus = nextBonus,
            payoutsTotal = current.payoutsTotal
        )
    }

    fun computeDepositApproval(current: WalletStateValues, coinAmount: Double): WalletStateValues {
        val cleanAmount = roundCoins(coinAmount.coerceAtLeast(0.0))
        return current.copy(deposited = roundCoins(current.deposited + cleanAmount))
    }

    fun computePayoutHold(current: WalletStateValues, coinAmount: Double): WalletStateValues {
        val cleanAmount = roundCoins(coinAmount)
        require(cleanAmount >= 50.0) { "Minimum withdrawal is 50 Coins (₹50)." }
        if (current.winning + 0.001 < cleanAmount) {
            throw IllegalStateException(
                "Only Winning Prize balance can be redeemed. Available Winning: ${String.format(Locale.US, "%.2f", current.winning)} Coins."
            )
        }
        val nextWin = roundCoins((current.winning - cleanAmount).coerceAtLeast(0.0))
        return current.copy(winning = nextWin)
    }

    fun computePayoutReview(
        current: WalletStateValues,
        coinAmount: Double,
        approve: Boolean,
        wasHeldOnSubmit: Boolean
    ): WalletStateValues {
        val cleanAmount = roundCoins(coinAmount)
        return if (approve) {
            require(cleanAmount >= 50.0) { "Minimum withdrawal is 50 Coins." }
            if (wasHeldOnSubmit) {
                current.copy(payoutsTotal = roundCoins(current.payoutsTotal + cleanAmount))
            } else {
                if (current.winning + 0.001 < cleanAmount) {
                    throw IllegalStateException(
                        "Insufficient Winning balance (${current.winning.toInt()} Coins) for ${cleanAmount.toInt()} Coins payout."
                    )
                }
                current.copy(
                    winning = roundCoins((current.winning - cleanAmount).coerceAtLeast(0.0)),
                    payoutsTotal = roundCoins(current.payoutsTotal + cleanAmount)
                )
            }
        } else {
            if (wasHeldOnSubmit) {
                current.copy(winning = roundCoins(current.winning + cleanAmount))
            } else {
                current
            }
        }
    }

    fun computeWinningPrizeUpdate(current: WalletStateValues, prizeDelta: Double): WalletStateValues {
        val cleanDelta = roundCoins(prizeDelta)
        return current.copy(winning = roundCoins((current.winning + cleanDelta).coerceAtLeast(0.0)))
    }

    fun computeBonusCredit(current: WalletStateValues, bonusCoins: Double): WalletStateValues {
        val cleanBonus = roundCoins(bonusCoins.coerceAtLeast(0.0))
        return current.copy(bonus = roundCoins(current.bonus + cleanBonus))
    }

    fun WalletStateValues.toWriteMap(userId: String, now: Any, includeCreatedAt: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "totalBalance" to total,
            "depositedBalance" to deposited,
            "winningBalance" to winning,
            "bonusBalance" to bonus,
            "earningsBalance" to bonus,
            "payoutsTotal" to payoutsTotal,
            "updatedAt" to now
        )
        if (includeCreatedAt) {
            map["userId"] = userId
            map["createdAt"] = now
        }
        return map
    }
}

fun <T> Flow<T>.retryTransientFirestoreErrors(
    tag: String,
    shouldRetry: () -> Boolean = { true }
): Flow<T> = this.retryWhen { cause, attempt ->
    if (cause is CancellationException) throw cause
    if (!shouldRetry()) return@retryWhen false
    val delayMs = (1000L shl attempt.toInt().coerceAtMost(3)).coerceAtMost(8000L)
    Log.w(tag, "Transient Firestore stream error (attempt ${attempt + 1}), retrying in ${delayMs}ms", cause)
    delay(delayMs)
    true
}
