package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("One STI", appName)
  }

  @Test
  fun `verify no internet strings are localized and defined`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val noInternetTitle = context.getString(R.string.no_internet_title)
    val retryText = context.getString(R.string.retry_connection)
    val networkSettings = context.getString(R.string.network_settings)
    val viewCached = context.getString(R.string.view_cached_portal)
    val offlineBanner = context.getString(R.string.offline_banner)

    assertEquals("No Internet Connection", noInternetTitle)
    assertEquals("Retry Connection", retryText)
    assertEquals("Network Settings", networkSettings)
    assertEquals("View Cached Content", viewCached)
    assertEquals("Offline Mode • Displaying cached content", offlineBanner)
  }

  @Test
  fun `verify biometric security strings are defined`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val promptTitle = context.getString(R.string.biometric_prompt_title)
    val unlockButton = context.getString(R.string.biometric_unlock_button)
    val settingsTitle = context.getString(R.string.biometric_settings_title)

    assertEquals("One STI Security", promptTitle)
    assertEquals("Unlock Portal", unlockButton)
    assertEquals("Biometric Protection", settingsTitle)
  }
}
