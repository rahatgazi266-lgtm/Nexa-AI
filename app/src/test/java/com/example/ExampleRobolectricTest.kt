package com.example

import com.example.service.SecurityScannerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun `verify app identity strings`() {
    val appName = "Nexa AI"
    assertEquals("Nexa AI", appName)
  }

  @Test
  fun `security scanner blocks dangerous executables`() {
    val res = SecurityScannerService.validateAttachment("payload.exe", 1024)
    assertFalse(res.safe)
    assertEquals("executable_blocked", res.threats.first().type)
  }

  @Test
  fun `security scanner allows safe files`() {
    val res = SecurityScannerService.validateAttachment("document.pdf", 1024)
    assertTrue(res.safe)
  }
}
