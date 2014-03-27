package mining.util

object UrlUtil {
  //TODO:still not perfect, best be fixed lenght format
  def urlToUid(url: String) = url.replaceAll("""http://""", "").replaceAll("[^a-zA-Z0-9]+","")
}