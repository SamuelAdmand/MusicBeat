var cachedToken = null;

async function getToken() {
  if (cachedToken) return cachedToken;
  try {
    var url = "https://apic-desktop.musixmatch.com/ws/1.1/token.get?app_id=web-desktop-app-v1.0";
    var res = await fetch(url);
    if (!res.ok) return null;
    var data = res.json();
    var token = data && data.message && data.message.body && data.message.body.user_token;
    if (token && token !== "Upgrade.Me") {
      cachedToken = token;
      return token;
    }
  } catch (e) {}
  return null;
}

module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = track.title;
    var artist = track.artist || "";
    var durationSec = Math.floor((track.durationMs || 0) / 1000);

    var token = await getToken();
    if (!token) return null;

    try {
      var url = "https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?format=json" +
                "&q_track=" + encodeURIComponent(title) +
                "&q_artist=" + encodeURIComponent(artist) +
                "&f_subtitle_length=" + durationSec +
                "&usertoken=" + encodeURIComponent(token) +
                "&app_id=web-desktop-app-v1.0";

      var res = await fetch(url);
      if (!res.ok) return null;
      var data = res.json();
      var macro = data && data.message && data.message.body && data.message.body.macro_calls;
      if (!macro) return null;

      var sub = macro["track.subtitles.get"];
      var subBody = sub && sub.message && sub.message.body && sub.message.body.subtitle_list;
      if (Array.isArray(subBody) && subBody.length > 0) {
        var subtitle = subBody[0] && subBody[0].subtitle;
        if (subtitle && subtitle.subtitle_body) {
          return subtitle.subtitle_body;
        }
      }

      var lyricsCall = macro["track.lyrics.get"];
      var lyricsBody = lyricsCall && lyricsCall.message && lyricsCall.message.body && lyricsCall.message.body.lyrics;
      if (lyricsBody && lyricsBody.lyrics_body) {
        return lyricsBody.lyrics_body;
      }
    } catch (e) {}

    return null;
  }
};
