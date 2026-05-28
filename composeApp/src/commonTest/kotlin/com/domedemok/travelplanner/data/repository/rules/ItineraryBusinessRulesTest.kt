package com.domedemok.travelplanner.data.repository.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItineraryBusinessRulesTest {

    private val msPerDay = ItineraryBusinessRules.MS_PER_DAY
    private val baseStart = 1_700_000_000_000L  // arbitrary epoch ms

    // ── Validation cases ────────────────────────────────────────────────────

    @Test
    fun `zero start returns empty plan`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(0L, baseStart, emptySet())
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `negative start returns empty plan`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(-1L, baseStart, emptySet())
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `zero end returns empty plan`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(baseStart, 0L, emptySet())
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `end before start returns empty plan`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(baseStart, baseStart - msPerDay, emptySet())
        assertTrue(plan.isEmpty())
    }

    // ── Day-count math ──────────────────────────────────────────────────────

    @Test
    fun `same-day trip yields exactly one day`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(baseStart, baseStart, emptySet())
        assertEquals(1, plan.size)
        assertEquals("day_1", plan[0].dayId)
        assertEquals(1, plan[0].dayNumber)
        assertEquals(baseStart, plan[0].dateMs)
    }

    @Test
    fun `three-day trip yields three sequentially numbered days`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(
            startDateMs    = baseStart,
            endDateMs      = baseStart + 2 * msPerDay,
            existingDayIds = emptySet(),
        )
        assertEquals(3, plan.size)
        assertEquals(listOf("day_1", "day_2", "day_3"), plan.map { it.dayId })
        assertEquals(listOf(1, 2, 3), plan.map { it.dayNumber })
        assertEquals(
            listOf(baseStart, baseStart + msPerDay, baseStart + 2 * msPerDay),
            plan.map { it.dateMs },
        )
    }

    @Test
    fun `day count is capped at MAX_DAY_COUNT to defend against bad inputs`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(
            startDateMs    = baseStart,
            endDateMs      = baseStart + 1000 * msPerDay,   // ~2.7 years — clearly bogus
            existingDayIds = emptySet(),
        )
        assertEquals(ItineraryBusinessRules.MAX_DAY_COUNT.toInt(), plan.size)
    }

    // ── Idempotency (gap filling) ───────────────────────────────────────────

    @Test
    fun `already-existing day ids are skipped`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(
            startDateMs    = baseStart,
            endDateMs      = baseStart + 4 * msPerDay,
            existingDayIds = setOf("day_2", "day_4"),
        )
        assertEquals(listOf("day_1", "day_3", "day_5"), plan.map { it.dayId })
        assertEquals(listOf(1, 3, 5), plan.map { it.dayNumber })
    }

    @Test
    fun `full overlap with existing ids yields empty plan`() {
        val plan = ItineraryBusinessRules.planDaysToCreate(
            startDateMs    = baseStart,
            endDateMs      = baseStart + 2 * msPerDay,
            existingDayIds = setOf("day_1", "day_2", "day_3"),
        )
        assertTrue(plan.isEmpty())
    }
}
