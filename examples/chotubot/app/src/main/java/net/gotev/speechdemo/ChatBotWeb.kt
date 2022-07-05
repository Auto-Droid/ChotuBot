package net.gotev.speechdemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity


class ChatBotWeb : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_bot_web)

        val webview = findViewById<WebView>(R.id.webview)
        webview.setWebViewClient(MyBrowser(this))
        webview.getSettings().setLoadsImagesAutomatically(true);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        //webview.loadUrl(resources.openRawResource(R.raw.page).toString())
        //webview.loadUrl("file:///android_asset/page.html");
        webview.loadUrl("http://alecta.co.in/page.html");
        Log.e("url","${webview.url}")
        //webview.loadUrl("javascript:plot();")

        /*webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (url == "file:///android_asset/page.html") {

                }
            }
        }*/

        setDesktopMode(webview,true)
    }

    private class MyBrowser(val context: Context) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.e("url","$url")
            if(url.equals("http://alecta.co.in/page.html", false)) {
                view.loadUrl(url)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(browserIntent)
            }

            return true
        }
    }


    fun setDesktopMode(webView: WebView, enabled: Boolean) {

        var newUserAgent = webView.settings.userAgentString
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);

        if (enabled) {
            try {
                val ua = webView.settings.userAgentString
                val androidOSString =
                    webView.settings.userAgentString.substring(ua.indexOf("("), ua.indexOf(")") + 1)
                newUserAgent =
                    webView.settings.userAgentString.replace(androidOSString, "(X11; Linux x86_64)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            newUserAgent = null
        }
        webView.settings.userAgentString = newUserAgent
        webView.settings.useWideViewPort = enabled
        webView.settings.loadWithOverviewMode = enabled
        webView.reload()
    }
}