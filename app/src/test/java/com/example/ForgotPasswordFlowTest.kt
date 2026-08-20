package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthResult
import com.example.auth.FirebaseAuthManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ForgotPasswordFlowTest {

    private lateinit var context: Context
    private lateinit var authManager: FirebaseAuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = FirebaseAuthManager(context)
    }

    @Test
    fun testSendPasswordResetEmail_validEmail_returnsSuccess() = runBlocking {
        val email = "premium.investor@quantum-realestate.it"
        val result = authManager.sendPasswordResetEmail(email)

        assertTrue("Expected Success for valid email", result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals(email, success.email)
    }

    @Test
    fun testSendPasswordResetEmail_trimmedEmail_returnsSuccess() = runBlocking {
        val rawEmail = "   investitore.pro@fondo-italia.com   "
        val result = authManager.sendPasswordResetEmail(rawEmail)

        assertTrue("Expected Success for email with whitespace", result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals("investitore.pro@fondo-italia.com", success.email)
    }

    @Test
    fun testSendPasswordResetEmail_emptyEmail_returnsError() = runBlocking {
        val result = authManager.sendPasswordResetEmail("   ")

        assertTrue("Expected Error for empty email", result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertTrue(error.message.contains("valido", ignoreCase = true))
    }

    @Test
    fun testSendPasswordResetEmail_invalidFormat_returnsError() = runBlocking {
        val result = authManager.sendPasswordResetEmail("not-an-email-address")

        assertTrue("Expected Error for invalid email format", result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertTrue(error.message.contains("valido", ignoreCase = true))
    }

    @Test
    fun testSendPasswordResetEmail_preservesActiveSubscriptionClaims() = runBlocking {
        // Set premium subscription
        authManager.setSubscriptionPlanAndClaims(plan = "ANNUAL", role = "pro_investor")
        
        // Trigger password reset
        val result = authManager.sendPasswordResetEmail("vip.investor@capital.com")
        assertTrue(result is AuthResult.Success)

        // Verify claims are unchanged
        val claims = authManager.fetchCustomClaims()
        assertTrue(claims.isPremium)
        assertEquals("ANNUAL", claims.plan)
        assertEquals("pro_investor", claims.role)
    }
}
