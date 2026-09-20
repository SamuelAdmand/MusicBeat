module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = cleanTitle(track.title);
    var artist = cleanArtist(track.artist || "");

    try {
      // 1. Search song
      var query = (title + " " + artist).trim();
      var searchUrl = "https://lyrics.paxsenix.org/netease/search?q=" + encodeURIComponent(query);
      var searchRes = await fetch(searchUrl, {
        headers: { "User-Agent": "MusicBeat/1.0 (Android)", "Accept": "application/json" }
      });
      if (!searchRes.ok) return null;
      var searchData = searchRes.json();
      var songs = searchData && searchData.result && searchData.result.songs;
      if (!Array.isArray(songs) || songs.length === 0) return null;

      var songId = songs[0].id;
      if (!songId) return null;

      // 2. Fetch lyrics
      var lyricsUrl = "https://lyrics.paxsenix.org/netease/lyrics?id=" + encodeURIComponent(songId);
      var lyricsRes = await fetch(lyricsUrl, {
        headers: { "User-Agent": "MusicBeat/1.0 (Android)", "Accept": "application/json" }
      });
      if (!lyricsRes.ok) return null;
      var lyricsData = lyricsRes.json();
      if (lyricsData && lyricsData.lrc && lyricsData.lrc.lyric) {
        var lyric = lyricsData.lrc.lyric.trim();
        return lyric || null;
      }
      return null;
    } catch (e) {
      return null;
    }
  }
};

function cleanTitle(t) {
  return t.replace(/\((?:feat\.?|official|video|audio|remix)[^)]*\)/gi, " ")
          .replace(/\[[^\]]*\]/g, " ")
          .replace(/\s+/g, " ")
          .trim();
}

function cleanArtist(a) {
  return a.split(/[,&/]/)[0].trim();
}
