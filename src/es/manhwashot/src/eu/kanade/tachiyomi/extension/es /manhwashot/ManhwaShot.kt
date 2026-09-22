package eu.kanade.tachiyomi.extension.es.manhwashot

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import io.github.keiyoushi.gradle.api.annotation.Source
import keiyoushi.annotation.Source

@Source
class ManhwaShot : ParsedHttpSource() {

    override val name = "ManhwaShot"
    override val baseUrl = "https://manhwashot.lat"
    override val lang = "es"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request =
        GET("\(baseUrl/explorar/?page=\)page", headers)

    override fun popularMangaSelector(): String = "div.series-grid div.s-card"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("a.s-card-title").text().trim()
        thumbnail_url = element.select("div.s-card-img img").attr("abs:src")
        setUrlWithoutDomain(element.select("a.s-card-imglink, a.s-card-title").attr("href"))
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int): Request =
        GET("\(baseUrl/explorar/?sort=latest&page=\)page", headers)

    override fun latestUpdatesSelector(): String = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("\(baseUrl/explorar/?q=\)query&page=$page", headers)

    override fun searchMangaSelector(): String = popularMangaSelector()
    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst("h1.series-title")?.text()?.trim() ?: ""
        thumbnail_url = document.selectFirst("div.series-cover img")?.attr("abs:src")
        description = document.select("div.series-desc").getOrNull(1)?.text()?.trim()
            ?: document.select("div.series-desc").first()?.text()?.trim() ?: ""
        genre = document.select("div.series-genres a.genre-tag").joinToString { it.text().trim() }

        val statusText = document.select("span.badge-pill").text().lowercase()
        status = when {
            statusText.contains("emisión") -> SManga.ONGOING
            statusText.contains("completado") -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    override fun chapterListSelector(): String = "div.chapters-grid a.ch-row"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.select("span.ch-num").text().trim()
        setUrlWithoutDomain(element.attr("href"))
    }

    override fun pageListParse(document: Document): List {
        val html = document.html()
        val regex = """"(https://img\.manhwashot\.lat/[^"]+\.webp)"""".toRegex()

        val matchedUrls = regex.findAll(html)
            .map { it.groupValues[1] }
            .filter { it.contains("/WP-manga/data/") || it.contains("/capitulo-") }
            .distinct()
            .toList()

        return matchedUrls.mapIndexed { index, url ->
            Page(index, "", url)
        }
    }

    override fun imageUrlParse(document: Document): String = ""
}
