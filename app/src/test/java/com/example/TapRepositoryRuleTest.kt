package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.TapRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TapRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun getOrCreateUserProfile_authenticatedUser_createsAndReadsProfile() = runBlocking {
    val email = "alice_${System.nanoTime()}@test.com"
    val uid = signInTestUser(email)
    val repository = TapRepository(firestore)

    val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getOrCreateUserProfile(uid, email, "AliceTapper", null)
    }

    assertNotNull(profile)
    assertEquals(uid, profile.userId)
    assertEquals("AliceTapper", profile.username)
    assertEquals(email, profile.email)
  }

  @Test
  fun syncTapsBatch_authenticatedUser_incrementsBalance() = runBlocking {
    val email = "bob_${System.nanoTime()}@test.com"
    val uid = signInTestUser(email)
    val repository = TapRepository(firestore)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getOrCreateUserProfile(uid, email, "BobTapper", null)
      repository.syncTapsBatch(
        userId = uid,
        coinsEarned = 500L,
        energyConsumed = 50,
        newEnergy = 950,
        tapsCount = 50L
      )
    }

    val balance = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.observeUserBalance(uid).first { it != null && it.availableCoins >= 500L }
    }

    assertNotNull(balance)
    assertEquals(500L, balance!!.availableCoins)
    assertEquals(50L, balance.totalTaps)
  }

  @Test
  fun syncLocalTapsToRealtimeDatabase_updatesBalance() = runBlocking {
    val email = "charlie_${System.nanoTime()}@test.com"
    val uid = signInTestUser(email)
    val repository = TapRepository(firestore)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getOrCreateUserProfile(uid, email, "CharlieTapper", null)
      val balance = repository.syncLocalTapsToRealtimeDatabase(
        userId = uid,
        localTapsCount = 25L,
        coinsEarned = 250L,
        remainingEnergy = 975
      )
      assertNotNull(balance)
      assertTrue(balance.availableCoins >= 250L)
      assertTrue(balance.totalTaps >= 25L)
    }

    val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.observeUserBalance(uid).first { it != null && it.availableCoins >= 250L }
    }
    assertNotNull(observed)
    assertEquals(250L, observed!!.availableCoins)
    assertEquals(25L, observed.totalTaps)
  }

  @Test
  fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = TapRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserProfile("unauthenticated_target").first()
      }
      fail("Expected snapshot listener to fail for unauthenticated caller")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertTrue(
        "Expected permission denied or unauthenticated exception, got: $e",
        firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
        e is IllegalStateException ||
        e.message?.contains("PERMISSION_DENIED") == true
      )
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
