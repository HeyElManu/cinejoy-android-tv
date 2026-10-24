package com.cinejoy.tv

import android.net.Uri

object AdBlocker {

    // Common ad, tracking, and popup domains frequently encountered on streaming platforms
    private val BLOCKED_HOST_PATTERNS = listOf(
        "popads",
        "popcash",
        "onclickpredictiv",
        "adcash",
        "doubleclick",
        "google-analytics",
        "googlesyndication",
        "adnxs",
        "propellerads",
        "histats",
        "monetag",
        "outbrain",
        "taboola",
        "bet365",
        "1xbet",
        "casino",
        "track",
        "banner",
        "juicyads",
        "exoclick",
        "trafficjunky",
        "adsterra",
        "coinhive",
        "cryptoloot"
    )

    // Allowed domains and streaming embed hosts
    private val ALLOWED_DOMAINS = listOf(
        "cinejoy.pk",
        "cinejoy.to",
        "cinejoy.lol",
        "cinejoy.asia",
        "cinejoy.ink",
        "vidsrc",
        "2embed",
        "autoembed",
        "superembed",
        "rabbitstream",
        "megacloud",
        "streamwish",
        "filemoon",
        "vidcloud",
        "tmdb.org",
        "themoviedb.org",
        "cloudflare.com",
        "jsdelivr.net",
        "unpkg.com"
    )

    fun shouldBlockUrl(url: String?): Boolean {
        if (url == null || url.isBlank()) return true

        if (url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("about:blank")) {
            return false
        }

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return true
        }

        val host = uri.host?.lowercase() ?: return true

        val isAllowed = ALLOWED_DOMAINS.any { allowed -> host.contains(allowed) }
        if (isAllowed) {
            val isKnownAd = BLOCKED_HOST_PATTERNS.any { blocked -> host.contains(blocked) }
            return isKnownAd
        }

        for (pattern in BLOCKED_HOST_PATTERNS) {
            if (host.contains(pattern)) {
                return true
            }
        }

        val spamTlds = listOf(".xyz", ".top", ".click", ".link", ".buzz", ".live", ".icu", ".cam")
        if (spamTlds.any { host.endsWith(it) }) {
            return true
        }

        return false
    }

    /**
     * Enhanced CSS & JS injection:
     * 1. Eliminates overlapping fixed headers, floating notices, and intrusive banners.
     * 2. Cleans the video player container so controls do not overlap.
     * 3. Blocks popup windows.
     */
    val TV_INJECTED_CSS_JS: String = """
        (function() {
            try {
                var style = document.getElementById('cinejoy-tv-custom-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'cinejoy-tv-custom-style';
                    style.innerHTML = `
                        /* 1. Prevent sticky headers and floating bars from overlapping the player */
                        header, .header, nav, .navbar, .sticky-top, .fixed-top {
                            position: relative !important;
                            z-index: 10 !important;
                        }

                        /* 2. Hide all ads, overlay trackers, and intrusive floating boxes */
                        iframe[id*="ad"], iframe[src*="ad"], iframe[src*="banner"],
                        .ad-container, .advertisement, .ad-banner, .banner-ad,
                        .alert, .notice-bar, .announcement,
                        div[class*="popup"]:not([class*="modal"]):not([class*="player"]),
                        div[id*="overlay"]:not([id*="player"]):not([class*="video"]),
                        div[style*="z-index: 2147483647"]:not(.jwplayer):not(.video-js),
                        div[style*="z-index: 999999"]:not(.jwplayer):not(.video-js),
                        #pop-ad, .floating-banner, #cookie-law-info-bar, .cookie-banner {
                            display: none !important;
                            visibility: hidden !important;
                            pointer-events: none !important;
                            height: 0 !important;
                            width: 0 !important;
                        }

                        /* 3. Optimize player area so controls are clean and unobstructed */
                        .player-wrap, .player-container, #player, .play-box, .video-player {
                            position: relative !important;
                            z-index: 50 !important;
                            max-width: 100% !important;
                            margin: 0 auto !important;
                        }

                        /* TV dark theme & smooth navigation */
                        html, body {
                            background-color: #0c0c0c !important;
                            color: #f0f0f0 !important;
                            user-select: none !important;
                            -webkit-user-select: none !important;
                        }

                        /* Focus outline for D-Pad navigation */
                        a:focus, button:focus, [tabindex]:focus {
                            outline: 3px solid #E50914 !important;
                            outline-offset: 2px !important;
                        }
                    `;
                    document.head.appendChild(style);
                }

                // 4. Block window.open spam
                window.open = function(url) {
                    console.log('Blocked popup:', url);
                    return null;
                };

                // 5. Remove blank target click redirects
                document.querySelectorAll('a[target="_blank"]').forEach(function(el) {
                    el.removeAttribute('target');
                });
            } catch(e) {
                console.error('Cinejoy TV script error', e);
            }
        })();
    """.trimIndent()
}
