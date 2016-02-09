package mining.io.dao

import java.sql._

import mining.io._
import mining.parser.FeedParser
import mining.util.UrlUtil
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import java.util
import java.util.Date

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls

/**
 * Feed database access object
 *
 * Created by awang on 5/12/15.
 */
object FeedDao {
    def apply() = {
        new FeedDao()
    }

    def resultToFeed(r:ResultSet): Feed ={
        Feed(
            r.getString("XML_URL"),
            r.getString("TITLE"),
            r.getString("TEXT"),
            r.getString("HTML_URL"),
            r.getString("FEED_TYPE"),
            r.getLong("FEED_ID"),
            r.getString("LAST_ETAG"),
            new Date(r.getTimestamp("CHECKED").getTime),
            r.getString("LAST_URL"),
            r.getString("ENCODING")
        )
    }

    def resultToStory(r:ResultSet):Story={
        Story(
            r.getLong("STORY_ID"),
            r.getLong("FEED_ID"),
            r.getString("TITLE"),
            r.getString("LINK"),
            new Date(r.getTimestamp("PUBLISHED").getTime),
            new Date(r.getTimestamp("UPDATED").getTime),
            r.getString("AUTHOR"),
            r.getString("DESCRIPTION"),
            r.getString("CONTENT")
        )
    }
}

