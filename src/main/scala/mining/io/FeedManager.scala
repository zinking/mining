package mining.io

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem

import mining.util.UrlUtil

trait FeedManager extends FeedReader with FeedWriter {
  
  /** Map from Feed UID to Feed Descriptor */
  def feedsMap: mutable.Map[String, Feed]
  
  /** Get the descriptor from feed URL */
  def loadFeedFromUrl(url: String): Option[Feed] = feedsMap.get(UrlUtil.urlToUid(url))
  
  /** Get the descriptor from feed UID */
  def loadFeedFromUid(uid: String): Option[Feed] = feedsMap.get(uid) 
  
  /** Load the map of all the feed descriptors */
  def loadFeeds(): mutable.Map[String, Feed]
  
  /** Create a new feed if the UID of the URL doesn't exist. Sync and persist after that */
  def createOrUpdateFeed(url: String): Feed 
  
  /** Create a new feed descriptor if it doesn't exist. */
  def createOrGetFeedDescriptor(url: String): Feed = {
    loadFeedFromUrl(url) match {
      case None => {
        val feed = FeedFactory.newFeed(url) 
        feedsMap += UrlUtil.urlToUid(url) -> feed
        write(feed)
      }
      case _ =>
    }
    loadFeedFromUrl(url).get 
  }
  
  /** Check all feeds in OPML file */
  def createOrUpdateFeedOPML(opml: Opml) = {
    for (url <- opml.allFeedsUrl) {
      Future{ createOrUpdateFeed(url) } //TODO: This method should return a future for client to know
    }
  }
}

