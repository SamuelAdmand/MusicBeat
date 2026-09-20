var APPLE_SEARCH = "https://amp-api.music.apple.com/v1/catalog/us/search";
var APPLE_PAGE_URL = "https://beta.music.apple.com";
var SCRIPT_REGEX = /\/assets\/index~[^"' <]+\.js/;
var TOKEN_REGEX = /eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g;

// In-memory token cache (per engine lifecycle)
var cachedToken = null;

module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = cleanTitle(track.title);
    var artist = cleanArtist(track.artist || "");
    var durationMs = track.durationMs || 0;

    try {
      var token = await getAppleMusicToken();
      if (!token) return null;

      var songs = await searchAppleMusic(token, title, artist);
      if (!songs) {
        // Token might be stale — clear and retry once
        cachedToken = null;
        token = await getAppleMusicToken();
        if (!token) return null;
        songs = await searchAppleMusic(token, title, artist);
      }
      if (!songs || songs.length === 0) return null;

      var bestSong = selectBestSong(songs, title, artist, durationMs);
      if (!bestSong || !bestSong.id) return null;

      return await fetchLyricsById(bestSong.id);
    } catch (e) {
      return null;
    }
  },

  searchLyrics: async function(query) {
    if (!query || !query.title) return [];
    var title = cleanTitle(query.title);
    var artist = cleanArtist(query.artist || "");
    var durationMs = query.durationMs || 0;

    try {
      var token = await getAppleMusicToken();
      if (!token) return [];

      var songs = await searchAppleMusic(token, title, artist);
      if (!songs) {
        cachedToken = null;
        token = await getAppleMusicToken();
        if (!token) return [];
        songs = await searchAppleMusic(token, title, artist);
      }
      if (!songs || songs.length === 0) return [];

      var results = [];
      var limit = Math.min(songs.length, 5);
      for (var i = 0; i < limit; i++) {
        var s = songs[i];
        var sAttr = s.attributes || {};
        var sId = s.id;
        try {
          var lrcText = await fetchLyricsById(sId);
          if (lrcText && lrcText.trim()) {
            results.push({
              id: "apple_music_" + sId,
              title: sAttr.name || title,
              artist: sAttr.artistName || artist,
              album: sAttr.albumName || "",
              durationSeconds: Math.floor((sAttr.durationInMillis || durationMs) / 1000),
              provider: "Apple Music",
              syncedLyrics: lrcText.trim(),
              plainLyrics: null
            });
          }
        } catch (e2) {}
      }
      return results;
    } catch (e) {
      return [];
    }
  }
};

/**
 * Dynamically retrieves a guest Apple Music JWT token by scraping
 * beta.music.apple.com's index script — matching SpotiFLAC-Mobile's
 * go_backend/lyrics_apple.go approach.
 */
async function getAppleMusicToken() {
  if (cachedToken) return cachedToken;

  try {
    // 1. Fetch the Apple Music beta page
    var pageRes = await fetch(APPLE_PAGE_URL, {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
      }
    });
    if (!pageRes.ok) return null;
    var pageHtml = pageRes.text();

    // 2. Extract the index script path
    var scriptMatch = pageHtml.match(SCRIPT_REGEX);
    if (!scriptMatch) return null;

    // 3. Fetch the script and extract JWT tokens
    var jsRes = await fetch(APPLE_PAGE_URL + scriptMatch[0], {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
      }
    });
    if (!jsRes.ok) return null;
    var jsText = jsRes.text();

    var tokens = jsText.match(TOKEN_REGEX);
    if (!tokens || tokens.length === 0) return null;

    // Use the first token (WebPlayKid) — matches SpotiFLAC-Mobile behavior
    cachedToken = tokens[0];
    return cachedToken;
  } catch (e) {
    return null;
  }
}

/**
 * Searches the Apple Music catalog for songs matching the query.
 * Returns null on 401 (token expired), empty array on no results.
 */
