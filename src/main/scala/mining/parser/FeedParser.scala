package mining.parser

import java.io.ByteArrayInputStream
import java.util.Date

import mining.exception.{PageNotChangedException, ServerErrorException, ServerNotExistException, InvalidFeedException}

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

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class FeedParser(val feed: Feed) {
    private val logger = LoggerFactory.getLogger(classOf[FeedParser])

    val url = feed.xmlUrl
    val spider = Spider()

    /**
     * Sync latest stories which are not in current feed
     * when calling sync feed, feed is always tried to be created
     * but it could be the case the xmlUrl is not a valid url, or not a valid feed
     * @return the feed if a valid one
     */
    def syncFeed(): Future[Feed] = {
        spider.syncFeedForContent(feed) map { response =>
            val content = spider.getResponseString(feed, response)
            val newEncoding = spider.getEncoding(feed, response)
            val newSyndFeed = syndFeedFromXML(content)
            val parsedEntries = newSyndFeed.getEntries.asScala.map(_.asInstanceOf[SyndEntry])
            val parsedStories = parsedEntries.map(StoryFactory.fromSyndFeed(_, feed))
            val newChecked = new Date
            if (feed.title.isEmpty) { //first time synced
                val newFeed = feed.copy(
                    title = newSyndFeed.getTitle,
                    text  = newSyndFeed.getDescription,
                    htmlUrl = newSyndFeed.getLink,
                    feedType = newSyndFeed.getFeedType,
                    encoding = newEncoding,
                    checked = newChecked
                )
                newFeed.unsavedStories ++= parsedStories
                newFeed
            }
            else {
                val updatedFeed = feed.copy(
                    encoding = newEncoding,
                    checked = newChecked
                )
                updatedFeed.unsavedStories ++= parsedStories
                updatedFeed
            }
        } recoverWith {
            case ex:PageNotChangedException =>
                Future.successful(
                    feed.copy(
                        visitCount = feed.visitCount+1,
                        errorCount = feed.updateCount+1
                    )
                )
            case ex: Throwable =>
                //logger.error("recover feed parsing {} {}", url, ex.getMessage, ex)
                Future.successful(
                    feed.copy(
                        visitCount = feed.visitCount+1,
                        errorCount = feed.errorCount+1
                    )
                )
        }
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
            case ex: Throwable =>
                logger.error(s"Parsing Exception {} as {} ", url, ex.getMessage, ex)
                throw InvalidFeedException(url)
        }
    }

    override def toString = s"FeedParser($url)"
}

object FeedParser {
    def apply(feed: Feed) = new FeedParser(feed)
}