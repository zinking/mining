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
            r.getString("ENCODING"),
            r.getLong("VISIT_COUNT"),
            r.getLong("UPDATE_COUNT"),
            r.getLong("REFRESH_COUNT"),
            r.getLong("REFRESH_ITEMCOUNT"),
            r.getLong("ERROR_COUNT"),
            r.getLong("AVG_REFRESH_DURATION")
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
    override def createOrUpdateFeed(url: String): Future[Option[Feed]] = {
        loadFeedFromUrl(url) match {
            case None =>
                val feed = FeedFactory.newFeed(url)
                val newFeedFuture = FeedParser(feed).syncFeed()
                newFeedFuture.map{ newFeed =>
                    // all stories aree new, as this is firstly insert
                    val newStories = newFeed.unsavedStories
                    log.info(
                        s"$url totally parsed {} stories, persisted {} new stories",
                        newStories.size,
                        newStories.size
                    )

                    if (newStories.nonEmpty) {
                        val ffinalFeed = newFeed.copy(
                            lastUrl = newStories.headOption.map(_.link).getOrElse(""),
                            checked = new Date,
                            visitCount = newFeed.visitCount+1,
                            updateCount = newFeed.updateCount+1,
                            refreshCount = newFeed.refreshCount+1,
                            refreshItemCount = newFeed.refreshItemCount+newStories.size
                        )
                        ffinalFeed.unsavedStories ++= newStories
                        val efeed = write(ffinalFeed)
                        Some(efeed)
                    }
                    else{
                        // nothing gets persist as it could be non valid feed stuff
                        None
                    }

                }

            case Some(feed) =>
                val feedFuture = FeedParser(feed).syncFeed()
                feedFuture.map{ updatedFeed=>
                    val newStories = getOnlyNewStories(updatedFeed.unsavedStories,feed.lastUrl)
                    log.info(
                        s"${feed.xmlUrl} totally parsed {} stories, persisted {} new stories",
                        updatedFeed.unsavedStories.size,
                        newStories.size
                    )
                    newStories.headOption.map(_.link) match{
                        case Some(newLastUrl) =>
                            //update average refresh duration
                            val totalRefreshDuration = updatedFeed.avgRefreshDuration * updatedFeed.refreshCount
                            val thisRefreshDuration = new Date().getTime - updatedFeed.checked.getTime
                            val newAverage:Long = (totalRefreshDuration+thisRefreshDuration)/(updatedFeed.refreshCount+1)

                            val finalFeed = updatedFeed.copy(
                                lastUrl = newLastUrl,
                                checked = new Date,
                                visitCount = updatedFeed.visitCount+1,
                                updateCount = updatedFeed.updateCount+1,
                                refreshCount = updatedFeed.refreshCount+1,
                                avgRefreshDuration = newAverage,
                                refreshItemCount = updatedFeed.refreshItemCount+newStories.size
                            )
                            finalFeed.unsavedStories.clear()
                            finalFeed.unsavedStories ++= newStories

                            val efeed = write(finalFeed)
                            Some(efeed)
                        case None =>
                            val finalFeed = updatedFeed.copy(
                                checked = new Date,
                                visitCount = updatedFeed.visitCount+1,
                                updateCount = updatedFeed.updateCount+1
                            )
                            Some(finalFeed)
                    }
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
        val q = s"update FEED_SOURCE set xml_url=?,last_etag=?,checked=?,last_url=?,encoding=?,visit_count=?," +
                s"update_count=?,refresh_count=?,error_count=?,avg_refresh_duration=?,refresh_itemcount=? " +
                s"where feed_id = ${feed.feedId}"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, feed.xmlUrl)
                statement.setString(2, feed.lastEtag)
                statement.setTimestamp(3, new Timestamp(feed.checked.getTime))
                statement.setString(4, feed.lastUrl)
                statement.setString(5, feed.encoding)

                statement.setLong(6, feed.visitCount)
                statement.setLong(7, feed.updateCount)
                statement.setLong(8, feed.refreshCount)
                statement.setLong(9, feed.errorCount)
                statement.setLong(10, feed.avgRefreshDuration)
                statement.setLong(11, feed.refreshItemCount)
                statement.executeUpdate()
            }
        }
        feed
    }

    private def insertFeed(feed:Feed):Feed={
        val q = "INSERT INTO FEED_SOURCE (XML_URL,HTML_URL,TITLE,TEXT,FEED_TYPE,LAST_ETAG,CHECKED,LAST_URL,ENCODING," +
                "VISIT_COUNT, UPDATE_COUNT,REFRESH_COUNT,ERROR_COUNT,AVG_REFRESH_DURATION,REFRESH_ITEMCOUNT" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
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

                statement.setLong(10, feed.visitCount)
                statement.setLong(11, feed.updateCount)
                statement.setLong(12, feed.refreshCount)
                statement.setLong(13, feed.errorCount)
                statement.setLong(14, feed.avgRefreshDuration)
                statement.setLong(15, feed.refreshItemCount)
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
