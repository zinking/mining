package mining.io.slick

import java.sql.Timestamp
import java.util.Date

import scala.collection.mutable
import scala.slick.driver.JdbcProfile

import mining.io._
import mining.parser.FeedParser
import mining.util.UrlUtil

class SlickFeedDAO(override val profile: JdbcProfile) 
  extends SlickDBConnection(profile) 
  with FeedManager
  with FeedWriter 
  with FeedReader {
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
  
  override def write(feed: Feed) = database withTransaction { implicit session =>
    feed.synchronized {
      //Persist feed info
      feed.feedId match {
        case 0L => feed.feedId = (feeds returning feeds.map(_.feedId)) += feed
        case _  => feeds.filter(_.feedId === feed.feedId).update(feed)
      }
      //Persist unsaved stories
      stories.insertAll(feed.unsavedStories.toSeq: _*)
      feed.unsavedStories.clear()
    }
  }
  
  override def createOrUpdateFeed(url: String): Feed = {
    val feed = createOrGetFeedDescriptor(url)
    feed.sync()
    write(feed) 
    feed
  }

  override def read(feed: Feed, count: Int = Int.MaxValue): Iterable[Story] = 
    database withTransaction { implicit session =>
      stories.filter(_.feedId === feed.feedId).list.take(count)
    }
  
  def getOpmlStories(opml:Opml): List[Story] = {
    database withTransaction { implicit session =>
	    opml.allFeeds.foldLeft[List[Story]]( List[Story]() )(( acc, node ) =>{
	       //val ss = stories.where( _.feedId === UrlUtil.urlToUid(node.xmlUrl) ).take(10) ???
	       //TODO: FEEDID FK need to be adapted , why long though?
	      val ss = stories.where( _.feedId === 0l ).take(10)
	       //val ss = stories.list.take(10)
	       acc ++ ss.buildColl
	     })
    }
  }
  
  def getStoryById( storyId:String ):Story = {
    database withTransaction { implicit session =>
      stories.filter( _.link === storyId ).first
    }
  }
  
  def getStoryContentById( storyId:String ):String = {
    database withTransaction { implicit session =>
      stories.filter( _.link === storyId ).first.content
    }
  }
  
  //Implicitly map j.u.Date to Timestamp for the following column definitions
  implicit def dateTime = MappedColumnType.base[Date, Timestamp](
    dt => new Timestamp(dt.getTime),
    ts => new Date(ts.getTime)
  )

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