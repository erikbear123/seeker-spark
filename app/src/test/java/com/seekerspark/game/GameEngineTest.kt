package com.seekerspark.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {
    @Test fun roundEndsAtThirtySecondsAndRejectsLateTaps() {
        val game = GameEngine(Random(7)); game.update(0)
        game.update(30_000); assertTrue(game.finished)
        assertEquals(0L, game.remainingMs); assertNull(game.tap(0, 30_001))
    }
    @Test fun eachSparkCanOnlyScoreOnce() {
        val game = GameEngine(Random(1))
        for (time in 0L..15_000L step 100) {
            game.update(time)
            val spark = game.targets.firstOrNull { it.kind == GameEngine.Kind.SPARK } ?: continue
            val first = game.tap(spark.cell, time)
            val score = game.score
            assertNotNull(first); assertNull(game.tap(spark.cell, time)); assertEquals(score, game.score)
            return
        }
        fail("Seed must produce a spark")
    }
    @Test fun threeTrapsEndRoundAndScoresStayNonnegative() {
        val game = GameEngine(Random(4))
        for (time in 0L..29_000L step 50) {
            game.update(time)
            game.targets.toList().filter { it.kind == GameEngine.Kind.TRAP }.forEach { game.tap(it.cell, time) }
            assertTrue(game.score >= 0)
            if (game.finished) break
        }
        assertEquals(0, game.lives); assertTrue(game.finished)
    }
    @Test fun expiredTargetsNeverScoreAndClockCannotMoveBackwards() {
        val game = GameEngine(Random(2)); game.update(0)
        val old = game.targets.first(); game.update(old.expires)
        assertFalse(game.targets.contains(old))
        game.update(0); assertEquals(old.expires, game.elapsed)
    }
    @Test fun targetsStayUniqueAndInsideTheBoard() {
        val game = GameEngine(Random(5))
        for (time in 0L..30_000L step 16) {
            game.update(time)
            assertEquals(game.targets.size, game.targets.map { it.cell }.toSet().size)
            assertTrue(game.targets.all { it.cell in 0..11 }); assertTrue(game.targets.size <= 5)
        }
    }
}
