package mining.parser

import java.io.StringReader
import org.jdom.input.SAXBuilder
import com.sun.syndication.io.SyndFeedInput
import com.sun.syndication.feed.synd.SyndEntry
import scala.collection.JavaConverters._
import mining.io.FeedDescriptor
import scala.collection.mutable
import scala.math.Ordering
import org.jdom.input.JDOMParseException
import org.slf4j.LoggerFactory
import com.sun.syndication.feed.synd.SyndFeed

class RSSFeed(val fd: FeedDescriptor) {
  //import SyndEntryOrdering._
  
  private val logger = LoggerFactory.getLogger(classOf[RSSFeed])

  val url = fd.feedUrl
  
  //val rssItems = mutable.SortedSet.empty[SyndEntry]
  val rssItems = mutable.ListBuffer.empty[SyndEntry]

  /** Sync latest feeds */
  def syncFeed(fd:FeedDescriptor){ 
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
    
    if( newEntries.length > 0 ) {//update fid
      fd.lastEntryUrl = newEntries(0).getLink()
    }
  }
  
  def addFeedEntries(entries: Iterable[SyndEntry]) = rssItems ++= entries
  
  protected[parser] def syndFeedFromXML(feedXML: String):SyndFeed = {
    try{
	    val dom = new SAXBuilder().build(
	      new StringReader(feedXML)
	    )
	    return new SyndFeedInput().build(dom)
    }
    catch{
      //TODO:DOM exception for cynergysystems caused by the rss url has changed
      //TODO: SOME DOMAIN CANNOT BE SEEN WITHIN CHINA http://blogs.nitobi.com, these should be caputred by spider
      case ex: JDOMParseException => logger.error(s"Parsing Exception with $url SKIPPING")
    }
    
    val dom = new SAXBuilder().build(
      new StringReader(Spider.EMPTY_RSS_FEED)
    )
    new SyndFeedInput().build(dom)
  }
   
  override def toString = s"Feed($url)"
}

object RSSFeed {
  def apply(fd: FeedDescriptor) = new RSSFeed(fd)
}

//TODO: disable for the moment as not supported by all
//if we maintain the order we retrieved, we should be fine.
//object SyndEntryOrdering {
//  implicit def syndEntryOrdering: Ordering[SyndEntry] = Ordering.fromLessThan((x, y) => x.getPublishedDate().compareTo(y.getPublishedDate()) > 0)
//}
