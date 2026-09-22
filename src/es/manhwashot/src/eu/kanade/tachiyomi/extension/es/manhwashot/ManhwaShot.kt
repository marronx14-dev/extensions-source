package eu.kanade.tachiyomi.extension.es.manhwashot

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import keiyoushi.annotation.Source
import keiyoushi.utils.asJsoup
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element

@Source
abstract class ManhwaShot : HttpSource() {

    override val name = "ManhwaShot"
    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", baseUrl + "/")

    override fun popularMangaRequest(page: Int): Request =
        GET(baseUrl + "/explorar/?page=" + page, headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val elements = document.select("div.series-grid div.s-card")
        val mangaArray = Array(elements.size) { i ->
            popularMangaFromElement(elements[i])
        }
        return MangasPage(mangaArray.asList(), false)
    }

    private fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("a.s-card-title").text().trim()
        thumbnail_url = element.select("div.s-card-img img").attr("abs:src")
        setUrlWithoutDomain(element.select("a.s-card-imglink, a.s-card-title").attr("href"))
    }

    override fun latestUpdatesRequest(page: Int): Request =
        GET(baseUrl + "/explorar/?sort=latest&page=" + page, headers)

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET(baseUrl + "/explorar/?q=" + query + "&page=" + page, headers)

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

    override fun chapterListParse(response: Response) = run {
        val document = response.asJsoup()
        val elements = document.select("div.chapters-grid a.ch-row")
        val chapters = Array(elements.size) { i ->
            val el = elements[i]
            SChapter.create().apply {
                name = el.select("span.ch-num").text().trim()
                setUrlWithoutDomain(el.attr("href"))
            }
        }
        chapters.asList()
    }

    override fun pageListParse(response: Response) = run {
        val html = response.body.string()
        val regex = Regex("""(https://img\.manhwashot\.lat/[^"]+\.webp)""")

        val matchedUrls = regex.findAll(html)
            .map { it.groupValues[1] }
            .filter { it.contains("/WP-manga/data/") || it.contains("/capitulo-") }
            .distinct()
            .toList()

        val pages = Array(matchedUrls.size) { i ->
            Page(i, "", matchedUrls[i])
        }
        pages.asList()
    }

    override fun imageUrlParse(response: Response): String = ""
}
