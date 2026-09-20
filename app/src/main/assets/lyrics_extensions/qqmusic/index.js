module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = cleanTitle(track.title);
    var artist = cleanArtist(track.artist || "");

    try {
      // 1. Search song
      var query = (title + " " + artist).trim();
      var searchUrl = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?format=json&w=" +
                      encodeURIComponent(query) + "&p=1&n=10";
      var searchRes = await fetch(searchUrl, {
        headers: {
          "Referer": "https://y.qq.com/",
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        }
      });
      if (!searchRes.ok) return null;
      var searchData = searchRes.json();
      var songList = searchData && searchData.data && searchData.data.song && searchData.data.song.list;
      if (!Array.isArray(songList) || songList.length === 0) return null;

      var match = songList[0];
      var songMid = match.mid || match.songmid;
      var songId = match.id || match.songid;
      if (!songMid) return null;

      // 2. Fetch lyrics
      var lyricsUrl = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=" +
                      encodeURIComponent(songMid) + "&songid=" + (songId || 0) +
                      "&format=json&nobase64=0";
      var lyricsRes = await fetch(lyricsUrl, {
        headers: {
          "Referer": "https://y.qq.com/",
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        }
      });
      if (!lyricsRes.ok) return null;
      var lyricsData = lyricsRes.json();
      if (!lyricsData || !lyricsData.lyric) return null;

      var rawLrc = decodeBase64(lyricsData.lyric);
      if (rawLrc && rawLrc.trim()) {
        return rawLrc.trim();
      }
      return null;
    } catch (e) {
      return null;
    }
  }
};

function decodeBase64(str) {
  if (typeof atob === 'function') {
    try { return decodeURIComponent(escape(atob(str))); } catch(e) {
      try { return atob(str); } catch(e2) {}
    }
  }
  var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  var output = '';
  str = String(str).replace(/=+$/, '');
  for (var bc = 0, bs, buffer, idx = 0; buffer = str.charAt(idx++); ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer, bc++ % 4) ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0) {
    buffer = chars.indexOf(buffer);
  }
  return output;
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
