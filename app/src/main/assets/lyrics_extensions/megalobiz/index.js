module.exports = {
  getLyrics: async function(track) {
    if (!track || !track.title) return null;
    var title = track.title;
    var artist = track.artist || "";
    var query = (title + " " + artist).trim();

    try {
      var searchUrl = "https://www.megalobiz.com/search/all?qry=" + encodeURIComponent(query) + "&searchButton.x=0&searchButton.y=0";
      var searchRes = await fetch(searchUrl, {
        headers: {
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }
      });
      if (!searchRes.ok) return null;
      var searchHtml = searchRes.text();
      if (!searchHtml) return null;

      // Find first /lrc/maker/ link
      var match = /href="(\/lrc\/maker\/[^"]+)"/i.exec(searchHtml);
      if (!match) return null;
      var hitPath = match[1];

      var hitUrl = "https://www.megalobiz.com" + hitPath;
      var hitRes = await fetch(hitUrl, {
        headers: {
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }
      });
      if (!hitRes.ok) return null;
      var hitHtml = hitRes.text();
      if (!hitHtml) return null;

      // Find <span id="lrc_..._details"> or content of the lyrics block
      var lrcMatch = /<span[^>]*id="lrc_[^"]*_details"[^>]*>([\s\S]*?)<\/span>/i.exec(hitHtml);
      if (lrcMatch) {
        return lrcMatch[1].replace(/<br\s*\/?>/gi, "\n").replace(/<[^>]+>/g, "").trim();
      }
    } catch (e) {}

    return null;
  }
};
