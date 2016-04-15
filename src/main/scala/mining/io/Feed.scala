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
                encoding: String,
                visitCount: Long,
                updateCount: Long,
                refreshCount: Long,
                refreshItemCount: Long,
                errorCount: Long,
                avgRefreshDuration: Long) {
    /** OPML outline for the feed */

    def getOpmlOutline: OpmlOutline = {
      OpmlOutline(
        List.empty,
        title,
        xmlUrl,
        feedType,
        text,
        htmlUrl
      )
    }

    /** Stories sync from RSS but not persisted yet */
    val unsavedStories = mutable.ListBuffer.empty[Story]

    /** Unique id generated from the feed URL */
    def uid = UrlUtil.urlToUid(xmlUrl)

    def getStatsString =
    s"$xmlUrl visitCount:$visitCount, updateCount:$updateCount, " +
    s"refreshCount:$refreshCount, errorCount:$errorCount, " +
    s"avgRefreshDuration:$avgRefreshDuration refreshItemCount:$refreshItemCount"

    override def toString = s"FeedDescriptor[$xmlUrl]"
}

object FeedFactory {
  /**
   * create new empty feed using only url
   * @param url url of the feed
   * @return empty feed
   */
    def newFeed(url: String) = new Feed(
        url,"","",url,"RSS", 0L, "", new Date, "", "UTF-8",
        0, 0, 0, 0, 0, 0
    )


  /**
   * function object to convert an flat opmloutline to feed
   */
    val outline2Feed: OpmlOutline=>Feed = (outline:OpmlOutline) => {
        new Feed(
            outline.xmlUrl, outline.title,
            outline.text, outline.htmlUrl,
            outline.outlineType, 0L, "",
            new Date, "", "UTF-8", 0, 0, 0, 0, 0, 0
        )
    }

  /**
   * map an opmloutline to a list of feeds
   * flat opml outline strucutre will be mapped to list with single element
   * folder opml outline structure will be mapped to list of feeds
   * @param outline the opml outline
   * @return a list of feeeds
   */
    def newFeeds(outline: OpmlOutline): List[Feed] = {
        if (outline.outlines.isEmpty) {
            List( outline2Feed(outline) )
        } else {
            outline.outlines.map{ outline=>
                outline2Feed(outline)
            }
        }
    }

  /**
   * create a list feed using opml object
   * @param opml opml object
   * @return a list of feeds
   */
    def newFeeds(opml: Opml): List[Feed] = {
        opml.outlines.flatMap { outline =>
          newFeeds(outline)
        }
    }
}

