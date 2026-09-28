package com.permabulk.browserenvironment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Bg = Color(0xFF0B0C1C)
private val Bar = Color(0xFF14163A)
private val Field = Color(0xFF0D0F24)
private val Accent = Color(0xFF39A7FF)
private val Accent2 = Color(0xFF9B5CFF)
private val TextCol = Color(0xFFE8EAFF)
private val Muted = Color(0xFF8E93C7)

/** Holds the live WebView plus the state the toolbar needs to show. */
private class BrowserHandle {
  var webView: WebView? = null
  var url by mutableStateOf("")
  var progress by mutableFloatStateOf(0f)
  var canGoBack by mutableStateOf(false)
  var canGoForward by mutableStateOf(false)
  var fullscreen by mutableStateOf(false)
  var desktop by mutableStateOf(BrowserState.desktopMode)

  fun refreshNav() {
    val w = webView ?: return
    canGoBack = w.canGoBack()
    canGoForward = w.canGoForward()
  }
}

@Composable
fun BrowserPanel(
    onToggleCurve: () -> Unit,
    onResize: (Float) -> Unit,
    isCurved: () -> Boolean,
) {
  val handle = remember { BrowserHandle() }
  var curvedLabel by remember { mutableStateOf(isCurved()) }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .clip(RoundedCornerShape(if (handle.fullscreen) 0.dp else 20.dp))
              .background(Bg),
  ) {
    if (!handle.fullscreen) {
      Toolbar(
          handle = handle,
          curved = curvedLabel,
          onToggleCurve = {
            onToggleCurve()
            curvedLabel = isCurved()
          },
          onResize = onResize,
      )
      Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
        if (handle.progress in 0.01f..0.99f) {
          LinearProgressIndicator(
              progress = { handle.progress },
              modifier = Modifier.fillMaxSize(),
              color = Accent,
              trackColor = Bar,
          )
        }
      }
    }
    Box(modifier = Modifier.fillMaxWidth().weight(1f)) { BrowserView(handle) }
  }
}

@Composable
private fun Toolbar(
    handle: BrowserHandle,
    curved: Boolean,
    onToggleCurve: () -> Unit,
    onResize: (Float) -> Unit,
) {
  val focus = LocalFocusManager.current
  var editing by remember { mutableStateOf(false) }
  var typed by remember { mutableStateOf("") }

  Row(
      modifier = Modifier.fillMaxWidth().height(64.dp).background(Bar).padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ToolButton("‹", enabled = handle.canGoBack) { handle.webView?.goBack() }
    ToolButton("›", enabled = handle.canGoForward) { handle.webView?.goForward() }
    ToolButton("⟳") { handle.webView?.reload() }
    ToolButton("⌂") { handle.webView?.loadUrl(BrowserState.HOME_URL) }

    // Address bar
    Box(
        modifier =
            Modifier.weight(1f)
                .fillMaxHeight()
                .padding(vertical = 10.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Field)
                .border(
                    2.dp,
                    if (editing) Brush.horizontalGradient(listOf(Accent, Accent2))
                    else SolidColor(Color(0xFF2A2F66)),
                    RoundedCornerShape(22.dp),
                )
                .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
      BasicTextField(
          value = if (editing) typed else prettyUrl(handle.url),
          onValueChange = { typed = it },
          singleLine = true,
          textStyle = TextStyle(color = TextCol, fontSize = 20.sp),
          cursorBrush = SolidColor(Accent),
          keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
          keyboardActions =
              KeyboardActions(
                  onGo = {
                    handle.webView?.loadUrl(BrowserState.toUrl(typed))
                    focus.clearFocus()
                  },
              ),
          modifier =
              Modifier.fillMaxWidth().onFocusChanged {
                if (it.isFocused && !editing) {
                  typed = if (handle.url == BrowserState.HOME_URL) "" else handle.url
                }
                editing = it.isFocused
              },
      )
    }

    ToolButton("−", wide = false) { onResize(-0.15f) }
    ToolButton("+", wide = false) { onResize(0.15f) }
    ToolButton(if (curved) "Flat" else "Curve", wide = true) { onToggleCurve() }
    ToolButton(if (handle.desktop) "Desktop" else "Mobile", wide = true) {
      handle.desktop = !handle.desktop
      BrowserState.desktopMode = handle.desktop
      handle.webView?.let { w ->
        applyUserAgent(w, handle.desktop)
        w.reload()
      }
    }
  }
}

