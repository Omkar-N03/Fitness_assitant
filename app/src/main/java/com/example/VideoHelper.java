package com.example;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.databinding.DialogVideoPlayerBinding;

/**
 * Helper utility for parsing YouTube video links, playing videos directly
 * inside the app via an embedded WebView player, and gracefully launching external
 * YouTube/browser intents with automatic fallback.
 */
public final class VideoHelper {

    private VideoHelper() {}

    /**
     * Extracts YouTube 11-character video ID from various link formats.
     */
    @Nullable
    public static String extractVideoId(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        String trimmed = url.trim();
        try {
            if (trimmed.contains("v=")) {
                int vIndex = trimmed.indexOf("v=");
                String id = trimmed.substring(vIndex + 2);
                int ampIndex = id.indexOf('&');
                if (ampIndex != -1) {
                    id = id.substring(0, ampIndex);
                }
                return id;
            } else if (trimmed.contains("youtu.be/")) {
                int slashIndex = trimmed.indexOf("youtu.be/");
                String id = trimmed.substring(slashIndex + "youtu.be/".length());
                int qIndex = id.indexOf('?');
                if (qIndex != -1) {
                    id = id.substring(0, qIndex);
                }
                return id;
            } else if (trimmed.contains("embed/")) {
                int embedIndex = trimmed.indexOf("embed/");
                String id = trimmed.substring(embedIndex + "embed/".length());
                int qIndex = id.indexOf('?');
                if (qIndex != -1) {
                    id = id.substring(0, qIndex);
                }
                return id;
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Generates responsive HTML embedding the YouTube iframe player.
     */
    public static String getEmbedHtml(@NonNull String videoId) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "  <style>\n" +
                "    * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000; }\n" +
                "    html, body { width: 100%; height: 100%; overflow: hidden; background: #000; }\n" +
                "    .container { position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }\n" +
                "    iframe { width: 100%; height: 100%; border: 0; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <iframe src=\"https://www.youtube-nocookie.com/embed/" + videoId + "?autoplay=1&playsinline=1&rel=0&modestbranding=1\"\n" +
                "            frameborder=\"0\"\n" +
                "            allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\"\n" +
                "            allowfullscreen>\n" +
                "    </iframe>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Configures a WebView for embedded YouTube video playback.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void setupVideoWebView(@NonNull WebView webView, @Nullable ProgressBar progressBar, @NonNull String videoId) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep video navigation inside WebView iframe
                return false;
            }
        });

        String html = getEmbedHtml(videoId);
        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null);
    }

    /**
     * Shows an in-app video player dialog with full playback controls.
     */
    public static void showInAppVideoDialog(@NonNull Context context,
                                            @NonNull String videoUrl,
                                            @NonNull String title,
                                            @Nullable String channelName) {
        String videoId = extractVideoId(videoUrl);
        if (videoId == null) {
            Toast.makeText(context, "Invalid video link format", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogVideoPlayerBinding binding = DialogVideoPlayerBinding.inflate(LayoutInflater.from(context));
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        binding.tvDialogVideoTitle.setText(title);
        if (channelName != null && !channelName.isEmpty()) {
            binding.tvDialogVideoChannel.setText("Instructor: " + channelName);
            binding.tvDialogVideoChannel.setVisibility(View.VISIBLE);
        } else {
            binding.tvDialogVideoChannel.setVisibility(View.GONE);
        }

        setupVideoWebView(binding.wvDialogVideo, binding.pbDialogVideoLoading, videoId);

        binding.ibDialogClose.setOnClickListener(v -> dialog.dismiss());
        binding.btnDialogClose.setOnClickListener(v -> dialog.dismiss());

        binding.btnDialogExternal.setOnClickListener(v -> {
            openExternalVideo(context, videoUrl, title, channelName);
        });

        dialog.setOnDismissListener(d -> {
            try {
                binding.wvDialogVideo.onPause();
                binding.wvDialogVideo.loadUrl("about:blank");
                binding.wvDialogVideo.stopLoading();
                binding.wvDialogVideo.destroy();
            } catch (Exception ignored) {}
        });

        dialog.show();
    }

    /**
     * Tries to open video via external native YouTube app or Web Browser,
     * with graceful fallback to in-app player if external apps are absent.
     */
    public static void openExternalVideo(@NonNull Context context,
                                         @NonNull String videoUrl,
                                         @NonNull String title,
                                         @Nullable String channelName) {
        String videoId = extractVideoId(videoUrl);
        boolean launched = false;

        // 1. Try native YouTube App intent first
        if (videoId != null) {
            try {
                Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
                appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(appIntent);
                launched = true;
            } catch (Exception ignored) {
                // Fallback to browser
            }
        }

        // 2. Try generic web browser
        if (!launched) {
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
                launched = true;
            } catch (Exception ignored) {
                // Fallback to in-app player
            }
        }

        // 3. If no external app/browser is available on the device, play in-app
        if (!launched) {
            Toast.makeText(context, "No external browser found — playing video directly in app", Toast.LENGTH_SHORT).show();
            showInAppVideoDialog(context, videoUrl, title, channelName);
        }
    }
}
