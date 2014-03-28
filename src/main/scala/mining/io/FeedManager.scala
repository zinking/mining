package mining.io

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem

import mining.util.UrlUtil

trait FeedManager {
  
  /** Map from Feed UID to Feed Descriptor */
  def feedsMap: mutable.Map[String, Feed]
  
  /** Persist current feed descriptors */
  def saveFeed(feed: Feed)
  
  /** Get the descriptor from feed URL */
  def loadFeedFromUrl(url: String): Option[Feed] = feedsMap.get(UrlUtil.urlToUid(url))
  
  /** Get the descriptor from feed UID */
  def loadFeedFromUid(uid: String): Option[Feed] = feedsMap.get(uid) 
  
  /** Load the map of all the feed descriptors */
  def loadFeeds(): mutable.Map[String, Feed]
  
  /** Create a new feed if the UID of the URL doesn't exist. Sync and persist after that */
  def createOrUpdateFeed(url: String): RSSFeed 
  
  /** Create a new feed descriptor if it doesn't exist. Also sync to ser file. */
  def createOrGetFeedDescriptor(url: String): Feed = {
    loadFeedFromUrl(url) match {
      case None => {
        val feed = FeedFactory.newFeed(url) 
        feedsMap += UrlUtil.urlToUid(url) -> feed
        saveFeed(feed)
      }
      case _ =>
    }
    loadFeedFromUrl(url).get 
  }
  
  /** Check all feeds in OPML file */
  def createOrUpdateFeedOPML(root: Elem) = {
    val rssOutNodes = root \\ "outline" filter{node => (node \ "@type").text == "rss"}
    for (rssOutNode <- rssOutNodes) {
      val url = (rssOutNode \ "@xmlUrl").text 
      Future{ createOrUpdateFeed(url) }
    }
  }
}

