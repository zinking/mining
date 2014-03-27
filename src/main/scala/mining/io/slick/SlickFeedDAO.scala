package mining.io.slick

import scala.slick.driver.JdbcProfile
import java.sql.Date
import scala.xml.Elem
import mining.io.FeedManager
import scala.collection.mutable
import mining.io.Feed
import mining.io.Feed
import mining.io.Story
import mining.util.UrlUtil

class SlickFeedDAO(override val profile: JdbcProfile) extends SlickDBConnection(profile) with FeedManager {
  import profile.simple._
  
  val stories = TableQuery[FeedStory]

  val feeds = TableQuery[FeedSource]
  
  def manageDDL() = {
    val tablesMap = SlickUtil.tablesMap(this)
    if (!tablesMap.contains("FEED_SOURCE")) database.withSession(implicit session => feeds.ddl.create)
    if (!tablesMap.contains("FEED_ENTRY")) database.withSession(implicit session => stories.ddl.create)
  }
  
  override lazy val feedsMap = loadFeeds() 

  override def loadFeeds() = database withSession { implicit session =>
    val map = mutable.Map.empty[String, Feed] 
    feeds.list.foreach(f => map += (UrlUtil.urlToUid(f.url) -> f))
    map
  }
  
  override def saveFeed(feed: Feed) = {
    database withTransaction { implicit session =>
      feed.feedId match {
        case 0L => feed.feedId = (feeds returning feeds.map(_.feedId)) += feed
        case _ => feeds.filter(_.feedId == feed.feedId).update(feed)
      }
    }
  }
  
  override def createOrUpdateFeed(url: String) = ???

  class FeedSource(tag: Tag) extends Table[Feed](tag, "FEED_SOURCE") {
    def feedId = column[Long]("FEED_ID", O.PrimaryKey, O.AutoInc)
    def url = column[String]("URL")
    def lastEtag = column[String]("LAST_ETAG")
    def checked = column[Date]("CHECKED")
    def lastUrl = column[String]("LAST_URL")
    def encoding = column[String]("ENCODING")
  
    def * = (url, feedId, lastEtag, checked, lastUrl, encoding) <> (Feed.tupled, Feed.unapply)
  }    

  class FeedStory(tag: Tag) extends Table[Story](tag, "FEED_STORY") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def feedId = column[Long]("FEED_ID")
    def title = column[String]("TITLE")
    def link = column[String]("LINK")
    def published = column[Date]("PUBLISHED")
    def updated = column[Date]("UPDATED")
    def author = column[String]("AUTHOR")
    def description = column[String]("DESCRIPTION")
    def content = column[String]("CONTENT")

    def feedFK = foreignKey("ENTRY_FK", feedId, feeds)(_.feedId)
    def * = (id, feedId, title, link, published, updated, author, description, content) <> (Story.tupled, Story.unapply)
  }
  
}

object SlickFeedDAO {
  def apply(profile: JdbcProfile) = new SlickFeedDAO(profile)
}