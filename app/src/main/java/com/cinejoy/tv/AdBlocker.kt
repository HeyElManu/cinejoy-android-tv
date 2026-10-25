package com.cinejoy.tv

import android.net.Uri

object AdBlocker {

    private val BLOCKED_HOST_PATTERNS = listOf(
        "popads", "popcash", "onclickpredictiv", "adcash",
        "doubleclick", "google-analytics", "googlesyndication", "adnxs",
        "propellerads", "histats", "monetag", "outbrain", "taboola",
        "bet365", "1xbet", "casino", "juicyads", "exoclick",
        "trafficjunky", "adsterra", "coinhive", "cryptoloot"
    )

    private val ALLOWED_DOMAINS = listOf(
        "cinejoy.pk", "cinejoy.to", "cinejoy.lol", "cinejoy.asia", "cinejoy.ink",
        "vidsrc", "2embed", "autoembed", "superembed", "rabbitstream",
        "megacloud", "streamwish", "filemoon", "vidcloud",
        "tmdb.org", "themoviedb.org", "cloudflare.com", "jsdelivr.net", "unpkg.com"
    )

    fun shouldBlockUrl(url: String?): Boolean {
        if (url == null || url.isBlank()) return true
        if (url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("about:blank")) return false
        val uri = try { Uri.parse(url) } catch (e: Exception) { return true }
        val host = uri.host?.lowercase() ?: return true
        if (ALLOWED_DOMAINS.any { host.contains(it) }) {
            return BLOCKED_HOST_PATTERNS.any { host.contains(it) }
        }
        if (BLOCKED_HOST_PATTERNS.any { host.contains(it) }) return true
        val spamTlds = listOf(".xyz", ".top", ".click", ".link", ".buzz", ".live", ".icu", ".cam")
        return spamTlds.any { host.endsWith(it) }
    }

    /**
     * Script injecté dans chaque page :
     * 1. Bloque TOUS les événements clavier au niveau JS → le lecteur vidéo
     *    ne peut plus capturer les flèches pour changer le volume.
     * 2. Supprime les popups, bannières et overlays publicitaires.
     * 3. Optimise l'affichage pour un écran TV 1080p.
     */
    val TV_INJECTED_CSS_JS: String = """
        (function() {
            try {
                // ── 1. BLOQUER TOUS LES ÉVÉNEMENTS CLAVIER côté JS ──────────────
                // Le lecteur vidéo HTML5 (JWPlayer, Plyr, Video.js…) ne recevra
                // JAMAIS les touches flèches → plus de changement de volume/seek
                // par le lecteur. Le curseur Android gère tout à la place.
                ['keydown', 'keyup', 'keypress'].forEach(function(evtType) {
                    window.addEventListener(evtType, function(e) {
                        e.stopImmediatePropagation();
                        e.preventDefault();
                    }, true);
                });

                // Empêche aussi les iframes (lecteurs embarqués) de recevoir les touches
                document.querySelectorAll('iframe').forEach(function(iframe) {
                    try {
                        ['keydown', 'keyup', 'keypress'].forEach(function(evtType) {
                            iframe.contentWindow.addEventListener(evtType, function(e) {
                                e.stopImmediatePropagation();
                                e.preventDefault();
                            }, true);
                        });
                    } catch(ignore) {}
                });

                // Observer pour appliquer le blocage aux iframes chargées dynamiquement
                var iframeObserver = new MutationObserver(function(mutations) {
                    mutations.forEach(function(m) {
                        m.addedNodes.forEach(function(node) {
                            if (node.tagName === 'IFRAME') {
                                try {
                                    ['keydown', 'keyup', 'keypress'].forEach(function(evtType) {
                                        node.contentWindow.addEventListener(evtType, function(e) {
                                            e.stopImmediatePropagation();
                                            e.preventDefault();
                                        }, true);
                                    });
                                } catch(ignore) {}
                            }
                        });
                    });
                });
                iframeObserver.observe(document.body, { childList: true, subtree: true });

                // ── 2. BLOQUER POPUPS & REDIRECTIONS ────────────────────────────
                window.open = function() { return null; };
                document.querySelectorAll('a[target="_blank"]').forEach(function(el) {
                    el.removeAttribute('target');
                });

                // ── 3. CSS TV : SUPPRIMER ADS & OPTIMISER AFFICHAGE ─────────────
                var style = document.getElementById('cinejoy-tv-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'cinejoy-tv-style';
                    style.innerHTML = [
                        /* Masquer bannières pubs et overlays */
                        'iframe[src*="ad"], .ad-container, .advertisement, .ad-banner,',
                        'div[class*="popup"]:not([class*="player"]),',
                        '#pop-ad, .floating-banner, #cookie-law-info-bar { display:none!important; }',

                        /* Éléments fixes / collants qui masquent le lecteur */
                        'header, .header, nav, .navbar, .sticky-top, .fixed-top {',
                        '  position: relative!important; z-index: 10!important; }',

                        /* Contenu TV sombre */
                        'html, body { background:#0d0d0d!important; color:#f0f0f0!important; }',

                        /* Lecteur propre */
                        '.player-wrap, #player, .video-player { z-index:50!important; }',
                    ].join(' ');
                    document.head.appendChild(style);
                }

            } catch(e) {
                console.error('CinejoyTV inject error', e);
            }
        })();
    """.trimIndent()
}
