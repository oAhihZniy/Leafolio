package app.leafolio.data

object LockPolicy {
    fun needsLock(delaySeconds: Int, leftAt: Long?, now: Long): Boolean =
        delaySeconds >= 0 && leftAt != null && now - leftAt >= delaySeconds * 1000L
}
