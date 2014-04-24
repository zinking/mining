package mining.io

import java.util.Date
import mining.util.UrlUtil
import mining.parser.FeedParser
import scala.collection.mutable

/**
 * It's allowed to use URL only as constructor to facilitate testing. 
 * But in real world the feed descriptor should be retrieved from FeedManager, to keep it in sync.
 */
case class Feed(val url: String,
                var feedId: Long,
                var lastEtag: String,
                var checked: Date,
                var lastUrl: String,
                var encoding: String) {
  /** OPML outline for the feed */
  var outline = OpmlOutline.empty()

  /** Stories sync from RSS but not persisted yet */
  val unsavedStories = mutable.ListBuffer.empty[Story]

  /** Sync new stories from RSS feed */
  def sync() = this.synchronized {
    unsavedStories ++= FeedParser(this).syncFeed()
  }
  
  /** Unique id generated from the feed URL */
  def uid = UrlUtil.urlToUid(url)
  
  override def toString = s"FeedDescriptor[$url]"
}

object FeedFactory {
  def newFeed(url: String) = new Feed(url, 0L, "", new Date(0), "", "UTF-8")
}

