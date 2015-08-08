package mining.io.slick

import scala.slick.driver.JdbcProfile
import mining.io.User
import mining.io.OpmlStorage
import java.sql.Blob
import mining.io.ReadStory
import java.util.Date
import mining.io.Feed
import java.sql.Timestamp
import mining.io.Story

class SlickUserFeedDDL(override val profile: JdbcProfile) 
  extends SlickDBConnection(profile) {
  import profile.simple._
  
  //Feed related queries
  val stories = TableQuery[FeedStory]
  val feeds = TableQuery[FeedSource]

  //User related queries
  val userInfo = TableQuery[UserInfo]
  val opmls = TableQuery[UserOpml]
  val userReadStories = TableQuery[UserReadStory]
   
  def manageDDL() = {
    val tablesMap = SlickUtil.tablesMap(this)
    try {
      database withTransaction { implicit session =>
        if (!tablesMap.contains("FEED_SOURCE")) feeds.ddl.create
        if (!tablesMap.contains("FEED_STORY")) stories.ddl.create
        if (!tablesMap.contains("USER_INFO")) userInfo.ddl.create
        if (!tablesMap.contains("USER_OPML")) opmls.ddl.create
        if (!tablesMap.contains("USER_STORY")) userReadStories.ddl.create
      }
    } catch {
      case e: Throwable => println(e) //TODO: Some problem when creating DDL in multiple Specs.
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

  class UserInfo(tag: Tag) extends Table[User](tag, "USER_INFO") {
    def userId = column[Long]("USER_ID", O.PrimaryKey, O.AutoInc) 
    def email = column[String]("EMAIL")
    def hideEmpty = column[String]("HIDE_EMTPY")
    def sort = column[String]("SORT")
    def display = column[String]("DISPLAY")

    def * = (userId, email, hideEmpty, sort, display) <> (User.tupled, User.unapply)
  }
  
  class UserOpml(tag: Tag) extends Table[OpmlStorage](tag, "USER_OPML") {
    def userId = column[Long]("USER_ID", O.PrimaryKey )
    def raw    = column[Blob]("RAW")
  
    def userOpmlFK = foreignKey("USER_OPML_FK", userId, userInfo)(_.userId)
    def * = (userId, raw) <> (OpmlStorage.tupled, OpmlStorage.unapply) 
  }
  
  class UserReadStory(tag: Tag) extends Table[ReadStory](tag, "USER_STORY") {
    def userId    = column[Long]("USER_ID", O.PrimaryKey )
    def storyId   = column[Long]("STORY_ID")
    def storyLink = column[String]("STORY_LINK")
    def star      = column[Boolean]("STAR")
    def read      = column[String]("READ")

    def userIdFK = foreignKey("USER_ID_FK", userId, userInfo)(_.userId)
    def userStoryFK = foreignKey("FEED_STORY", storyId, stories)(_.id) 
    def * = (userId, storyId,storyLink, star, read ) <> (ReadStory.tupled, ReadStory.unapply) 
  }
}