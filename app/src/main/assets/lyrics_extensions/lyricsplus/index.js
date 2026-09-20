var MIRRORS = [
  "https://lyricsplus.prjktla.my.id",
  "https://lyricsplus.atomix.one",
  "https://lyricsplus.binimum.org",
  "https://lyricsplus.prjktla.workers.dev",
  "https://lyricsplus-seven.vercel.app",
  "https://lyrics-plus-backend.vercel.app"
];

var lastGoodMirror = null;

module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = track.title;
    var artist = track.artist || "";
    var durationMs = track.durationMs || 0;

    var hosts = lastGoodMirror ? [lastGoodMirror].concat(MIRRORS.filter(function(m) { return m !== lastGoodMirror; })) : MIRRORS;

    for (var i = 0; i < hosts.length; i++) {
      var host = hosts[i];
      try {
        var url = host + "/v2/lyrics?title=" + encodeURIComponent(title) +
                  "&artist=" + encodeURIComponent(artist) +
                  "&duration=" + durationMs;
        var res = await fetch(url, {
          headers: {
            "Accept": "application/json",
            "User-Agent": "BitChord (https://github.com/bitchord)"
          }
        });
        if (res.ok) {
          var data = res.json();
          if (data && (data.ttml || data.lyrics || data.lrc)) {
            lastGoodMirror = host;
            return data.ttml || data.lyrics || data.lrc;
          }
        }
      } catch (e) {}
    }
    return null;
  }
};
