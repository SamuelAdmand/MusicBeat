module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = track.title;
    var artist = track.artist || "";
    var durationSec = Math.floor((track.durationMs || 0) / 1000);
    var album = track.album || "";

    var cleanArtist = artist.replace(/\s*\((?:feat|ft)\.?.*?\)/i, "")
                            .replace(/\s*(?:feat|ft)\.?.*$/i, "")
                            .trim();

    async function query(a) {
      var url = "https://lyrics-api.boidu.dev/getLyrics?s=" + encodeURIComponent(title) +
                "&a=" + encodeURIComponent(a);
      if (durationSec > 0) url += "&d=" + durationSec;
      if (album) url += "&al=" + encodeURIComponent(album);

      try {
        var res = await fetch(url, {
          headers: {
            "Accept": "application/json",
            "User-Agent": "BitChord (https://github.com/bitchord)"
          }
        });
        if (!res.ok) return null;
        var data = res.json();
        return data && data.ttml ? data.ttml : null;
      } catch (e) {
        return null;
      }
    }

    var ttml = await query(artist);
    if (!ttml && cleanArtist && cleanArtist.toLowerCase() !== artist.toLowerCase()) {
      ttml = await query(cleanArtist);
    }
    return ttml;
  }
};
