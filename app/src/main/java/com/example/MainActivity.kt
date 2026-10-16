package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.ByteArrayInputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StiBlue
import kotlinx.coroutines.delay

private const val ONE_STI_URL = "https://one.sti.edu/"
private const val PREFS_NAME = "one_sti_state"
private const val KEY_LAST_URL = "last_url"
private const val KEY_SCROLL_Y = "scroll_y"

// Helper to check if device has active internet connectivity
private fun isNetworkAvailable(context: Context): Boolean {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
  val network = cm.activeNetwork ?: return false
  val capabilities = cm.getNetworkCapabilities(network) ?: return false
  return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

// Helper to detect any logout, sign-out, or session-termination URLs
private fun isLogoutUrl(url: String?): Boolean {
  if (url.isNullOrBlank()) return false
  val lower = url.lowercase()
  return lower.contains("logout") ||
         lower.contains("signout") ||
         lower.contains("sign-out") ||
         lower.contains("log-out") ||
         lower.contains("logoff") ||
         lower.contains("log-off") ||
         lower.contains("endsession") ||
         lower.contains("wa=wsignout") ||
         lower.contains("disconnect") ||
         lower.contains("clearcookies") ||
         (lower.contains("login.microsoftonline.com") && lower.contains("/logout"))
}

// Helper to only save genuine portal pages, not temporary OAuth handshake URLs or logout pages
private fun isSavablePortalUrl(url: String?): Boolean {
  if (url.isNullOrBlank() || !url.startsWith("http")) return false
  if (isLogoutUrl(url)) return false
  val lower = url.lowercase()
  if (lower.contains("login.microsoftonline.com") ||
      lower.contains("login.live.com") ||
      lower.contains("login.microsoft.com") ||
      lower.contains("oauth2") ||
      lower.contains("signin") ||
      lower.contains("authorize") ||
      lower.contains("saml") ||
      lower.contains("processauth")) {
    return false
  }
  return lower.contains("one.sti.edu")
}

// Convert session cookies into permanent cookies with a 1-year max age so accounts never log out
private fun persistAndProtectCookies(url: String) {
  try {
    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(url) ?: return
    val uri = Uri.parse(url)
    val host = uri.host ?: return
    val isHttps = url.startsWith("https", ignoreCase = true)
    val secureFlag = if (isHttps) "; Secure" else ""
    val parts = cookies.split(";")
    for (part in parts) {
      val trimmed = part.trim()
      if (trimmed.isNotEmpty()) {
        val eqIdx = trimmed.indexOf('=')
        if (eqIdx > 0) {
          val name = trimmed.substring(0, eqIdx).trim()
          val value = trimmed.substring(eqIdx + 1).trim()
          cookieManager.setCookie(
            url,
            "$name=$value; Max-Age=31536000; Path=/; Domain=$host$secureFlag; SameSite=Lax"
          )
        }
      }
    }
    cookieManager.flush()
  } catch (_: Exception) {}
}

// Client-side script to prevent logout, maintain server session heartbeat, and reset idle timers
private val LOGOUT_PREVENTION_JS = """
(function() {
  if (window.__oneStiLogoutProtectionActive) return;
  window.__oneStiLogoutProtectionActive = true;

  // 1. Keep-alive heartbeat: ping current origin every 90 seconds to maintain active server session
  try {
    setInterval(function() {
      try {
        var pingUrl = window.location.origin + window.location.pathname;
        fetch(pingUrl, {
          method: 'HEAD',
          cache: 'no-store',
          credentials: 'include'
        }).catch(function() {});
      } catch(e) {}
      // Emit synthetic interactions to reset client-side idle timers
      try {
        var evt = new MouseEvent('mousemove', { bubbles: true, cancelable: true });
        document.dispatchEvent(evt);
        window.dispatchEvent(new Event('scroll'));
      } catch(e) {}
    }, 90000);
  } catch(e) {}

  // 2. Intercept window.location.assign and window.location.replace to logout URLs
  try {
    var origAssign = window.location.assign;
    window.location.assign = function(url) {
      if (typeof url === 'string') {
        var low = url.toLowerCase();
        if (low.indexOf('logout') !== -1 || low.indexOf('signout') !== -1 || low.indexOf('logoff') !== -1 || low.indexOf('endsession') !== -1) {
          console.log('[ONE STI] Blocked location.assign to logout URL: ' + url);
          return;
        }
      }
      return origAssign.apply(window.location, arguments);
    };

    var origReplace = window.location.replace;
    window.location.replace = function(url) {
      if (typeof url === 'string') {
        var low = url.toLowerCase();
        if (low.indexOf('logout') !== -1 || low.indexOf('signout') !== -1 || low.indexOf('logoff') !== -1 || low.indexOf('endsession') !== -1) {
          console.log('[ONE STI] Blocked location.replace to logout URL: ' + url);
          return;
        }
      }
      return origReplace.apply(window.location, arguments);
    };
  } catch(e) {}

  // 3. Intercept DOM clicks on logout/sign-out buttons or links
  document.addEventListener('click', function(e) {
    try {
      var el = e.target;
      while (el && el !== document.body && el !== document.documentElement) {
        var text = (el.innerText || el.textContent || '').trim().toLowerCase();
        var href = (el.getAttribute('href') || '').toLowerCase();
        var id = (el.id || '').toLowerCase();
        var onclickAttr = (el.getAttribute('onclick') || '').toLowerCase();

        var isLogout = href.indexOf('logout') !== -1 || href.indexOf('signout') !== -1 || href.indexOf('logoff') !== -1 || href.indexOf('endsession') !== -1 ||
                       text === 'log out' || text === 'logout' || text === 'sign out' || text === 'signout' || text === 'sign-out' || text === 'log-out' ||
                       onclickAttr.indexOf('logout') !== -1 || onclickAttr.indexOf('signout') !== -1 ||
                       id.indexOf('logout') !== -1 || id.indexOf('signout') !== -1;

        if (isLogout) {
          e.preventDefault();
          e.stopPropagation();
          e.stopImmediatePropagation();
          console.log('[ONE STI] Blocked logout click action');
          return false;
        }
        el = el.parentElement;
      }
    } catch(err) {}
  }, true);

  // 4. Neutralize idle timeout warnings / auto-logout modals if present
  try {
    if (typeof window.sessionTimeout !== 'undefined') window.sessionTimeout = 999999999;
    if (typeof window.idleTime !== 'undefined') window.idleTime = 0;
    if (typeof window.resetTimer === 'function') window.resetTimer();
  } catch(e) {}

  // 5. Automatically confirm "Stay Signed In" / "Continue Session" prompts
  try {
    setInterval(function() {
      try {
        var buttons = document.querySelectorAll('button, a, input[type="button"], input[type="submit"]');
        for (var i = 0; i < buttons.length; i++) {
          var val = (buttons[i].innerText || buttons[i].value || '').toLowerCase();
          if (val.indexOf('stay logged in') !== -1 || val.indexOf('keep me logged in') !== -1 || val.indexOf('continue session') !== -1 || val.indexOf('extend session') !== -1) {
            buttons[i].click();
          }
        }
      } catch(e) {}
    }, 5000);
  } catch(e) {}
})();
""".trimIndent()

class MainActivity : ComponentActivity() {
  private var activeWebView: WebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    applyHighRefreshRate()

    // Ensure native CookieManager accepts third-party authentication cookies
    try {
      val cookieManager = CookieManager.getInstance()
      cookieManager.setAcceptCookie(true)
    } catch (_: Exception) {}

    // Check initial network state to load cache if offline
    val online = isNetworkAvailable(this)
    val prewarmedWebView = createConfiguredWebView(this).apply {
      if (!online) {
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
      }
      loadUrl(ONE_STI_URL)
    }
    activeWebView = prewarmedWebView

    setContent {
      MyApplicationTheme {
        OneStiApp(
          existingWebView = prewarmedWebView,
          onWebViewBound = { activeWebView = it }
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    applyHighRefreshRate()
    activeWebView?.onResume()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    applyHighRefreshRate()
  }

  // Request display high refresh rate (144Hz / 120Hz) if supported by device hardware
  private fun applyHighRefreshRate() {
    try {
      val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                       Build.FINGERPRINT.startsWith("unknown") ||
                       Build.MODEL.contains("google_sdk") ||
                       Build.MODEL.contains("Emulator") ||
                       Build.HARDWARE.contains("goldfish") ||
                       Build.HARDWARE.contains("ranchu") ||
                       !java.io.File("/dev/dri").exists()
      if (isEmulator) return

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val modes = display?.supportedModes ?: emptyArray()
        val targetMode = modes.filter { it.refreshRate >= 143f }.maxByOrNull { it.refreshRate }
          ?: modes.maxByOrNull { it.refreshRate }
        if (targetMode != null && targetMode.refreshRate > 60f) {
          val params = window.attributes
          params.preferredDisplayModeId = targetMode.modeId
          window.attributes = params
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        @Suppress("DEPRECATION")
        val modes = windowManager.defaultDisplay.supportedModes
        val targetMode = modes.filter { it.refreshRate >= 143f }.maxByOrNull { it.refreshRate }
          ?: modes.maxByOrNull { it.refreshRate }
        if (targetMode != null && targetMode.refreshRate > 60f) {
          val params = window.attributes
          params.preferredDisplayModeId = targetMode.modeId
          window.attributes = params
        }
      }
    } catch (_: Exception) {}
  }

  override fun onPause() {
    activeWebView?.let { wv ->
      wv.onPause()
      val currentUrl = wv.url
      if (isSavablePortalUrl(currentUrl)) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          .edit()
          .putString(KEY_LAST_URL, currentUrl)
          .putInt(KEY_SCROLL_Y, wv.scrollY)
          .apply()
      }
    }
    persistAndProtectCookies(ONE_STI_URL)
    persistAndProtectCookies("https://login.microsoftonline.com/")
    try {
      CookieManager.getInstance().flush()
    } catch (_: Exception) {}
    super.onPause()
  }

  override fun onStop() {
    persistAndProtectCookies(ONE_STI_URL)
    persistAndProtectCookies("https://login.microsoftonline.com/")
    try {
      CookieManager.getInstance().flush()
    } catch (_: Exception) {}
    super.onStop()
  }

  override fun onDestroy() {
    activeWebView?.let { wv ->
      (wv.parent as? ViewGroup)?.removeView(wv)
      wv.destroy()
    }
    activeWebView = null
    super.onDestroy()
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      val wv = activeWebView
      if (wv != null && wv.canGoBack()) {
        wv.goBack()
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }
}

@Composable
fun OneStiApp(
  existingWebView: WebView? = null,
  onWebViewBound: (WebView) -> Unit = {}
) {
  val context = LocalContext.current
  var webViewInstance by remember { mutableStateOf<WebView?>(existingWebView) }
  var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }
  var isOfflineMode by remember { mutableStateOf(!isOnline) }
  var hasError by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }

  // Monitor real-time network connectivity changes
  DisposableEffect(context) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        (context as? Activity)?.runOnUiThread {
          isOnline = true
          if (hasError) {
            hasError = false
            webViewInstance?.settings?.cacheMode = WebSettings.LOAD_DEFAULT
            webViewInstance?.reload()
          }
        }
      }

      override fun onLost(network: Network) {
        (context as? Activity)?.runOnUiThread {
          isOnline = false
        }
      }
    }
    try {
      cm?.registerNetworkCallback(request, callback)
    } catch (_: Exception) {}

    onDispose {
      try {
        cm?.unregisterNetworkCallback(callback)
      } catch (_: Exception) {}
    }
  }

  // Periodic cookie protection & keep-alive every 30s to permanently preserve authentication
  LaunchedEffect(Unit) {
    while (true) {
      delay(30_000L)
      try {
        persistAndProtectCookies(ONE_STI_URL)
        persistAndProtectCookies("https://login.microsoftonline.com/")
        CookieManager.getInstance().flush()
        webViewInstance?.evaluateJavascript(LOGOUT_PREVENTION_JS, null)
      } catch (_: Exception) {}
    }
  }

  // System back button: navigates backwards in ONE STI portal history; exits on home
  BackHandler(enabled = true) {
    val wv = webViewInstance
    when {
      hasError -> {
        (context as? Activity)?.finish()
      }
      isOfflineMode && wv != null && wv.canGoBack() -> {
        wv.goBack()
      }
      wv != null && wv.canGoBack() -> {
        wv.goBack()
      }
      else -> {
        (context as? Activity)?.finish()
      }
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Offline Mode Banner when browsing cached content
        if (isOfflineMode) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            shadowElevation = 2.dp
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_cloud_off),
                  contentDescription = null,
                  tint = Color(0xFFB45309),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = stringResource(R.string.offline_mode_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                  )
                  Text(
                    text = stringResource(R.string.offline_banner),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB45309)
                  )
                }
              }
              TextButton(
                onClick = {
                  if (isNetworkAvailable(context)) {
                    isOfflineMode = false
                    hasError = false
                    webViewInstance?.settings?.cacheMode = WebSettings.LOAD_DEFAULT
                    webViewInstance?.reload()
                    Toast.makeText(context, context.getString(R.string.connection_restored), Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "No active internet connection yet", Toast.LENGTH_SHORT).show()
                  }
                }
              ) {
                Text(
                  text = stringResource(R.string.go_online),
                  fontWeight = FontWeight.Bold,
                  color = StiBlue
                )
              }
            }
          }
        }

        OneStiWebViewContainer(
          existingWebView = webViewInstance ?: existingWebView,
          onWebViewCreated = { wv ->
            webViewInstance = wv
            onWebViewBound(wv)
          },
          onPageFinished = {
            hasError = false
          },
          onError = { err ->
            if (!isOfflineMode) {
              hasError = true
              errorMessage = err
            }
          },
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .testTag("one_sti_webview")
        )
      }

      // No Internet Connection UI Screen
      if (hasError && !isOfflineMode) {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .testTag("no_internet_screen"),
          color = MaterialTheme.colorScheme.background
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(StiBlue.copy(alpha = 0.10f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_wifi_off),
                contentDescription = stringResource(R.string.no_internet_title),
                tint = StiBlue,
                modifier = Modifier.size(48.dp)
              )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
              text = stringResource(R.string.no_internet_title),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = StiBlue,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = if (errorMessage.isNotBlank() && errorMessage.contains("Unable")) errorMessage else stringResource(R.string.no_internet_desc),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action 1: Retry Connection
            Button(
              onClick = {
                if (isNetworkAvailable(context)) {
                  hasError = false
                  isOfflineMode = false
                  webViewInstance?.settings?.cacheMode = WebSettings.LOAD_DEFAULT
                  webViewInstance?.reload()
                } else {
                  hasError = true
                  Toast.makeText(context, "No connection detected. Please check network.", Toast.LENGTH_SHORT).show()
                  webViewInstance?.reload()
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = StiBlue,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("error_retry_button")
            ) {
              Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
              Spacer(modifier = Modifier.size(8.dp))
              Text(
                text = stringResource(R.string.retry_connection),
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 2: View Cached Content (Offline Mode)
            OutlinedButton(
              onClick = {
                isOfflineMode = true
                hasError = false
                webViewInstance?.apply {
                  settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                  val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                  val lastUrl = prefs.getString(KEY_LAST_URL, null)
                  loadUrl(if (isSavablePortalUrl(lastUrl)) lastUrl!! else ONE_STI_URL)
                }
                Toast.makeText(context, context.getString(R.string.offline_banner), Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = StiBlue
              ),
              border = BorderStroke(1.5.dp, StiBlue),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("view_cached_button")
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_cloud_off),
                contentDescription = null,
                tint = StiBlue,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.size(8.dp))
              Text(
                text = stringResource(R.string.view_cached_portal),
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 3: Open Network Settings
            TextButton(
              onClick = {
                try {
                  context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } catch (_: Exception) {
                  try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                  } catch (_: Exception) {}
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("network_settings_button")
            ) {
              Text(
                text = stringResource(R.string.network_settings),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
fun createConfiguredWebView(
  context: Context,
  onPageFinished: () -> Unit = {},
  onError: (String) -> Unit = {}
): WebView {
  val webView = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )

    setBackgroundColor(android.graphics.Color.parseColor("#005596"))
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    overScrollMode = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS
    isNestedScrollingEnabled = true

    val isHeadlessOrEmulator = !java.io.File("/dev/dri").exists() ||
                               Build.FINGERPRINT.startsWith("generic") ||
                               Build.FINGERPRINT.startsWith("unknown") ||
                               Build.MODEL.contains("google_sdk") ||
                               Build.MODEL.contains("Emulator") ||
                               Build.HARDWARE.contains("goldfish") ||
                               Build.HARDWARE.contains("ranchu")
    if (isHeadlessOrEmulator) {
      setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
    } else {
      setLayerType(android.view.View.LAYER_TYPE_NONE, null)
    }

    // Configure CookieManager for native persistent login & Microsoft SSO
    try {
      val cookieManager = CookieManager.getInstance()
      cookieManager.setAcceptCookie(true)
      cookieManager.setAcceptThirdPartyCookies(this, true)
    } catch (_: Exception) {}

    // Instant web settings: offscreen pre-raster, fast cache, and script enabled
    settings.apply {
      javaScriptEnabled = true
      javaScriptCanOpenWindowsAutomatically = true
      setSupportMultipleWindows(true)
      domStorageEnabled = true
      databaseEnabled = true
      saveFormData = true
      cacheMode = WebSettings.LOAD_DEFAULT
      allowFileAccess = false
      allowContentAccess = true
      setSupportZoom(true)
      builtInZoomControls = true
      displayZoomControls = false
      useWideViewPort = true
      loadWithOverviewMode = true
      mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
      offscreenPreRaster = true
      loadsImagesAutomatically = true
      blockNetworkImage = false

      val defaultUa = userAgentString
      if (defaultUa.contains("; wv")) {
        userAgentString = defaultUa.replace("; wv", "")
      }
    }

    webChromeClient = object : WebChromeClient() {
      override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
      ): Boolean {
        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
        transport.webView = view
        resultMsg.sendToTarget()
        return true
      }

      override fun onPermissionRequest(request: PermissionRequest?) {
        request?.deny()
      }
    }

    webViewClient = object : WebViewClient() {
      override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        view?.evaluateJavascript(LOGOUT_PREVENTION_JS, null)
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished()
        view?.evaluateJavascript(LOGOUT_PREVENTION_JS, null)

        // Convert and protect cookies so session never expires or clears
        url?.let {
          persistAndProtectCookies(it)
          if (it.contains("sti.edu")) {
            persistAndProtectCookies(ONE_STI_URL)
          }
        }

        if (isSavablePortalUrl(url)) {
          context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_URL, url)
            .apply()
        }

        try {
          CookieManager.getInstance().flush()
        } catch (_: Exception) {}
      }

      override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        view?.evaluateJavascript(LOGOUT_PREVENTION_JS, null)
        if (isSavablePortalUrl(url)) {
          context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_URL, url)
            .apply()
        }
      }

      override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val urlString = request?.url?.toString() ?: ""
        if (isLogoutUrl(urlString)) {
          return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }
        return super.shouldInterceptRequest(view, request)
      }

      override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val targetUri = request?.url ?: return false
        val urlString = targetUri.toString()
        val scheme = targetUri.scheme?.lowercase() ?: ""

        // Never log out: block any navigation to logout / signout endpoints
        if (isLogoutUrl(urlString)) {
          Toast.makeText(context, "Account logout is disabled to keep you logged in permanently", Toast.LENGTH_SHORT).show()
          return true
        }

        if (urlString.startsWith("intent://")) {
          return try {
            val intent = Intent.parseUri(urlString, Intent.URI_INTENT_SCHEME)
            if (intent != null) {
              val fallbackUrl = intent.getStringExtra("browser_fallback_url")
              val packageManager = context.packageManager
              val info = packageManager.resolveActivity(intent, 0)
              if (info != null) {
                context.startActivity(intent)
                true
              } else if (!fallbackUrl.isNullOrBlank()) {
                view?.loadUrl(fallbackUrl)
                true
              } else false
            } else false
          } catch (_: Exception) {
            true
          }
        }

        if (scheme != "http" && scheme != "https") {
          return try {
            val intent = Intent(Intent.ACTION_VIEW, targetUri)
            context.startActivity(intent)
            true
          } catch (_: Exception) {
            true
          }
        }

        return false
      }

      override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
      ) {
        handler?.proceed()
      }

      override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
      ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
          val errorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode ?: 0 else 0
          val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description?.toString() ?: "" else ""

          if (errorCode == WebViewClient.ERROR_UNKNOWN ||
              desc.contains("ERR_ABORTED") ||
              desc.contains("ERR_CACHE_MISS") ||
              desc.contains("net::ERR_ABORTED")) {
            return
          }

          if (errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
              errorCode == WebViewClient.ERROR_CONNECT ||
              errorCode == WebViewClient.ERROR_TIMEOUT) {
            onError(if (desc.isNotBlank()) desc else "Unable to connect to ONE STI")
          }
        }
      }

      override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
      ): Boolean {
        if (view != null) {
          (view.parent as? ViewGroup)?.removeView(view)
          view.destroy()
        }
        onError("Web renderer was restarted. Tap Try Again to reload.")
        return true
      }
    }

    setOnKeyListener { _, keyCode, event ->
      if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
        if (canGoBack()) {
          goBack()
          return@setOnKeyListener true
        }
      }
      false
    }
  }
  return webView
}

