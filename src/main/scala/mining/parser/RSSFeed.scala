package mining.parser

import java.io.StringReader
import org.jdom.input.SAXBuilder
import com.sun.syndication.io.SyndFeedInput
import com.sun.syndication.feed.synd.SyndEntry
import scala.collection.JavaConverters._
import mining.io.FeedDescriptor

class RSSFeed(val fd: FeedDescriptor) {
  val url = fd.feedUrl

  var root = {
    //val content = new Spider().getRssFeed(url, fd) //implicit dependency on metadata
    val dom = new SAXBuilder().build(
      new StringReader(Spider.empty_rssfeed)
    )
    new SyndFeedInput().build(dom)
  }
  
  def sync_feed( fd:FeedDescriptor ){//sync latest feed 
    
    val content = new Spider().getRssFeed(url, fd) //implicit dependency on metadata
    val dom = new SAXBuilder().build(
      new StringReader(content)
    )
    val fd1 = new SyndFeedInput().build(dom)
    
    val lastupdate_til = fd.last_entry_url
    val old_entries = root.getEntries().asScala.map(_.asInstanceOf[SyndEntry])
    val new_entries = fd1.getEntries().asScala.map(_.asInstanceOf[SyndEntry])
    
    root = fd1 //update feed
    for( synd <- new_entries ){
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
