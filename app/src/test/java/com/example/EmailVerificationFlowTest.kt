package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthResult
import com.example.auth.FirebaseAuthManager
import com.example.data.InvestorProfile
import com.example.data.PropertyRepository
import com.example.ui.DealRadarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EmailVerificationFlowTest {

    private lateinit var context: Context
    private lateinit var authManager: FirebaseAuthManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        authManager = FirebaseAuthManager(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSignUpWithEmail_triggersMandatoryVerificationFlow() = runTest {
        val email = "new.investor@quantum-capital.it"
        val password = "SecurePassword123!"

        val result = authManager.signUpWithEmail(email, password)

        // Must require verification or return pending verification
        assertTrue("Expected RequiresVerification upon sign up", result is AuthResult.RequiresVerification)
        val req = result as AuthResult.RequiresVerification
        assertEquals(email, req.email)
        assertFalse("User should not be verified immediately", authManager.isEmailVerified())
    }

    @Test
    fun testSendEmailVerification_succeedsForRegisteredUser() = runTest {
        val email = "verified.lead@realestate.it"
        authManager.signUpWithEmail(email, "StrongPass999")

        val verificationResult = authManager.sendEmailVerification()

        assertTrue("Expected Success when dispatching verification email", verificationResult is AuthResult.Success)
        val success = verificationResult as AuthResult.Success
        assertFalse("Account should remain unverified until confirmation link is clicked", success.isEmailVerified)
    }

    @Test
    fun testCompleteEmailVerification_unlocksVerifiedStatus() = runTest {
        val email = "active.investor@milanodeals.it"
        authManager.signUpWithEmail(email, "Password2026")
        assertFalse(authManager.isEmailVerified())

        // Simulate user clicking email verification link
        authManager.setSimulatedEmailVerified(true)

        assertTrue("Account must be marked verified after link confirmation", authManager.isEmailVerified())
    }

    @Test
    fun testSignIn_unverifiedAccountReturnsRequiresVerification() = runTest {
        val email = "pending.user@funds.it"
        authManager.setSimulatedEmailVerified(false)

        val result = authManager.signInWithEmail(email, "Pass12345")

        assertTrue("SignIn should require verification if email is unconfirmed", result is AuthResult.RequiresVerification)
    }

    @Test
    fun testSignIn_verifiedAccountReturnsSuccess() = runTest {
        val email = "verified.user@funds.it"
        authManager.setSimulatedEmailVerified(true)

        val result = authManager.signInWithEmail(email, "Pass12345")

        assertTrue("SignIn should succeed for verified accounts", result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertTrue(success.isEmailVerified)
    }

    @Test
    fun testSimulateEmailVerificationComplete_togglesState() = runTest {
        authManager.setSimulatedEmailVerified(false)
        assertFalse(authManager.isEmailVerified())

        authManager.setSimulatedEmailVerified(true)
        assertTrue(authManager.isEmailVerified())
    }
}
