package mining.parser

import java.io.StringReader
import org.jdom.input.SAXBuilder
import com.sun.syndication.io.SyndFeedInput
import com.sun.syndication.feed.synd.SyndEntry
import scala.collection.JavaConverters._
import mining.io.FeedDescriptor

class RSSFeed(val fd: FeedDescriptor) {
  val url = fd.feedUrl

  val root = {
    val content = new Spider().getRssFeed(url, fd) //implicit dependency on metadata
    val dom = new SAXBuilder().build(
      new StringReader(content)
    )
    new SyndFeedInput().build(dom)
  }
  
  def update_feed( fd1:RSSFeed, fd:FeedDescriptor ){
    val lastupdate_til = fd.last_entry_url
    val old_entries = root.getEntries().asScala.map(_.asInstanceOf[SyndEntry])
    for( synd <- fd1.rssItems ){
      //assumptions should be made that the entries are sorted
      
      if ( synd.getLink() == lastupdate_til ){
        return
      }
      else{
        old_entries += synd
      }
    }
  }
   
  lazy val rssItems: Iterable[SyndEntry] = root.getEntries().asScala.map(_.asInstanceOf[SyndEntry])
   
  override def toString = s"Feed($url)"
}

object RSSFeed {
  def apply(fd: FeedDescriptor) = new RSSFeed(fd)
}
