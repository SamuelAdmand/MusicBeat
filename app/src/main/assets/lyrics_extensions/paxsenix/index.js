var APPLE_SEARCH = "https://amp-api.music.apple.com/v1/catalog/us/search";
var FALLBACK_APPLE_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzg5MTQ2OTA2LCJleHAiOjE3OTUxOTQ5MDYsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.N9nCdw8Bc2GRy3C_RBnCJ4MwhnHX8wz_kSzq4A3k-wfF7B_1T9JyQ0VZUMUu3HzjqWff09ZwL060B8JGAxJHTA";

module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = track.title;
    var artist = track.artist || "";
    var durationMs = track.durationMs || 0;

    try {
      // 1. Search Apple Music catalog
      var term = (title + " " + artist).trim();
      var searchUrl = APPLE_SEARCH + "?term=" + encodeURIComponent(term) + "&types=songs&limit=10&l=en-US";
      var searchRes = await fetch(searchUrl, {
        headers: {
          "Authorization": "Bearer " + FALLBACK_APPLE_TOKEN,
          "Origin": "https://music.apple.com",
          "Referer": "https://music.apple.com/",
          "User-Agent": "BitChord (https://github.com/bitchord)"
        }
      });
      if (!searchRes.ok) return null;
      var searchData = searchRes.json();
      var songs = searchData && searchData.results && searchData.results.songs && searchData.results.songs.data;
      if (!Array.isArray(songs) || songs.length === 0) return null;

      // Pick best matching song ID
      var bestId = null;
      var bestDiff = 999999999;
      for (var i = 0; i < songs.length; i++) {
        var s = songs[i];
        var sDur = s.attributes && s.attributes.durationInMillis ? s.attributes.durationInMillis : 0;
        var diff = Math.abs(sDur - durationMs);
        if (bestId === null || diff < bestDiff) {
          bestId = s.id;
          bestDiff = diff;
        }
      }
      if (!bestId) return null;

      // 2. Fetch lyrics from PaxSenix proxy
      var lyricsUrl = "https://lyrics.paxsenix.org/apple-music/lyrics?id=" + encodeURIComponent(bestId) + "&ttml=true";
      var lyricsRes = await fetch(lyricsUrl, {
        headers: { "User-Agent": "BitChord (https://github.com/bitchord)" }
      });
      if (!lyricsRes.ok) return null;
      var raw = lyricsRes.text();
      return raw || null;
    } catch (e) {
      return null;
    }
  }
};
