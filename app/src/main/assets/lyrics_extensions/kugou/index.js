module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = track.title;
    var artist = track.artist || "";
    var durationMs = track.durationMs || 0;
    var keyword = (title + " " + artist).trim();

    try {
      var searchUrl = "https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=" +
                      encodeURIComponent(keyword) + "&duration=" + durationMs + "&hash=";
      var searchRes = await fetch(searchUrl, {
        headers: { "User-Agent": "BitChord (https://github.com/bitchord)" }
      });
      if (!searchRes.ok) return null;
      var searchData = searchRes.json();
      var candidates = searchData && searchData.candidates;
      if (!Array.isArray(candidates) || candidates.length === 0) return null;

      var best = candidates[0];
      if (!best || !best.id || !best.accesskey) return null;

      var downloadUrl = "https://lyrics.kugou.com/download?ver=1&client=pc&id=" +
                        encodeURIComponent(best.id) + "&accesskey=" + encodeURIComponent(best.accesskey) +
                        "&fmt=lrc&charset=utf8";
      var downloadRes = await fetch(downloadUrl, {
        headers: { "User-Agent": "BitChord (https://github.com/bitchord)" }
      });
      if (!downloadRes.ok) return null;
      var downloadData = downloadRes.json();
      if (!downloadData || !downloadData.content) return null;

      // Base64 decode
      var base64 = downloadData.content;
      return typeof atob === "function" ? atob(base64) : base64;
    } catch (e) {
      return null;
    }
  }
};
