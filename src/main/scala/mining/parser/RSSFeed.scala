package mining.parser

import java.io.StringReader
import org.jdom.input.SAXBuilder
import com.sun.syndication.io.SyndFeedInput
import com.sun.syndication.feed.synd.SyndEntry
import scala.collection.JavaConverters._

class RSSFeed(val url: String) {
  val metadata = Map.empty[String, String]

  val root = {
    val content = new Spider().getRssFeed(url, metadata) //implicit dependency on metadata
    val dom = new SAXBuilder().build(
      new StringReader(content)
    )
    new SyndFeedInput().build(dom);
  }
   
  lazy val rssItems: Iterable[SyndEntry] = root.getEntries().asScala.map(_.asInstanceOf[SyndEntry])
   
  override def toString = s"Feed($url)"
}

object RSSFeed {
  def apply(url: String) = new RSSFeed(url)
}
