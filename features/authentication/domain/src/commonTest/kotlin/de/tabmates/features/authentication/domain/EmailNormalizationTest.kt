package de.tabmates.features.authentication.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class EmailNormalizationTest {
    @Test
    fun `lower cases the whole address`() {
        assertEquals("max.mustermann@web.de", "Max.Mustermann@Web.DE".normalizeEmail())
    }

    @Test
    fun `strips surrounding whitespace`() {
        assertEquals("test@example.com", "  test@example.com\n".normalizeEmail())
    }

    @Test
    fun `trims and lower cases together`() {
        assertEquals("max.mustermann@web.de", " Max.Mustermann@Web.DE ".normalizeEmail())
    }

    @Test
    fun `leaves an already normalized address untouched`() {
        assertEquals("test@example.com", "test@example.com".normalizeEmail())
    }

    @Test
    fun `keeps a blank address empty so callers can still detect it`() {
        assertEquals("", "   ".normalizeEmail())
    }

    @Test
    fun `normalized addresses pass validation`() {
        assertEquals(true, EmailValidator.validate(" Max.Mustermann@Web.DE ".normalizeEmail()))
    }
}
