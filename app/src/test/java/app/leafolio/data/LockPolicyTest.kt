package app.leafolio.data

import org.junit.Assert.*
import org.junit.Test

class LockPolicyTest {
    @Test fun lockTimeoutAndDisabledBoundary() {
        assertFalse(LockPolicy.needsLock(-1, 1, 100000))
        assertFalse(LockPolicy.needsLock(60, 1, 60000))
        assertTrue(LockPolicy.needsLock(60, 1, 60001))
        assertTrue(LockPolicy.needsLock(0, 1, 1))
        assertFalse(LockPolicy.needsLock(0, null, 1))
    }
}
