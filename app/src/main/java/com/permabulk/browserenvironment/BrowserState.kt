package com.permabulk.browserenvironment

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import java.net.URLEncoder

/** Tiny bit of persisted state: last page and desktop/mobile preference. */
object BrowserState {
  const val HOME_URL = "file:///android_asset/home.html"

  const val DESKTOP_UA =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
          "Chrome/128.0.0.0 Safari/537.36"

  private lateinit var prefs: SharedPreferences

  fun init(context: Context) {
    prefs = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
  }

  var lastUrl: String
    get() = prefs.getString("lastUrl", HOME_URL) ?: HOME_URL
    set(v) = prefs.edit().putString("lastUrl", v).apply()

  var desktopMode: Boolean
    get() = prefs.getBoolean("desktop", true)
    set(v) = prefs.edit().putBoolean("desktop", v).apply()

  /** Turns whatever was typed into the address bar into a URL (or a Google search). */
  fun toUrl(input: String): String {
    val q = input.trim()
    if (q.isEmpty()) return HOME_URL
    if (q.startsWith("http://") || q.startsWith("https://") || q.startsWith("file://")) return q
    if (!q.contains(' ') && Patterns.WEB_URL.matcher(q).matches()) return "https://$q"
    return "https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8")
  }
}
