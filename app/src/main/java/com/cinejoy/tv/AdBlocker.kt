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

    /**
     * Determines whether a given URL should be blocked as an ad or unwanted redirect.
     */
    fun shouldBlockUrl(url: String?): Boolean {
        if (url == null || url.isBlank()) return true

        // Allow data and blob protocols for media player
        if (url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("about:blank")) {
            return false
        }

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return true
        }

        val host = uri.host?.lowercase() ?: return true

        // Check if host matches any allowed pattern
        val isAllowed = ALLOWED_DOMAINS.any { allowed -> host.contains(allowed) }
        if (isAllowed) {
            // Still check for specific ad query or path
            val isKnownAd = BLOCKED_HOST_PATTERNS.any { blocked -> host.contains(blocked) }
            return isKnownAd
        }

        // Check against blocked patterns
        for (pattern in BLOCKED_HOST_PATTERNS) {
            if (host.contains(pattern)) {
                return true
            }
        }

        // In streaming sites, random new domains opened on click are almost always ads
        // If not in allowed list and looks like typical spam TLD
        val spamTlds = listOf(".xyz", ".top", ".click", ".link", ".buzz", ".live", ".icu", ".cam")
        if (spamTlds.any { host.endsWith(it) }) {
            return true
        }

        return false
    }

    /**
     * CSS and JavaScript injection to hide overlay ads, anti-adblock banners,
     * and enlarge interactive buttons for a 10-foot TV experience.
     */
    val TV_INJECTED_CSS_JS: String = """
        (function() {
            try {
                // 1. Inject anti-ad and TV UI CSS
                var style = document.getElementById('cinejoy-tv-custom-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'cinejoy-tv-custom-style';
                    style.innerHTML = `
                        /* Remove ad banners and popups */
                        iframe[id*="ad"], iframe[src*="ad"], iframe[src*="banner"],
                        .ad-container, .advertisement, .ad-banner, .banner-ad,
                        div[class*="popup"]:not([class*="modal"]):not([class*="player"]),
                        div[id*="overlay"]:not([id*="player"]):not([class*="video"]),
                        div[style*="z-index: 2147483647"]:not(.jwplayer):not(.video-js),
                        div[style*="z-index: 999999"]:not(.jwplayer):not(.video-js),
                        #pop-ad, .floating-banner {
                            display: none !important;
                            visibility: hidden !important;
                            pointer-events: none !important;
                            opacity: 0 !important;
                            width: 0 !important;
                            height: 0 !important;
                        }

                        /* TV 10-foot UI optimizations */
                        html, body {
                            background-color: #0d0d0d !important;
                            color: #f1f1f1 !important;
                            user-select: none !important;
                            -webkit-user-select: none !important;
                            scroll-behavior: smooth !important;
                        }

                        /* Highlight focused elements for TV remote D-Pad navigation */
                        :focus {
                            outline: 3px solid #E50914 !important;
                            outline-offset: 2px !important;
                            box-shadow: 0 0 12px rgba(229, 9, 20, 0.8) !important;
                        }

                        /* Ensure video containers fill the display properly */
                        video {
                            max-width: 100% !important;
                        }
                    `;
                    document.head.appendChild(style);
                }

                // 2. Neutralize rogue window.open popup spam
                window.open = function(url, target, features) {
                    console.log('CinejoyTV blocked popup window: ' + url);
                    return null;
                };

                // 3. Remove annoying click hijackers
                document.querySelectorAll('a[target="_blank"]').forEach(function(el) {
                    var href = el.getAttribute('href') || '';
                    if (href.indexOf('cinejoy') === -1 && href.indexOf('http') === 0) {
                        el.removeAttribute('target');
                    }
                });
            } catch(e) {
                console.error('Cinejoy TV script error', e);
            }
        })();
    """.trimIndent()
}
