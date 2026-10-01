package com.seekerspark.game

import kotlin.random.Random

/** Pure game rules; time comes from an active-play monotonic clock. */
class GameEngine(private val random: Random = Random.Default) {
    enum class Kind { SPARK, TRAP }
    data class Target(val cell: Int, val kind: Kind, val born: Long, val expires: Long)
    data class Hit(val points: Int, val kind: Kind)
    val targets = mutableListOf<Target>()
    var score = 0; private set
    var combo = 0; private set
    var hits = 0; private set
    var lives = 3; private set
    var finished = false; private set
    private var nextSpawn = 0L
    var elapsed = 0L; private set
    val remainingMs: Long get() = (DURATION_MS - elapsed).coerceAtLeast(0)

    fun update(activeMs: Long) {
        if (finished) return
        elapsed = maxOf(elapsed, activeMs.coerceAtLeast(0))
        if (elapsed >= DURATION_MS || lives <= 0) {
            finished = true
            targets.clear()
            return
        }
        val expired = targets.filter { it.expires <= elapsed }
        if (expired.any { it.kind == Kind.SPARK }) combo = 0
        targets.removeAll(expired.toSet())
        if (elapsed >= nextSpawn) {
            val free = (0 until 12).filter { cell -> targets.none { it.cell == cell } }
            if (free.isNotEmpty() && targets.size < 5) {
                val kind = if (random.nextDouble() < 0.23) Kind.TRAP else Kind.SPARK
                val lifetime = (1550L - elapsed / 50).coerceAtLeast(850)
                targets += Target(free[random.nextInt(free.size)], kind, elapsed, elapsed + lifetime)
            }
            nextSpawn = elapsed + (650L - elapsed / 100).coerceAtLeast(350)
        }
    }

    fun tap(cell: Int, activeMs: Long): Hit? {
        update(activeMs)
        if (finished) return null
        val target = targets.firstOrNull { it.cell == cell } ?: return null
        targets.remove(target)
        return if (target.kind == Kind.SPARK) {
            combo += 1; hits += 1
            val points = 10 + minOf(combo - 1, 4) * 2
            score += points
            Hit(points, target.kind)
        } else {
            combo = 0; lives -= 1
            score = (score - 10).coerceAtLeast(0)
            if (lives == 0) { finished = true; targets.clear() }
            Hit(-10, target.kind)
        }
    }

    companion object { const val DURATION_MS = 30_000L }
}
