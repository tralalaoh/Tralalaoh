package com.turkish123

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Turkish123Provider : MainAPI() {
    override var mainUrl = "https://hds.turkish123.com"
    override var name = "Turkish123"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/series" to "Turkish Series",
        "$mainUrl/popular" to "Popular",
        "$mainUrl/latest" to "Latest Episodes"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document
        val home = document.select("div.item, article.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src") ?: this.selectFirst("img")?.attr("data-src"))
        
        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search?q=$query").document
        return document.select("div.item, article.item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        
        val title = document.selectFirst("h1, .title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("img.poster, .poster img")?.attr("src"))
        val description = document.selectFirst(".description, .synopsis, p")?.text()?.trim()
        
        val episodes = document.select("div.episode-item, li.episode, a.episode").mapNotNull { ep ->
            val epHref = fixUrlNull(ep.attr("href")) ?: return@mapNotNull null
            val epName = ep.text().trim()
            val epNum = Regex("Episode (\\d+)").find(epName)?.groupValues?.get(1)?.toIntOrNull()
            
            Episode(
                epHref,
                epName,
                episode = epNum
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Extract video iframes and sources
        document.select("iframe[src*=player], iframe[src*=embed]").forEach { iframe ->
            val iframeUrl = fixUrl(iframe.attr("src"))
            loadExtractor(iframeUrl, subtitleCallback, callback)
        }
        
        // Extract direct video sources
        document.select("source[src], video source").forEach { source ->
            val videoUrl = fixUrl(source.attr("src"))
            val quality = source.attr("label").ifEmpty { "Unknown" }
            
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    videoUrl,
                    referer = mainUrl,
                    quality = getQualityFromName(quality),
                )
            )
        }
        
        return true
    }
}
