var APPLE_SEARCH = "https://amp-api.music.apple.com/v1/catalog/us/search";
var APPLE_PAGE_URL = "https://beta.music.apple.com";
var SCRIPT_REGEX = /\/assets\/index~[^"' <]+\.js/;
var TOKEN_REGEX = /eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g;

// Hardcoded fallback token — used as the fast-path default to avoid
// downloading a 3.3MB JS file from Apple's CDN every time. Dynamic refresh
// (via refreshToken) is only attempted when this returns HTTP 401.
var FALLBACK_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzg2MzYyMTUwLCJleHAiOjE3OTI0MTAxNTAsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.wmgvODbrLN8VxNt45wP6fxrI-U2PJhDD1Y1ZokU1ZqAKg_2F8rB30P_MwzPlQ0SyEGPXNg8Pfh7HUsO1cBv3cQ";
var dynamicToken = null;

module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = cleanTitle(track.title);
    var rawArtist = (track.artist || "").trim();
    var artist = cleanArtist(rawArtist);
    var durationMs = track.durationMs || 0;

    try {
      console.log('[AppleMusic] getLyrics: title=' + title + ', artist=' + artist);
      var token = getToken();
      // Try with full artist name first (matches SpotiFLAC-Mobile), then fallback to clean primary artist
      var songs = await searchAppleMusic(token, title, rawArtist);

      // 401 = token expired, try dynamic refresh
      if (songs === null) {
        token = await refreshToken();
        if (!token) return null;
        songs = await searchAppleMusic(token, title, rawArtist);
      }

      if ((!songs || songs.length === 0) && artist && artist !== rawArtist) {
        songs = await searchAppleMusic(token, title, artist);
      }

      if (!songs || songs.length === 0) {
        songs = await searchAppleMusic(token, title, "");
      }

      if (!songs || songs.length === 0) return null;

      var bestSong = selectBestSong(songs, title, artist, durationMs);
      if (!bestSong || !bestSong.id) return null;
      console.log('[AppleMusic] getLyrics bestSong: ' + bestSong.id + ' (' + (bestSong.attributes && bestSong.attributes.name) + ')');

      return await fetchLyricsById(bestSong.id);
    } catch (e) {
      console.error('[AppleMusic] getLyrics error: ' + (e && e.message ? e.message : String(e)));
      return null;
    }
  },

  searchLyrics: async function(query) {
    if (!query || !query.title) return [];
    var title = cleanTitle(query.title);
    var rawArtist = (query.artist || "").trim();
    var artist = cleanArtist(rawArtist);
    var durationMs = query.durationMs || 0;
    console.log('[AppleMusic] searchLyrics: title=' + title + ', artist=' + artist);

    try {
      var token = getToken();
      var songs = await searchAppleMusic(token, title, rawArtist);

      if (songs === null) {
        token = await refreshToken();
        if (!token) return [];
        songs = await searchAppleMusic(token, title, rawArtist);
      }

      if ((!songs || songs.length === 0) && artist && artist !== rawArtist) {
        songs = await searchAppleMusic(token, title, artist);
      }

      if (!songs || songs.length === 0) {
        songs = await searchAppleMusic(token, title, "");
      }

      if (!songs || songs.length === 0) {
        console.log('[AppleMusic] searchLyrics: no songs found in Apple catalog');
        return [];
      }

      console.log('[AppleMusic] searchLyrics: found ' + songs.length + ' songs, fetching lyrics in parallel...');
      var limit = Math.min(songs.length, 5);
      var promises = songs.slice(0, limit).map(async function(s) {
        var sAttr = s.attributes || {};
        try {
          var lrcText = await fetchLyricsById(s.id);
          if (lrcText && lrcText.trim()) {
            return {
              id: "apple_music_" + s.id,
              title: sAttr.name || title,
              artist: sAttr.artistName || artist,
              album: sAttr.albumName || "",
              durationSeconds: Math.floor((sAttr.durationInMillis || durationMs) / 1000),
              provider: "Apple Music",
              syncedLyrics: lrcText.trim(),
              plainLyrics: null
            };
          }
        } catch (e2) {}
        return null;
      });

      var settled = await Promise.all(promises);
      var results = settled.filter(function(r) { return r !== null; });
      console.log('[AppleMusic] searchLyrics: returning ' + results.length + ' matched lyrics');
      return results;
    } catch (e) {
      console.error('[AppleMusic] searchLyrics error: ' + (e && e.message ? e.message : String(e)));
      return [];
    }
  }
};

/**
 * Returns the best available token: dynamic (if refreshed) → fallback.
 */
function getToken() {
  return dynamicToken || FALLBACK_TOKEN || getFallbackTokenFromPage();
}

/**
 * Fetches a fresh fallback token on first run by requesting
 * beta.music.apple.com and extracting it from the page's index script.
 * This is lazy-initialized and cached for the engine's lifetime.
 */
function getFallbackTokenFromPage() {
  // Will be set if refreshToken succeeds; otherwise we have no token
  return null;
}

/**
 * Dynamically retrieves a guest Apple Music JWT token by scraping
 * beta.music.apple.com — matching SpotiFLAC-Mobile's Go backend.
 * Only called on 401 (expired token) to avoid the expensive 3.3MB download.
 */
async function refreshToken() {
  try {
    var pageRes = await fetch(APPLE_PAGE_URL, {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
      }
    });
    if (!pageRes.ok) return null;
    var pageHtml = pageRes.text();

    var scriptMatch = pageHtml.match(SCRIPT_REGEX);
    if (!scriptMatch) return null;

    var jsRes = await fetch(APPLE_PAGE_URL + scriptMatch[0], {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
      }
    });
    if (!jsRes.ok) return null;
    var jsText = jsRes.text();

    var tokens = jsText.match(TOKEN_REGEX);
    if (!tokens || tokens.length === 0) return null;

    dynamicToken = tokens[0];
    return dynamicToken;
  } catch (e) {
    return null;
  }
}

/**
 * Searches the Apple Music catalog.
 * Returns null on 401 (signal to refresh token), empty array on no results.
 */
async function searchAppleMusic(token, title, artist) {
  if (!token) return null;
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

  if (searchRes.status === 401) return null;
  if (!searchRes.ok) return [];

  var searchData = searchRes.json();
  var songs = searchData && searchData.results && searchData.results.songs && searchData.results.songs.data;
  if (!Array.isArray(songs)) return [];
  return songs;
}

/**
 * Fetches lyrics from Paxsenix proxy for a given Apple Music song ID.
 */
async function fetchLyricsById(songId) {
  var lyricsUrl = "https://lyrics.paxsenix.org/apple-music/lyrics?id=" + encodeURIComponent(songId);
  var lyricsRes = await fetch(lyricsUrl, {
    headers: { "User-Agent": "MusicBeat/1.0 (Android)" }
  });
  if (!lyricsRes.ok) return null;

  var data = lyricsRes.json();
  if (!data) return null;

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
 * Selects best matching song — title match is primary, duration secondary.
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
    } else if (sName.indexOf(normTitle) === 0) {
      score += 50;
    } else if (sName.indexOf(normTitle) >= 0) {
      score += 20;
    } else {
      continue;
    }

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

  return best || (songs.length > 0 ? songs[0] : null);
}

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
