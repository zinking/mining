package mining.util

object UrlUtil {
    def urlToUid(url: String) = url.replaceAll( """http://""", "").replaceAll("[^a-zA-Z0-9]+", "")
}