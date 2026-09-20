var MIRRORS = [
  "https://lyricsplus.binimum.org",
  "https://lyricsplus.prjktla.workers.dev",
  "https://lyricsplus.prjktla.my.id",
  "https://lyricsplus.atomix.one"
];

module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = cleanTitle(track.title);
    var artist = cleanArtist(track.artist || "");
    var durationSec = (track.durationMs || 0) / 1000.0;

    for (var i = 0; i < MIRRORS.length; i++) {
      var server = MIRRORS[i];
      try {
        var url = server + "/v2/lyrics/get?title=" + encodeURIComponent(title) +
                  "&artist=" + encodeURIComponent(artist) +
                  (durationSec > 0 ? ("&duration=" + durationSec) : "");
        var res = await fetch(url, {
          headers: {
            "Accept": "application/json",
            "User-Agent": "MusicBeat/1.0 (Android)"
          }
        });
        if (!res.ok) continue;
        var data = res.json();
        if (!data || !Array.isArray(data.lyrics) || data.lyrics.length === 0) continue;

        // Convert KPOE JSON format to enhanced LRC text
        var lrc = buildLyricsPlusLRC(data);
        if (lrc && lrc.trim()) return lrc.trim();
      } catch (e) {}
    }
    return null;
  },

  searchLyrics: async function(query) {
    var lrc = await module.exports.getLyrics(query);
    if (!lrc) return [];
    return [{
      id: "lyricsplus_" + encodeURIComponent((query.title || "").trim()),
      title: query.title || "",
      artist: query.artist || "",
      album: query.album || "",
      durationSeconds: Math.floor((query.durationMs || 0) / 1000),
      provider: "LyricsPlus",
      syncedLyrics: lrc,
      plainLyrics: null
    }];
  }
};

function buildLyricsPlusLRC(data) {
  var isWordType = (data.type === "Word" || data.type === "Syllable" || data.type === "word" || data.type === "syllable");
  var lines = [];

  for (var i = 0; i < data.lyrics.length; i++) {
    var line = data.lyrics[i];
    var timeMs = Math.floor(line.time || 0);
    var timeTag = msToLRCTimestamp(timeMs);

    if (isWordType && Array.isArray(line.syllabus) && line.syllabus.length > 0) {
      var lineStr = timeTag;
      for (var j = 0; j < line.syllabus.length; j++) {
        var syl = line.syllabus[j];
        var sylTimeMs = Math.floor(syl.time || 0);
        lineStr += "<" + msToLRCTimestampInline(sylTimeMs) + ">" + (syl.text || "");
      }
      lines.push(lineStr);
    } else {
      var text = line.text || "";
      if (!text && Array.isArray(line.syllabus)) {
        text = line.syllabus.map(function(s) { return s.text || ""; }).join("");
      }
      lines.push(timeTag + text.trim());
    }
  }

  return lines.join("\n");
}

function msToLRCTimestamp(ms) {
  var totalSec = Math.floor(ms / 1000);
  var minutes = Math.floor(totalSec / 60);
  var seconds = totalSec % 60;
  var hundredths = Math.floor((ms % 1000) / 10);
  return "[" + pad(minutes, 2) + ":" + pad(seconds, 2) + "." + pad(hundredths, 2) + "]";
}

function msToLRCTimestampInline(ms) {
  var totalSec = Math.floor(ms / 1000);
  var minutes = Math.floor(totalSec / 60);
  var seconds = totalSec % 60;
  var hundredths = Math.floor((ms % 1000) / 10);
  return pad(minutes, 2) + ":" + pad(seconds, 2) + "." + pad(hundredths, 2);
}

function pad(n, width) {
  var s = n + "";
  while (s.length < width) s = "0" + s;
  return s;
}

function cleanTitle(t) {
  return t.replace(/\((?:feat\.?|official|video|audio|remix)[^)]*\)/gi, " ")
          .replace(/\[[^\]]*\]/g, " ")
          .replace(/\s+/g, " ")
          .trim();
}

function cleanArtist(a) {
  return a.split(/[,&/]/)[0].trim();
}
