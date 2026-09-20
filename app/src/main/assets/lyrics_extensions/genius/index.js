module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = cleanTitle(track.title);
    var artist = cleanTitle(track.artist || "");
    var query = (title + " " + artist).trim();

    try {
      var searchUrl = "https://genius.com/api/search/multi?per_page=5&q=" + encodeURIComponent(query);
      var searchRes = await fetch(searchUrl, {
        headers: {
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }
      });
      if (!searchRes.ok) return null;
      var data = searchRes.json();
      var sections = data && data.response && data.response.sections;
      if (!Array.isArray(sections)) return null;

      var songUrl = null;
      for (var i = 0; i < sections.length; i++) {
        var sec = sections[i];
        if (sec.type === "song" && Array.isArray(sec.hits) && sec.hits.length > 0) {
          var hit = sec.hits[0];
          if (hit.result && hit.result.url) {
            songUrl = hit.result.url;
            break;
          }
        }
      }
      if (!songUrl) return null;

      var pageRes = await fetch(songUrl, {
        headers: {
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }
      });
      if (!pageRes.ok) return null;
      var html = pageRes.text();
      if (!html) return null;

      return parseGeniusHtml(html);
    } catch (e) {
      return null;
    }
  }
};

function cleanTitle(str) {
  return str.replace(/\((?:from|feat\.?|official|lyrical|video|audio|remix)[^)]*\)/gi, " ")
            .replace(/\[[^\]]*\]/g, " ")
            .replace(/\s+/g, " ")
            .trim();
}

function parseGeniusHtml(html) {
  // Find all data-lyrics-container="true" sections
  var regex = /<div[^>]*data-lyrics-container="true"[^>]*>([\s\S]*?)<\/div>/gi;
  var match;
  var textParts = [];
  while ((match = regex.exec(html)) !== null) {
    var section = match[1];
    // Replace <br> and <br/> with newline
    section = section.replace(/<br\s*\/?>/gi, "\n");
    // Strip all HTML tags
    section = section.replace(/<[^>]+>/g, "");
    // Decode HTML entities
    section = decodeEntities(section);
    textParts.push(section.trim());
  }

  var fullText = textParts.join("\n\n").trim();
  // Remove "You might also like" and embed remnants
  fullText = fullText.replace(/You might also like/gi, "")
                     .replace(/\d*Embed$/gi, "")
                     .trim();

  return fullText.length > 0 ? fullText : null;
}

function decodeEntities(str) {
  return str.replace(/&amp;/g, "&")
            .replace(/&lt;/g, "<")
            .replace(/&gt;/g, ">")
            .replace(/&quot;/g, '"')
            .replace(/&#39;/g, "'")
            .replace(/&#x27;/g, "'")
            .replace(/&nbsp;/g, " ");
}
