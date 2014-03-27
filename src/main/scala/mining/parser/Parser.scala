package mining.parser

import org.jdom.input.SAXBuilder
import java.io.ByteArrayInputStream
import mining.io.Feed
import com.sun.syndication.io.SyndFeedInput
import com.sun.syndication.feed.synd.SyndFeed
import org.slf4j.LoggerFactory
import java.io.StringReader
import scala.collection.JavaConverters._
import com.sun.syndication.feed.synd.SyndEntry
import mining.io.Story
import mining.io.StoryFactory

class Parser(val feed: Feed) {
  private val logger = LoggerFactory.getLogger(classOf[Parser])
  
  val url = feed.url
  
   /** Sync latest feeds */
  def syncFeed(): Iterable[Story]= { 
    val content = Spider().getFeedContent(feed) 
    val newSyndFeed = syndFeedFromXML(content)
    
    val newEntries = newSyndFeed.getEntries().asScala.map(_.asInstanceOf[SyndEntry])
    val newStories = newEntries.map(StoryFactory.fromSyndFeed(_, feed)).takeWhile(_.link != feed.lastUrl)

    newStories.headOption match {
      case Some(story) => feed.lastUrl = story.link
      case None => 
    } 

    newStories
  }

  protected def syndFeedFromXML(feedXML: String): SyndFeed = {
    try {
    	val builder = new SAXBuilder()
    	val input = new ByteArrayInputStream(feedXML.getBytes(feed.encoding));
	    val dom = builder.build(input)
	    new SyndFeedInput().build(dom)
    }
    catch {
      //TODO:DOM exception for cynergysystems caused by the rss url has changed
      //TODO: SOME DOMAIN CANNOT BE SEEN WITHIN CHINA http://blogs.nitobi.com, these should be caputred by spider
      case ex: Throwable => logger.error(s"Parsing Exception for $url with $ex SKIPPING")
      val dom = new SAXBuilder().build(new StringReader(Spider.EMPTY_RSS_FEED))
      new SyndFeedInput().build(dom)
    }
  }

}

object Parser {
  def apply(feed: Feed) = new Parser(feed)
}