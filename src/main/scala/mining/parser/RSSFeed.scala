package mining.parser

import java.io.StringReader
import org.jdom.input.SAXBuilder
import com.sun.syndication.io.SyndFeedInput
import com.sun.syndication.feed.synd.SyndEntry
import scala.collection.JavaConverters._
import mining.io.FeedDescriptor
import scala.collection.mutable
import scala.math.Ordering

class RSSFeed(val fd: FeedDescriptor) {
  import SyndEntryOrdering._

  val url = fd.feedUrl
  
  val rssItems = mutable.SortedSet.empty[SyndEntry]

  /** Sync latest feeds */
  def syncFeed(): Unit = { 
    val content = new Spider().getRssFeed(url, fd) //implicit dependency on metadata
    val newSyndFeed = syndFeedFromXML(content)
    
    val lastUpdateUrl = fd.lastEntryUrl
    val newEntries = newSyndFeed.getEntries().asScala.map(_.asInstanceOf[SyndEntry])
    
    for(synd <- newEntries){
      if (synd.getLink() == lastUpdateUrl){
        return
      }
      else{
        rssItems += synd
      }
    }
  }
  
  def addFeedEntries(entries: Iterable[SyndEntry]) = rssItems ++= entries
  
  protected[parser] def syndFeedFromXML(feedXML: String) = {
    val dom = new SAXBuilder().build(
      new StringReader(feedXML)
    )
    new SyndFeedInput().build(dom)
  }
   
  override def toString = s"Feed($url)"
}

object RSSFeed {
  def apply(fd: FeedDescriptor) = new RSSFeed(fd)
}

object SyndEntryOrdering {
  implicit def syndEntryOrdering: Ordering[SyndEntry] = Ordering.fromLessThan((x, y) => x.getPublishedDate().compareTo(y.getPublishedDate()) > 0)
}
