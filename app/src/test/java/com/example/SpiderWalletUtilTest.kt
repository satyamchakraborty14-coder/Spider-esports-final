package com.example

import com.example.data.SpiderWalletUtil
import com.example.data.SpiderWalletUtil.toWriteMap
import com.example.data.WalletStateValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpiderWalletUtilTest {

    @Test
    fun `payout hold deducts winning balance immediately and payout approval only increments payoutsTotal`() {
        val initial = WalletStateValues(deposited = 40.0, winning = 200.0, bonus = 10.0, payoutsTotal = 0.0)

        val held = SpiderWalletUtil.computePayoutHold(initial, 100.0)
        assertEquals(40.0, held.deposited, 0.001)
        assertEquals(100.0, held.winning, 0.001)
        assertEquals(10.0, held.bonus, 0.001)
        assertEquals(150.0, held.total, 0.001)
        assertEquals(0.0, held.payoutsTotal, 0.001)

        val paid = SpiderWalletUtil.computePayoutReview(held, 100.0, approve = true, wasHeldOnSubmit = true)
        assertEquals(100.0, paid.winning, 0.001)
        assertEquals(150.0, paid.total, 0.001)
        assertEquals(100.0, paid.payoutsTotal, 0.001)
    }

    @Test
    fun `payout hold rejection refunds held winning balance and restores total`() {
        val initial = WalletStateValues(deposited = 20.0, winning = 120.0, bonus = 5.0, payoutsTotal = 50.0)
        val held = SpiderWalletUtil.computePayoutHold(initial, 75.0)
        assertEquals(45.0, held.winning, 0.001)
        assertEquals(70.0, held.total, 0.001)

        val refunded = SpiderWalletUtil.computePayoutReview(held, 75.0, approve = false, wasHeldOnSubmit = true)
        assertEquals(120.0, refunded.winning, 0.001)
        assertEquals(145.0, refunded.total, 0.001)
        assertEquals(50.0, refunded.payoutsTotal, 0.001)
    }

    @Test
    fun `legacy unheld payout approval deducts winning balance once and rejection leaves wallet unchanged`() {
        val initial = WalletStateValues(deposited = 10.0, winning = 150.0, bonus = 0.0, payoutsTotal = 0.0)

        val rejected = SpiderWalletUtil.computePayoutReview(initial, 60.0, approve = false, wasHeldOnSubmit = false)
        assertEquals(initial, rejected)

        val approved = SpiderWalletUtil.computePayoutReview(initial, 60.0, approve = true, wasHeldOnSubmit = false)
        assertEquals(90.0, approved.winning, 0.001)
        assertEquals(100.0, approved.total, 0.001)
        assertEquals(60.0, approved.payoutsTotal, 0.001)
    }

    @Test(expected = IllegalStateException::class)
    fun `payout hold fails when winning balance is insufficient even if total is higher`() {
        val initial = WalletStateValues(deposited = 500.0, winning = 40.0, bonus = 100.0, payoutsTotal = 0.0)
        SpiderWalletUtil.computePayoutHold(initial, 50.0)
    }

    @Test
    fun `admin adjustment credits and debits each bucket cleanly and syncs total`() {
        val initial = WalletStateValues(deposited = 50.0, winning = 100.0, bonus = 25.0, payoutsTotal = 0.0)

        val afterWinCredit = SpiderWalletUtil.computeAdminAdjustment(initial, 75.0, "CREDIT", "WINNING")
        assertEquals(175.0, afterWinCredit.winning, 0.001)
        assertEquals(250.0, afterWinCredit.total, 0.001)

        val afterDepCredit = SpiderWalletUtil.computeAdminAdjustment(afterWinCredit, 30.0, "CREDIT", "DEPOSIT")
        assertEquals(80.0, afterDepCredit.deposited, 0.001)
        assertEquals(280.0, afterDepCredit.total, 0.001)

        val afterBonusDebit = SpiderWalletUtil.computeAdminAdjustment(afterDepCredit, 25.0, "DEBIT", "BONUS")
        assertEquals(0.0, afterBonusDebit.bonus, 0.001)
        assertEquals(255.0, afterBonusDebit.total, 0.001)
    }

    @Test(expected = IllegalStateException::class)
    fun `admin debit rejects amount exceeding selected bucket instead of clamping`() {
        val initial = WalletStateValues(deposited = 50.0, winning = 20.0, bonus = 0.0, payoutsTotal = 0.0)
        SpiderWalletUtil.computeAdminAdjustment(initial, 30.0, "DEBIT", "WINNING")
    }

    @Test
    fun `deposit approval, prize update, bonus credit, and tournament join debit compute exact totals`() {
        val start = WalletStateValues(deposited = 0.0, winning = 0.0, bonus = 0.0, payoutsTotal = 0.0)
        val afterDeposit = SpiderWalletUtil.computeDepositApproval(start, 100.0)
        assertEquals(100.0, afterDeposit.deposited, 0.001)
        assertEquals(100.0, afterDeposit.total, 0.001)

        val afterBonus = SpiderWalletUtil.computeBonusCredit(afterDeposit, 25.0)
        assertEquals(25.0, afterBonus.bonus, 0.001)
        assertEquals(125.0, afterBonus.total, 0.001)

        val afterPrize = SpiderWalletUtil.computeWinningPrizeUpdate(afterBonus, 150.0)
        assertEquals(150.0, afterPrize.winning, 0.001)
        assertEquals(275.0, afterPrize.total, 0.001)

        // Tournament join of 140 uses Deposit(100) + Bonus(25) + Winning(15) = 140
        val afterJoin = SpiderWalletUtil.computeTournamentJoinDebit(afterPrize, 140.0)
        assertEquals(0.0, afterJoin.deposited, 0.001)
        assertEquals(0.0, afterJoin.bonus, 0.001)
        assertEquals(135.0, afterJoin.winning, 0.001)
        assertEquals(135.0, afterJoin.total, 0.001)

        val writeMap = afterJoin.toWriteMap("uid_1", "NOW", includeCreatedAt = false)
        assertEquals(135.0, writeMap["totalBalance"])
        assertEquals(135.0, writeMap["winningBalance"])
        assertEquals(0.0, writeMap["bonusBalance"])
        assertEquals(0.0, writeMap["earningsBalance"])
        assertFalse(writeMap.containsKey("createdAt"))

        val createMap = afterJoin.toWriteMap("uid_1", "NOW", includeCreatedAt = true)
        assertTrue(createMap.containsKey("createdAt"))
        assertEquals("uid_1", createMap["userId"])
    }
}
