package com.emm.justchill.feature.account

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.ui.atoms.AmountTone
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountRowToneTest {

    @Test
    fun `a silent account is muted, whatever its empty net would sign`() {
        assertEquals(AccountNetTone.Muted, accountRow(movementCount = 0, netIsPositive = false).netTone)
    }

    @Test
    fun `an account whose income outweighs its spend takes success`() {
        assertEquals(AccountNetTone.Positive, accountRow(movementCount = 2, netIsPositive = true).netTone)
    }

    @Test
    fun `an account that spent more than it earned stays monochrome`() {
        assertEquals(AccountNetTone.Neutral, accountRow(movementCount = 1, netIsPositive = false).netTone)
    }

    @Test
    fun `the row paints muted as mute, positive as success and neutral as monochrome`() {
        assertEquals(
            listOf(AmountTone.Mute, AmountTone.Pos, AmountTone.Neutral),
            listOf(AccountNetTone.Muted, AccountNetTone.Positive, AccountNetTone.Neutral).map { it.toAmountTone() },
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