@Composable
fun OneStiWebViewContainer(
  existingWebView: WebView?,
  onWebViewCreated: (WebView) -> Unit,
  onPageFinished: () -> Unit,
  onError: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val webViewRef = remember { java.util.concurrent.atomic.AtomicReference<WebView?>(existingWebView) }

  DisposableEffect(Unit) {
    onDispose {
      try {
        persistAndProtectCookies(ONE_STI_URL)
        persistAndProtectCookies("https://login.microsoftonline.com/")
        CookieManager.getInstance().flush()
        val wv = webViewRef.getAndSet(null)
        (wv?.parent as? ViewGroup)?.removeView(wv)
        wv?.destroy()
      } catch (_: Exception) {}
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      val webView = existingWebView ?: createConfiguredWebView(
        context = ctx,
        onPageFinished = onPageFinished,
        onError = onError
      ).apply {
        loadUrl(ONE_STI_URL)
      }

      (webView.parent as? ViewGroup)?.removeView(webView)
      webViewRef.set(webView)
      onWebViewCreated(webView)

      val swipeRefresh = SwipeRefreshLayout(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
        setColorSchemeColors(
          android.graphics.Color.parseColor("#005596"),
          android.graphics.Color.parseColor("#FFD100")
        )
        setProgressBackgroundColorSchemeColor(android.graphics.Color.WHITE)

        setOnChildScrollUpCallback { _, _ ->
          webView.canScrollVertically(-1)
        }

        setOnRefreshListener {
          isRefreshing = false
          webView.reload()
        }

        addView(webView)
      }

      swipeRefresh
    },
    update = { swipeRefresh ->
      swipeRefresh.isRefreshing = false
    }
  )
}
