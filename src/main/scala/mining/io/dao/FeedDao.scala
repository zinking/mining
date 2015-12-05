package mining.io.dao

import java.sql._

import com.mysql.jdbc.PreparedStatement
import com.typesafe.config.{ConfigFactory, Config}
import mining.io._
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
 * Created by awang on 5/12/15.
 */
object FeedDao {
    def apply(db:String) = {
        val conf:Config = ConfigFactory.load
        val connection = JdbcConnectionFactory(conf.getConfig(db)).getPooledConnection()
        new FeedDao(connection)
    }

    def resultToFeed(r:ResultSet): Feed ={
        Feed(
            r.getString("URL"),
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

class FeedDao(connection:Connection) extends Dao
with FeedManager
with FeedWriter
with FeedReader {
    import FeedDao._
    override def log: Logger = LoggerFactory.getLogger(classOf[FeedDao])

    /** Map from Feed UID to Feed Descriptor */
    override lazy val feedsMap = loadFeeds()
    override def loadFeeds() = {
        val map = mutable.Map.empty[String, Feed]
        getAllFeeds.map { f =>
            map += (UrlUtil.urlToUid(f.url) -> f)
        }
        map
    }

    override def write(feed: Feed) = {
        feed.synchronized {
            insertOrUpdateFeed(feed)
            for(story<-feed.unsavedStories){
                insertFeedStory(feed,story)
            }
            feed.unsavedStories.clear()
        }
    }

    override def createOrUpdateFeed(url: String): Feed = {
        val feed = createOrGetFeedDescriptor(url)
        feed.sync()
        write(feed)
        feedsMap += UrlUtil.urlToUid(feed.url) -> feed
        feed
    }

    override def read(feed: Feed, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
        val q = s"select * from FEED_STORY where feed_id=${feed.feedId} order by updated desc limit ${(pageNo)*pageSize},$pageSize "
        val result = new util.ArrayList[Story]
        using(connection.prepareStatement(q)) { statement =>
            using(statement.executeQuery(q)) { rs =>
                while (rs.next) {
                    val story = resultToStory(rs)
                    result.add(story)
                }
            }
        }
        result.asScala.toList
    }



    def getAllFeeds:Iterable[Feed] = {
        val q = "select * from FEED_SOURCE "
        val result = new util.ArrayList[Feed]
        using(connection.prepareStatement(q)) { statement =>
            using(statement.executeQuery(q)) { rs =>
                while (rs.next) {
                    val feed = resultToFeed(rs)
                    result.add(feed)
                }
            }
        }
        result.asScala.toList
    }

    def getAllFutureFeeds:Future[Iterable[Feed]] = Future{
        val q = "select * from FEED_SOURCE "
        val result = new util.ArrayList[Feed]
        using(connection.prepareStatement(q)) { statement =>
            using(statement.executeQuery(q)) { rs =>
                while (rs.next) {
                    val feed = resultToFeed(rs)
                    result.add(feed)
                }
            }
        }
        result.asScala.toList
    }


    def insertOrUpdateFeed(feed:Feed):Feed={
        if(feed.feedId<=0){
            insertFeed(feed)
        }
        else{
            updateFeed(feed)
        }
    }
    private def updateFeed(feed:Feed):Feed={
        val q = s"update FEED_SOURCE set url=?,last_etag=?,checked=?,last_url=?,encoding=? where feed_id = ${feed.feedId}"
        using(connection.prepareStatement(q)) { statement =>
            statement.setString(1,feed.url)
            statement.setString(2,feed.lastEtag)
            statement.setTimestamp(3, new Timestamp(feed.checked.getTime))
            statement.setString(4,feed.lastUrl)
            statement.setString(5,feed.encoding)
            statement.executeUpdate()
        }
        feed
    }

    private def insertFeed(feed:Feed):Feed={
        val q = "INSERT INTO FEED_SOURCE (URL,LAST_ETAG,CHECKED,LAST_URL,ENCODING) VALUES (?,?,?,?,?)"
        using(connection.prepareStatement(q,Statement.RETURN_GENERATED_KEYS)) { statement =>
            statement.setString(1,feed.url)
            statement.setString(2,feed.lastEtag)
            statement.setTimestamp(3, new Timestamp(feed.checked.getTime))
            statement.setString(4,feed.lastUrl)
            statement.setString(5,feed.encoding)
            statement.executeUpdate()
            val newFeedIdRS = statement.getGeneratedKeys
            if(newFeedIdRS.next){
                feed.feedId = newFeedIdRS.getLong(1)
            } else{
                throw new SQLException("Feed Insertion failed")
            }
        }
        feed
    }

    def insertFeedStory(feed:Feed,story:Story):Story = {
        val q = "INSERT INTO FEED_STORY (feed_id,title,link,published,updated,author,description,content) VALUES (?,?,?,?,?,?,?,?)"
        using(connection.prepareStatement(q,Statement.RETURN_GENERATED_KEYS)) { statement =>
            statement.setLong(1,feed.feedId)
            statement.setString(2,story.title)
            statement.setString(3,story.link)
            statement.setTimestamp(4, new Timestamp(story.published.getTime))
            statement.setTimestamp(5, new Timestamp(story.updated.getTime))
            statement.setString(6,story.author)
            statement.setString(7,story.description)
            statement.setString(8,story.content)
            statement.executeUpdate()
            val newStoryIdRS = statement.getGeneratedKeys
            if(newStoryIdRS.next){
                story.id = newStoryIdRS.getLong(1)
            } else{
                throw new SQLException("story Insertion failed")
            }
        }
        story
    }


    def getOpmlStories(opml: Opml, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
        opml.allFeedsUrl.foldLeft(List[Story]())((acc, node) => {
            val fd = loadFeedFromUrl(node)
            fd match {
                case Some(feed) =>
                    val ss = read(feed,pageSize,pageNo)
                    acc ++ ss
                case _ => acc
            }
        })
    }

    def getFeedStories(feedUrl: String, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
        //val fd = loadFeedFromUrl(feedUrl)
        val fd = feedsMap.get(UrlUtil.urlToUid(feedUrl))
        fd match {
            case Some(feed) =>
                read(feed,pageSize,pageNo)
            case None => List.empty[Story]
        }
    }

    def getStoryById(storyId: Long): Story = {
        val q = "select * from FEED_STORY where story_id=?"
        val result = new util.ArrayList[Story]
        using(connection.prepareStatement(q)) { statement =>
            statement.setLong(1,storyId)
            using(statement.executeQuery()) { rs =>
                while (rs.next) {
                    val story = resultToStory(rs)
                    result.add(story)
                }
            }
        }
        result.get(0)
    }

    def getStoryByLink(sl: String): Story = {
        val q = "select * from FEED_STORY where link=?"
        val result = new util.ArrayList[Story]
        using(connection.prepareStatement(q)) { statement =>
            statement.setString(1,sl)
            using(statement.executeQuery()) { rs =>
                while (rs.next) {
                    val story = resultToStory(rs)
                    result.add(story)
                }
            }
        }
        result.get(0)
    }
}
