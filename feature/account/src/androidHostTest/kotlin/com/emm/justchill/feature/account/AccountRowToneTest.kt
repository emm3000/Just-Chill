package com.emm.justchill.feature.account

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.ui.atoms.AmountTone
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountRowToneTest {

    @Test
    fun `a silent account is muted, whatever its empty net would sign`() {
        assertEquals(AmountTone.Mute, accountNetTone(movementCount = 0, netIsPositive = false))
    }

    @Test
    fun `an account whose income outweighs its spend takes success`() {
        assertEquals(AmountTone.Pos, accountNetTone(movementCount = 2, netIsPositive = true))
    }

    @Test
    fun `an account that spent more than it earned stays monochrome`() {
        assertEquals(AmountTone.Neutral, accountNetTone(movementCount = 1, netIsPositive = false))
    }

    @Test
    fun `the row's net tone is muted when silent, positive when income outweighs spend, else neutral`() {
        val silent: AccountMonthUi = accountRow(movementCount = 0, netIsPositive = false)
        val earning: AccountMonthUi = accountRow(movementCount = 2, netIsPositive = true)
        val spending: AccountMonthUi = accountRow(movementCount = 1, netIsPositive = false)

        assertEquals(
            listOf(AccountNetTone.Muted, AccountNetTone.Positive, AccountNetTone.Neutral),
            listOf(silent.netTone, earning.netTone, spending.netTone),
        )
    }

    @Test
    fun `the row's id is its account's id as a plain string, whatever the account is renamed to`() {
        val original: AccountMonthUi = accountRow(movementCount = 1, netIsPositive = true, name = "BCP")
        val renamed: AccountMonthUi = accountRow(movementCount = 1, netIsPositive = true, name = "Sueldo")

        assertEquals(listOf("acc-1", "acc-1"), listOf(original.id, renamed.id))
    }

    private fun accountRow(movementCount: Int, netIsPositive: Boolean, name: String = "BCP"): AccountMonthUi =
        AccountMonthUi(
            account = Account(accountId = AccountId("acc-1"), name = name),
            movementCount = movementCount,
            net = "S/ 0.00",
            netIsPositive = netIsPositive,
        )
}
