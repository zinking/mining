package mining.io

import scala.collection.mutable
import org.slf4j.LoggerFactory
import com.sun.syndication.feed.synd.SyndEntry
import mining.parser.Parser

class RSSFeed(val feed: Feed) {
  
  private val logger = LoggerFactory.getLogger(classOf[RSSFeed])

  val url = feed.url
  
  val stories = mutable.ListBuffer.empty[Story]

  def syncFeed(): Unit = stories ++= Parser(feed).syncFeed()
  
  def addFeedEntries(entries: Iterable[Story]) = stories ++= entries
   
  override def toString = s"Feed($url)"
}

object RSSFeed {
  def apply(fd: Feed) = new RSSFeed(fd)
}
