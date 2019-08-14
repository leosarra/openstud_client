package com.lithium.leona.openstud.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.widget.Toolbar;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WebViewActivity extends BaseDataActivity {

    @BindView(R.id.webview)
    WebView webView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    WebViewClient client;
    private boolean javascriptInjected = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!initData()) return;
        setContentView(R.layout.activity_web_view);
        ButterKnife.bind(this);
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_arrow_back);
        WebView.setWebContentsDebuggingEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        setupWebView(getIntent());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(Intent intent) {
        Bundle bdl = intent.getExtras();
        String title = bdl.getString("title");
        String subtitle = bdl.getString("subtitle", null);
        String url = bdl.getString("url");
        int type = bdl.getInt("webviewType");
        setTitle(title);
        if (subtitle != null) toolbar.setSubtitle(subtitle);
        client = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                inject(view, url, type);
            }

        };

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.setWebViewClient(client);
        webView.loadUrl(url);


    }

    private void inject(WebView view, String url, int type) {
        if (type == ClientHelper.WebViewType.EMAIL.getValue()) {
            if (url.startsWith("https://login.studenti.uniroma1.it") && !url.contains("logout")) {
                view.setVisibility(View.GONE);
                if (!javascriptInjected) view.loadUrl(
                        "javascript:(function() { " +
                                "setTimeout(function(){" +
                                "var studentid = document.getElementById('username');"
                                + "var password = document.getElementById('password');"
                                + "var login = document.getElementsByName('samlButton');"
                                + "if (password == undefined || studentid == undefined || login == undefined || login.length == 0) return;"
                                + "studentid.value = '" + student.getStudentID() + "';"
                                + "password.value = '" + os.getStudentPassword() + "';"
                                + "login[0].click();" +
                                "}, 500)})()");
                javascriptInjected = true;
            }
            else if (url.contains("logout")) {
                view.setVisibility(View.GONE);
                onBackPressed();
            }
            else view.setVisibility(View.VISIBLE);
        }
        else {
            throw new IllegalArgumentException("WebView type not supported");
        }
    }
}
