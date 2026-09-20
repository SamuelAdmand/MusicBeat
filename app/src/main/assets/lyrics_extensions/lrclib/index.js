module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = cleanText(track.title);
    var artist = cleanText(track.artist || "");
    var durationSec = Math.floor((track.durationMs || 0) / 1000);

    // 1. Exact match
    try {
      var getUrl = "https://lrclib.net/api/get?track_name=" + encodeURIComponent(title) +
                   "&artist_name=" + encodeURIComponent(artist) +
                   "&duration=" + durationSec;
      var getRes = await fetch(getUrl, {
        headers: { "User-Agent": "BitChord (https://github.com/bitchord)" }
      });
      if (getRes.ok) {
        var data = getRes.json();
        if (data.syncedLyrics && data.syncedLyrics.trim()) return data.syncedLyrics;
        if (data.plainLyrics && data.plainLyrics.trim()) return data.plainLyrics;
      }
    } catch (e) {}

    // 2. Fuzzy search fallback
    try {
      var searchUrl = "https://lrclib.net/api/search?track_name=" + encodeURIComponent(title) +
                      "&artist_name=" + encodeURIComponent(artist);
      var searchRes = await fetch(searchUrl, {
        headers: { "User-Agent": "BitChord (https://github.com/bitchord)" }
      });
      if (searchRes.ok) {
        var hits = searchRes.json();
        if (Array.isArray(hits) && hits.length > 0) {
          // Prefer synced lyrics with closest duration
          var syncedHits = hits.filter(function(h) { return h.syncedLyrics && h.syncedLyrics.trim(); });
          if (syncedHits.length > 0) {
            syncedHits.sort(function(a, b) {
              return Math.abs((a.duration || 0) - durationSec) - Math.abs((b.duration || 0) - durationSec);
            });
            return syncedHits[0].syncedLyrics;
          }
          var plainHits = hits.filter(function(h) { return h.plainLyrics && h.plainLyrics.trim(); });
          if (plainHits.length > 0) {
            plainHits.sort(function(a, b) {
              return Math.abs((a.duration || 0) - durationSec) - Math.abs((b.duration || 0) - durationSec);
            });
            return plainHits[0].plainLyrics;
          }
        }
      }
    } catch (e) {}

    return null;
  },

  searchLyrics: async function(query) {
    if (!query || !query.title) return [];
    var title = cleanText(query.title);
    var artist = cleanText(query.artist || "");
    try {
      var searchUrl = "https://lrclib.net/api/search?track_name=" + encodeURIComponent(title) +
                      "&artist_name=" + encodeURIComponent(artist);
      var res = await fetch(searchUrl, {
        headers: { "User-Agent": "BitChord (https://github.com/bitchord)" }
      });
      if (!res.ok) return [];
      var hits = res.json();
      if (!Array.isArray(hits)) return [];
      return hits.map(function(h) {
        return {
          id: "lrclib_" + h.id,
          title: h.trackName || title,
          artist: h.artistName || artist,
          album: h.albumName || "",
          durationSeconds: Math.floor(h.duration || 0),
          provider: "LRCLIB",
          syncedLyrics: h.syncedLyrics || null,
          plainLyrics: h.plainLyrics || null
        };
      });
    } catch (e) {
      return [];
    }
  }
};

function cleanText(text) {
  return text.replace(/\((?:from|feat\.?|official|lyrical|video|audio|remix)[^)]*\)/gi, " ")
             .replace(/\[[^\]]*\]/g, " ")
             .replace(/\b(?:official (?:video|audio|music video)|lyrical|full song|4k video)\b/gi, " ")
             .replace(/\s+/g, " ")
             .trim();
}