class FeedDao() extends Dao
with FeedManager
with FeedWriter
with FeedReader {
    import FeedDao._
    override def log: Logger = LoggerFactory.getLogger(classOf[FeedDao])

    /** get the descriptor from feed url */
    override def loadFeedFromUrl(url: String): Option[Feed] = {
        val q = s"select * from FEED_SOURCE where XML_URL=?"
        val result = new util.ArrayList[Feed]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1,url)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val feed = resultToFeed(rs)
                        result.add(feed)
                    }
                }
            }
        }
        result.asScala.toList.headOption
    }

    /** Get the descriptor from feed UID */
    override def loadFeedFromUid(uid: String): Option[Feed] = {
        ???
    }

    /** Map from Feed UID to Feed Descriptor */
    //override lazy val feedsMap = loadFeeds()
    override def loadFeeds() = {
        val map = mutable.Map.empty[String, Feed]
        getAllFeeds.map { f =>
            //map += (UrlUtil.urlToUid(f.xmlUrl) -> f)
            map += (f.xmlUrl-> f)
        }
        map
    }

    override def write(feed: Feed): Feed = {
        feed.synchronized {
            val newFeed = insertOrUpdateFeed(feed)
            for(story<-feed.unsavedStories){
                insertFeedStory(newFeed,story)
            }
            feed.unsavedStories.clear()
            newFeed
        }
    }

    def getOnlyNewStories(parsedStories:mutable.ListBuffer[Story],lastUrl:String):mutable.ListBuffer[Story] = {
        //val sortedStories = parsedStories.sortBy(_.published)(Ordering[Date].reverse)
        //assumption is it's already reverse sorted
        parsedStories.takeWhile(_.link != lastUrl)
    }

    /**
     * create feed could return option non as url invalid or not properly processed by spider
     * @param url the url for the spider to fetch
     * @return option of the feed
     */
    override def createOrUpdateFeed(url: String): Option[Feed] = {
        loadFeedFromUrl(url) match {
            case None =>
                val feed = FeedFactory.newFeed(url)
                val newFeed = FeedParser(feed).syncFeed()
                val newStories = newFeed.unsavedStories
                log.info("parsed {} total stories",newFeed.unsavedStories.size)
                val finalFeed = newFeed.copy(
                    lastUrl = newStories.headOption.map(_.link).getOrElse(""),
                    checked = new Date
                )
                finalFeed.unsavedStories ++= newStories
                log.info("persisted {} new stories",newStories.size)
                if (newStories.nonEmpty) {
                    val feed = write(finalFeed)
                    Some(feed)
                }
                else{
                    None
                }

            case Some(feed) =>
                val newFeed = FeedParser(feed).syncFeed()
                log.info("parsed {} total stories",newFeed.unsavedStories.size)
                val newStories = getOnlyNewStories(newFeed.unsavedStories,feed.lastUrl)
                newStories.headOption.map(_.link) match{
                    case Some(newLastUrl) =>
                        val finalFeed = newFeed.copy(lastUrl=newLastUrl,checked = new Date)
                        finalFeed.unsavedStories.clear()
                        finalFeed.unsavedStories ++= newStories
                        log.info("persisted {} new stories",newStories.size)
                        val feed = write(finalFeed)
                        Some(feed)
                    case None =>
                        log.info("nothing to persist as nothing new")
                        val finalFeed = newFeed.copy(checked = new Date)
                        Some(finalFeed)
                }

        }
    }

    override def getStoriesFromFeed(feed: Feed, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
        val q = s"select * from FEED_STORY where feed_id=${feed.feedId} " +
        s"order by updated desc, story_id desc limit ${pageNo*pageSize},$pageSize "
        val result = new util.ArrayList[Story]
        using(JdbcConnectionFactory.getPooledConnection){connection=>
            using(connection.prepareStatement(q)) { statement =>
                using(statement.executeQuery(q)) { rs =>
                    while (rs.next) {
                        val story = resultToStory(rs)
                        result.add(story)
                    }
                }
            }
        }
        result.asScala.toList
    }



    def getAllFeeds:Iterable[Feed] = {
        val q = "select * from FEED_SOURCE "
        val result = new util.ArrayList[Feed]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                using(statement.executeQuery(q)) { rs =>
                    while (rs.next) {
                        val feed = resultToFeed(rs)
                        result.add(feed)
                    }
                }
            }
        }
        result.asScala.toList
    }



    def getAllFutureFeeds:Future[Iterable[Feed]] = Future{
        val q = "select * from FEED_SOURCE "
        val result = new util.ArrayList[Feed]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                using(statement.executeQuery(q)) { rs =>
                    while (rs.next) {
                        val feed = resultToFeed(rs)
                        result.add(feed)
                    }
                }
            }
        }
        result.asScala.toList
    }

    /**
     * verify the feed doesn't exist yet and create it
     * @param feed feed to be created
     * @return created feed in system
     */
    def verifyAndCreateFeed( feed:Feed ): Feed = {
        val feedOption = loadFeedFromUrl(feed.xmlUrl)
        feedOption match {
            case Some(fd) =>
                fd
            case None =>
                insertFeed(feed)
        }
    }

    def insertOrUpdateFeed(feed: Feed): Feed = {
        if (feed.feedId <= 0) {
            insertFeed(feed)
        }
        else {
            updateFeed(feed)
        }
    }
    private def updateFeed(feed:Feed):Feed={
        val q = s"update FEED_SOURCE set xml_url=?,last_etag=?,checked=?,last_url=?,encoding=? where feed_id = ${feed.feedId}"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, feed.xmlUrl)
                statement.setString(2, feed.lastEtag)
                statement.setTimestamp(3, new Timestamp(feed.checked.getTime))
                statement.setString(4, feed.lastUrl)
                statement.setString(5, feed.encoding)
                statement.executeUpdate()
            }
        }
        feed
    }

    private def insertFeed(feed:Feed):Feed={
        val q = "INSERT INTO FEED_SOURCE (XML_URL,HTML_URL,TITLE,TEXT,FEED_TYPE,LAST_ETAG,CHECKED,LAST_URL,ENCODING) VALUES (?,?,?,?,?,?,?,?,?)"
        val result = new util.ArrayList[Feed]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) { statement =>
                statement.setString(1, feed.xmlUrl)
                statement.setString(2, feed.htmlUrl)
                statement.setString(3, feed.title)
                statement.setString(4, feed.text)
                statement.setString(5, feed.feedType)
                statement.setString(6, feed.lastEtag)
                statement.setTimestamp(7, new Timestamp(feed.checked.getTime))
                statement.setString(8, feed.lastUrl)
                statement.setString(9, feed.encoding)
                statement.executeUpdate()
                val newFeedIdRS = statement.getGeneratedKeys
                if (newFeedIdRS.next) {
                    val newFeed = feed.copy(feedId=newFeedIdRS.getLong(1))
                    result.add(newFeed)
                } else {
                    throw new SQLException("Feed Insertion failed")
                }
            }
        }
        result.get(0)
    }

    def insertFeedStory(feed:Feed,story:Story):Story = {
        val q = "INSERT INTO FEED_STORY (feed_id,title,link,published,updated,author,description,content) VALUES (?,?,?,?,?,?,?,?)"
        val result = new util.ArrayList[Story]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) { statement =>
                statement.setLong(1, feed.feedId)
                statement.setString(2, story.title)
                statement.setString(3, story.link)
                statement.setTimestamp(4, new Timestamp(story.published.getTime))
                statement.setTimestamp(5, new Timestamp(story.updated.getTime))
                statement.setString(6, story.author)
                statement.setString(7, story.description)
                statement.setString(8, story.content)
                statement.executeUpdate()
                val newStoryIdRS = statement.getGeneratedKeys
                if (newStoryIdRS.next) {
                    val newStoryId = newStoryIdRS.getLong(1)
                    val newStory = story.copy(id=newStoryId)
                    result.add(newStory)
                } else {
                    throw new SQLException("story Insertion failed")
                }
            }
        }
        result.get(0)
    }


    def getOpmlStories(opml: Opml, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
        opml.allFeedsUrl.foldLeft(List[Story]())((acc, node) => {
            val fd = loadFeedFromUrl(node)
            fd match {
                case Some(feed) =>
                    val ss = getStoriesFromFeed(feed,pageSize,pageNo)
                    acc ++ ss
                case _ => acc
            }
        })
    }

    def getOpmlFeeds(opml: Opml): Iterable[Feed] = {
        opml.allFeedsUrl.foldLeft(List[Feed]())((acc, node) => {
            val fd = loadFeedFromUrl(node)
            fd match {
                case Some(feed) =>
                    acc :+ feed
                case _ => acc
            }
        })
    }

    def getFeedStories(feedUrl: String, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
        val fd = loadFeedFromUrl(feedUrl)
        fd match {
            case Some(feed) =>
                getStoriesFromFeed(feed,pageSize,pageNo)
            case None => List.empty[Story]
        }
    }

    def getStoryById(storyId: Long): Story = {
        val q = "select * from FEED_STORY where story_id=?"
        val result = new util.ArrayList[Story]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, storyId)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val story = resultToStory(rs)
                        result.add(story)
                    }
                }
            }
        }
        result.get(0)
    }

    def getStoryByLink(sl: String): Story = {
        val q = "select * from FEED_STORY where link=?"
        val result = new util.ArrayList[Story]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, sl)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val story = resultToStory(rs)
                        result.add(story)
                    }
                }
            }
        }
        result.get(0)
    }

    def getRawOutlineFromFeed(xmlUrl:String):Option[OpmlOutline] = {
        loadFeedFromUrl(xmlUrl).map{ feed=>
            OpmlOutline(List.empty,feed.title,feed.xmlUrl,feed.feedType,feed.text,feed.htmlUrl)
        }
    }
}
