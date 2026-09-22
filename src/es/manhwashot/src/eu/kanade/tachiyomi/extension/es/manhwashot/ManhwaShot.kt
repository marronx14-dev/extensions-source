package eu.kanade.tachiyomi.extension.es.manhwashot

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import keiyoushi.annotation.Source
import keiyoushi.utils.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable

@Source
class ManhwaShot : HttpSource() {

    override val name = "ManhwaShot"
    override val baseUrl = "https://manhwashot.lat"
    override val lang = "es"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request =
        GET("\(baseUrl/explorar/?page=\)page", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select("div.series-grid div.s-card").map { element ->
            popularMangaFromElement(element)
        }
        return MangasPage(mangas, false)
    }

    private fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("a.s-card-title").text().trim()
        thumbnail_url = element.select("div.s-card-img img").attr("abs:src")
        setUrlWithoutDomain(element.select("a.s-card-imglink, a.s-card-title").attr("href"))
    }

    override fun latestUpdatesRequest(page: Int): Request =
        GET("\(baseUrl/explorar/?sort=latest&page=\)page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("\(baseUrl/explorar/?q=\)query&page=$page", headers)

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        return SManga.create().apply {
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
    }

    override fun chapterListParse(response: Response): List {
        val document = response.asJsoup()
        return document.select("div.chapters-grid a.ch-row").map { element ->
            SChapter.create().apply {
                name = element.select("span.ch-num").text().trim()
                setUrlWithoutDomain(element.attr("href"))
            }
        }
    }

    override fun pageListParse(response: Response): List {
        val html = response.body.string()
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

    override fun imageUrlParse(response: Response): String = ""
}