async function searchAppleMusic(token, title, artist) {
  var term = (title + " " + artist).trim();
  var searchUrl = APPLE_SEARCH + "?term=" + encodeURIComponent(term) + "&types=songs&limit=10&l=en-US";

  var searchRes = await fetch(searchUrl, {
    headers: {
      "Authorization": "Bearer " + token,
      "Origin": "https://music.apple.com",
      "Referer": "https://music.apple.com/",
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
  });

  if (searchRes.status === 401) return null; // Signal token expired
  if (!searchRes.ok) return [];

  var searchData = searchRes.json();
  var songs = searchData && searchData.results && searchData.results.songs && searchData.results.songs.data;
  if (!Array.isArray(songs)) return [];
  return songs;
}

/**
 * Fetches lyrics for a given Apple Music song ID via the Paxsenix proxy.
 * Returns the best available LRC text, preferring elrc > lrc > formatted content > plain.
 */
async function fetchLyricsById(songId) {
  var lyricsUrl = "https://lyrics.paxsenix.org/apple-music/lyrics?id=" + encodeURIComponent(songId);
  var lyricsRes = await fetch(lyricsUrl, {
    headers: { "User-Agent": "MusicBeat/1.0 (Android)" }
  });
  if (!lyricsRes.ok) return null;

  var data = lyricsRes.json();
  if (!data) return null;

  // Priority: elrc (word-synced) > lrc (line-synced) > formatted content > plain > ttml
  if (data.elrc && data.elrc.trim()) return data.elrc.trim();
  if (data.lrc && data.lrc.trim()) return data.lrc.trim();

  if (Array.isArray(data.content) && data.content.length > 0) {
    return formatPaxContent(data.type || "Line", data.content);
  }

  if (data.plain && data.plain.trim()) return data.plain.trim();
  if (data.ttmlContent && data.ttmlContent.trim()) return data.ttmlContent.trim();

  return null;
}

/**
 * Selects the best matching song from Apple Music search results.
 * Title match is the primary criterion; duration is secondary (and
 * ignored when durationMs <= 0, which is common in the lyrics editor).
 */
function selectBestSong(songs, title, artist, durationMs) {
  var normTitle = cleanTitle(title).toLowerCase();
  var best = null;
  var bestScore = -1;

  for (var i = 0; i < songs.length; i++) {
    var s = songs[i];
    var sName = cleanTitle(s.attributes && s.attributes.name ? s.attributes.name : "").toLowerCase();
    var sDur = s.attributes && s.attributes.durationInMillis ? s.attributes.durationInMillis : 0;

    var score = 0;
    if (sName === normTitle) {
      score += 100;
    } else if (sName.startsWith(normTitle)) {
      score += 50;
    } else if (sName.indexOf(normTitle) >= 0) {
      score += 20;
    } else {
      continue;
    }

    // Only factor in duration when the caller provides a valid value
    if (durationMs > 0 && sDur > 0) {
      var diff = Math.abs(sDur - durationMs);
      if (diff < 10000) score += 30;
      else if (diff < 30000) score += 10;
    }

    if (score > bestScore) {
      bestScore = score;
      best = s;
    }
  }

  // If no title matched at all, fall back to the first result
  return best || (songs.length > 0 ? songs[0] : null);
}

/**
 * Formats Paxsenix structured content into LRC text.
 */
function formatPaxContent(lyricsType, content) {
  var isSyllable = (lyricsType === "Syllable" || lyricsType === "syllable");
  var lines = [];

  for (var i = 0; i < content.length; i++) {
    var line = content[i];
    var timeTag = msToLRCTimestamp(line.timestamp || 0);

    if (isSyllable && Array.isArray(line.text) && line.text.length > 0) {
      var lineStr = timeTag;
      for (var j = 0; j < line.text.length; j++) {
        var detail = line.text[j];
        if (detail.timestamp !== undefined && detail.timestamp !== null) {
          lineStr += "<" + msToLRCTimestampInline(detail.timestamp) + ">" + (detail.text || "");
        } else {
          lineStr += (detail.text || "");
        }
        if (!detail.part) lineStr += " ";
      }
      lines.push(lineStr.trim());
    } else if (Array.isArray(line.text) && line.text.length > 0) {
      lines.push(timeTag + (line.text[0].text || ""));
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