@Composable
private fun ToolButton(
    label: String,
    enabled: Boolean = true,
    wide: Boolean = false,
    onClick: () -> Unit,
) {
  Box(
      modifier =
          Modifier.height(44.dp)
              .then(if (wide) Modifier.widthIn(min = 88.dp) else Modifier.size(44.dp))
              .clip(RoundedCornerShape(22.dp))
              .background(if (enabled) Color(0xFF22265A) else Color(0xFF181A3A))
              .clickable(enabled = enabled, onClick = onClick)
              .padding(horizontal = if (wide) 14.dp else 0.dp),
      contentAlignment = Alignment.Center,
  ) {
    Text(
        label,
        color = if (enabled) TextCol else Muted.copy(alpha = 0.5f),
        fontSize = if (label.length > 1) 17.sp else 26.sp,
        fontWeight = FontWeight.SemiBold,
    )
  }
}

private fun prettyUrl(url: String): String =
    when {
      url.isEmpty() || url == BrowserState.HOME_URL -> "Search Google or type a URL"
      else -> url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    }

private fun applyUserAgent(w: WebView, desktop: Boolean) {
  w.settings.userAgentString =
      if (desktop) BrowserState.DESKTOP_UA else WebSettings.getDefaultUserAgent(w.context)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserView(handle: BrowserHandle) {
  AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        val root = FrameLayout(ctx)
        val fullscreenHost = FrameLayout(ctx).apply { setBackgroundColor(AColor.BLACK) }
        var customView: View? = null
        var customCallback: WebChromeClient.CustomViewCallback? = null

        val web =
            WebView(ctx).apply {
              setBackgroundColor(AColor.parseColor("#070814"))
              settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false) // target=_blank opens in this same screen
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowFileAccess = true
              }
              applyUserAgent(this, handle.desktop)
              CookieManager.getInstance().setAcceptCookie(true)
              CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

              webViewClient =
                  object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                      handle.url = url
                      handle.refreshNav()
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                      handle.url = url
                      handle.refreshNav()
                      BrowserState.lastUrl = url
                      CookieManager.getInstance().flush()
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String,
                        isReload: Boolean,
                    ) {
                      // Catches in-page navigation on sites like YouTube (SPA route changes)
                      handle.url = url
                      handle.refreshNav()
                      BrowserState.lastUrl = url
                    }
                  }

              webChromeClient =
                  object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                      handle.progress = newProgress / 100f
                    }

                    // Video fullscreen button (YouTube etc): fill the whole screen panel
                    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                      if (customView != null) {
                        callback.onCustomViewHidden()
                        return
                      }
                      customView = view
                      customCallback = callback
                      fullscreenHost.addView(
                          view,
                          FrameLayout.LayoutParams(
                              ViewGroup.LayoutParams.MATCH_PARENT,
                              ViewGroup.LayoutParams.MATCH_PARENT,
                          ),
                      )
                      fullscreenHost.visibility = View.VISIBLE
                      handle.fullscreen = true
                    }

                    override fun onHideCustomView() {
                      customView?.let { fullscreenHost.removeView(it) }
                      customView = null
                      fullscreenHost.visibility = View.GONE
                      handle.fullscreen = false
                      customCallback?.onCustomViewHidden()
                      customCallback = null
                    }
                  }
            }

        root.addView(
            web,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        fullscreenHost.visibility = View.GONE
        root.addView(
            fullscreenHost,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        handle.webView = web
        web.loadUrl(BrowserState.lastUrl)
        root
      },
  )
}
