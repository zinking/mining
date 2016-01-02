package mining.parser

import java.io.ByteArrayInputStream
import java.io.StringReader

import scala.collection.JavaConverters._

import org.jdom.input.SAXBuilder
import org.slf4j.LoggerFactory

import com.sun.syndication.feed.synd.SyndEntry
import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.io.SyndFeedInput

import mining.io.Feed
import mining.io.OpmlOutline
import mining.io.Story
import mining.io.StoryFactory

class FeedParser(val feed: Feed) {
    private val logger = LoggerFactory.getLogger(classOf[FeedParser])


    val url = feed.xmlUrl

    /** Sync latest stories which are not in current feed */
    def syncFeed(): Feed = {
        val (newFeed, content) = Spider().syncFeedForContent(feed)
        val newSyndFeed = syndFeedFromXML(content)

        /*title,xmlUrl,outType,text,htmlUrl*/
        newFeed.outline = OpmlOutline(newSyndFeed.getTitle, url, newSyndFeed.getFeedType,
            newSyndFeed.getDescription, newSyndFeed.getLink)

        val parsedEntries = newSyndFeed.getEntries.asScala.map(_.asInstanceOf[SyndEntry])
        val parsedStories = parsedEntries.map(StoryFactory.fromSyndFeed(_, feed))
        newFeed.unsavedStories ++= parsedStories

        newFeed
    }

    /** Getting a Rome SyndFeed object from XML */
    protected def syndFeedFromXML(feedXML: String): SyndFeed = {
        try {
            val builder = new SAXBuilder()
            val input = new ByteArrayInputStream(feedXML.getBytes(feed.encoding))
            val dom = builder.build(input)
            new SyndFeedInput().build(dom)
        }
        catch {
            //TODO:DOM exception for cynergysystems caused by the rss url has changed
            //TODO: SOME DOMAIN CANNOT BE SEEN WITHIN CHINA http://blogs.nitobi.com, these should be captured by spider
            case ex: Throwable => logger.error(s"Parsing Exception for $url with $ex SKIPPING")
                val dom = new SAXBuilder().build(new StringReader(Spider.EMPTY_RSS_FEED))
                new SyndFeedInput().build(dom)
        }
    }

    override def toString = s"Feed($url)"
}

object FeedParser {
    def apply(feed: Feed) = new FeedParser(feed)
}