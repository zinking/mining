package mining.io

import java.util.Date
import mining.util.UrlUtil
import mining.parser.FeedParser
import scala.collection.mutable

/**
 * It's allowed to use URL only as constructor to facilitate testing. 
 * But in real world the feed descriptor should be retrieved from FeedManager, to keep it in sync.
 */
case class Feed(xmlUrl: String,
                title: String,
                text: String,
                htmlUrl: String,
                feedType: String,
                feedId: Long,
                lastEtag: String,
                checked: Date,
                lastUrl: String,
                encoding: String) {
    /** OPML outline for the feed */
    var outline = OpmlOutline.empty()

    /** Stories sync from RSS but not persisted yet */
    val unsavedStories = mutable.ListBuffer.empty[Story]

    /** Unique id generated from the feed URL */
    def uid = UrlUtil.urlToUid(xmlUrl)

    override def toString = s"FeedDescriptor[$xmlUrl]"
}

object FeedFactory {
    def newFeed(url: String) = new Feed(url,"","",url,"RSS", 0L, "", new Date, "", "UTF-8")
}

