package africa.matchpass.sdk.internal.ui

import africa.matchpass.sdk.AccessResult
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassGrant
import africa.matchpass.sdk.internal.AccessChecker
import africa.matchpass.sdk.internal.GuestSessionDto
import africa.matchpass.sdk.internal.IssuePassDto
import africa.matchpass.sdk.internal.MatchPassClient
import africa.matchpass.sdk.internal.MatchPassService
import africa.matchpass.sdk.internal.MatchPassStore
import africa.matchpass.sdk.internal.OtpRequestDto
import africa.matchpass.sdk.internal.OtpResponseDto
import africa.matchpass.sdk.internal.OtpVerifyDto
import africa.matchpass.sdk.internal.PassDto
import africa.matchpass.sdk.internal.ValidatePassDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val config = MatchPassConfig(apiKey = "test-key")
    private val content = MatchPassContent(id = "epl-match-001", title = "Arsenal vs Man City", price = "29.00")
    private val service = mockk<MatchPassService>()
    private val store = mockk<MatchPassStore>(relaxed = true)
    private val checker = mockk<AccessChecker>()
    private val client = mockk<MatchPassClient> { every { this@mockk.service } returns this@PaywallViewModelTest.service }

    private val capturedGrants = mutableListOf<MatchPassGrant>()
    private lateinit var viewModel: PaywallViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { store.getPhone() } returns ""
        // userPhone is last — must use named param because it's not a function type
        viewModel = PaywallViewModel(
            config = config,
            content = content,
            client = client,
            store = store,
            checker = checker,
            onAccessGranted = { grant -> capturedGrants.add(grant) },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ──────────────────────────────────────────────────────────

    @Test
    fun `initial step is EnteringPhone`() {
        assertEquals(PaywallStep.EnteringPhone, viewModel.state.value.step)
    }

    @Test
    fun `initial phone is pre-filled from store`() {
        every { store.getPhone() } returns "+27821234567"
        val vm = PaywallViewModel(
            config = config, content = content, client = client, store = store,
            checker = checker, onAccessGranted = {},
        )
        assertEquals("+27821234567", vm.state.value.phoneNumber)
    }

    // ── Field mutations ────────────────────────────────────────────────────────

    @Test
    fun `setPhone updates phoneNumber and clears error`() = runTest {
        viewModel.setPhone("+27831234567")
        assertEquals("+27831234567", viewModel.state.value.phoneNumber)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `setOtp updates otpCode and clears error`() = runTest {
        viewModel.setOtp("123456")
        assertEquals("123456", viewModel.state.value.otpCode)
        assertNull(viewModel.state.value.error)
    }

    // ── onStart ────────────────────────────────────────────────────────────────

    @Test
    fun `onStart stays on EnteringPhone when no token and no known phone`() = runTest {
        every { store.getToken(content.id) } returns null
        viewModel.onStart()
        advanceUntilIdle()
        assertEquals(PaywallStep.EnteringPhone, viewModel.state.value.step)
        coVerify(exactly = 0) { checker.check(any()) }
    }

    @Test
    fun `onStart fires onAccessGranted when stored pass is still valid`() = runTest {
        every { store.getToken(content.id) } returns "tok-valid"
        val grant = MatchPassGrant(token = "tok-valid", contentId = content.id, expiresAt = "2099-01-01T00:00:00Z")
        coEvery { checker.check(content) } returns AccessResult.Granted(grant)

        viewModel.onStart()
        advanceUntilIdle()

        assertEquals(1, capturedGrants.size)
        assertEquals("tok-valid", capturedGrants[0].token)
    }

    @Test
    fun `onStart goes to Confirming with error when stored pass is expired`() = runTest {
        every { store.getToken(content.id) } returns "tok-old"
        coEvery { checker.check(content) } returns AccessResult.Expired("2020-01-01T00:00:00Z")

        viewModel.onStart()
        advanceUntilIdle()

        // Expired → Confirming (let user repurchase), not EnteringPhone
        assertEquals(PaywallStep.Confirming, viewModel.state.value.step)
        assertNotNull(viewModel.state.value.error)
        assertTrue(capturedGrants.isEmpty())
    }

    @Test
    fun `onStart goes to Confirming when phone is known but no server pass exists`() = runTest {
        every { store.getToken(content.id) } returns null
        every { store.getPhone() } returns "+27821234567"
        // Server returns 404 — no existing pass, proceed to purchase
        coEvery { service.lookupPass(any(), "+27821234567", content.id) } throws
            retrofit2.HttpException(
                okhttp3.ResponseBody.create(null, "").let {
                    retrofit2.Response.error<Any>(404, it)
                }
            )

        viewModel.onStart()
        advanceUntilIdle()

        assertEquals(PaywallStep.Confirming, viewModel.state.value.step)
    }

    @Test
    fun `onStart restores pass and fires onAccessGranted when server has a valid pass`() = runTest {
        every { store.getToken(content.id) } returns null
        every { store.getPhone() } returns "+27821234567"
        val dto = africa.matchpass.sdk.internal.LookupPassDto(
            token = "restored-tok",
            contentId = content.id,
            expiresAt = "2099-01-01T00:00:00Z",
            valid = true,
        )
        coEvery { service.lookupPass(any(), "+27821234567", content.id) } returns dto

        viewModel.onStart()
        advanceUntilIdle()

        verify { store.savePass(content.id, "restored-tok") }
        assertEquals(1, capturedGrants.size)
        assertEquals("restored-tok", capturedGrants[0].token)
    }

    // ── requestOtp ─────────────────────────────────────────────────────────────

    @Test
    fun `requestOtp with blank phone does nothing`() = runTest {
        viewModel.setPhone("   ")
        viewModel.requestOtp()
        advanceUntilIdle()
        assertEquals(PaywallStep.EnteringPhone, viewModel.state.value.step)
        coVerify(exactly = 0) { service.requestOtp(any(), any()) }
    }

    @Test
    fun `requestOtp transitions to AwaitingOtp on success`() = runTest {
        viewModel.setPhone("+27821234567")
        coEvery { service.requestOtp(any(), OtpRequestDto("+27821234567")) } returns
            OtpResponseDto(message = "OTP sent", otp = "")

        viewModel.requestOtp()
        advanceUntilIdle()

        assertEquals(PaywallStep.AwaitingOtp, viewModel.state.value.step)
    }

    @Test
    fun `requestOtp sets demoOtp when server returns one`() = runTest {
        viewModel.setPhone("+27821234567")
        coEvery { service.requestOtp(any(), any()) } returns OtpResponseDto(otp = "987654")

        viewModel.requestOtp()
        advanceUntilIdle()

        assertEquals("987654", viewModel.state.value.demoOtp)
    }

    @Test
    fun `requestOtp returns to EnteringPhone with error on failure`() = runTest {
        viewModel.setPhone("+27821234567")
        coEvery { service.requestOtp(any(), any()) } throws IOException("Network unreachable")

        viewModel.requestOtp()
        advanceUntilIdle()

        assertEquals(PaywallStep.EnteringPhone, viewModel.state.value.step)
        assertNotNull(viewModel.state.value.error)
    }

    // ── verifyOtp ──────────────────────────────────────────────────────────────

    @Test
    fun `verifyOtp transitions to Confirming on success`() = runTest {
        viewModel.setPhone("+27821234567")
        viewModel.setOtp("123456")
        coEvery { service.verifyOtp(OtpVerifyDto("+27821234567", "123456")) } returns
            GuestSessionDto(sessionToken = "sess-tok", userRef = "+27821234567")

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals(PaywallStep.Confirming, viewModel.state.value.step)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `verifyOtp shows error when OTP is wrong`() = runTest {
        viewModel.setPhone("+27821234567")
        viewModel.setOtp("000000")
        coEvery { service.verifyOtp(any()) } throws RuntimeException("Invalid OTP")

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
    }

    // ── confirmAndPay ──────────────────────────────────────────────────────────

    @Test
    fun `confirmAndPay transitions through all steps then reaches AccessGranted`() = runTest {
        viewModel.setPhone("+27821234567")
        val issuedPass = PassDto(token = "pass-tok", contentId = content.id, expiresAt = "2099-01-01T00:00:00Z")
        coEvery {
            service.issuePass("ApiKey test-key", IssuePassDto(content.id, "+27821234567", "29.00", "ZAR"))
        } returns issuedPass
        coEvery { service.validatePass("ApiKey test-key", "pass-tok") } returns
            ValidatePassDto(isValid = true, status = "active", expiresAt = "2099-01-01T00:00:00Z")

        viewModel.confirmAndPay()
        advanceTimeBy(3_000)
        advanceUntilIdle()

        // Grant is stored in state — not yet fired (user must tap Watch Now)
        assertEquals(PaywallStep.AccessGranted, viewModel.state.value.step)
        assertNotNull(viewModel.state.value.issuedGrant)
        assertTrue(capturedGrants.isEmpty())
    }

    @Test
    fun `watchContent fires onAccessGranted after successful purchase`() = runTest {
        viewModel.setPhone("+27821234567")
        val issuedPass = PassDto(token = "pass-tok", contentId = content.id, expiresAt = "2099-01-01T00:00:00Z")
        coEvery { service.issuePass(any(), any()) } returns issuedPass
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = true, status = "active", expiresAt = "2099-01-01T00:00:00Z")

        viewModel.confirmAndPay()
        advanceTimeBy(3_000)
        advanceUntilIdle()

        viewModel.watchContent()

        assertEquals(1, capturedGrants.size)
        assertEquals("pass-tok", capturedGrants[0].token)
    }

    @Test
    fun `confirmAndPay returns to Confirming with error when issuePass fails`() = runTest {
        viewModel.setPhone("+27821234567")
        coEvery { service.issuePass(any(), any()) } throws IOException("Server error")

        viewModel.confirmAndPay()
        advanceTimeBy(3_000)
        advanceUntilIdle()

        assertEquals(PaywallStep.Confirming, viewModel.state.value.step)
        assertNotNull(viewModel.state.value.error)
        assertTrue(capturedGrants.isEmpty())
    }

    @Test
    fun `confirmAndPay saves pass to store after successful issuance`() = runTest {
        viewModel.setPhone("+27821234567")
        val issuedPass = PassDto(token = "pass-tok", contentId = content.id, expiresAt = "2099-01-01T00:00:00Z")
        coEvery { service.issuePass(any(), any()) } returns issuedPass
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = true, status = "active")

        viewModel.confirmAndPay()
        advanceTimeBy(3_000)
        advanceUntilIdle()

        verify { store.savePass(content.id, "pass-tok") }
    }
}
