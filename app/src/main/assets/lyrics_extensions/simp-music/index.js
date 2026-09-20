module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.videoId) return null;
    try {
      var url = "https://api-lyrics.simpmusic.org/v1/" + encodeURIComponent(track.videoId);
      var res = await fetch(url, {
        headers: { "User-Agent": "BitChord (https://github.com/bitchord)" }
      });
      if (!res.ok) return null;
      var raw = res.text();
      return raw || null;
    } catch (e) {
      return null;
    }
  }
};
