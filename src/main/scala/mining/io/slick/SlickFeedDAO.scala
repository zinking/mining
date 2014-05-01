package mining.io.slick

import java.sql.Timestamp
import java.util.Date
import scala.collection.mutable
import scala.slick.driver.JdbcProfile
import mining.io._
import mining.parser.FeedParser
import mining.util.UrlUtil
import org.slf4j.LoggerFactory

class SlickFeedDAO(override val profile: JdbcProfile) 
  extends SlickUserFeedDDL(profile) 
  with FeedManager
  with FeedWriter 
  with FeedReader {
  import profile.simple._
  
  val logger = LoggerFactory.getLogger(classOf[SlickFeedDAO])

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
      val unsaved = feed.unsavedStories.toSeq
      if (!unsaved.isEmpty) {
        val count = stories.insertAll(unsaved: _*)
        feed.unsavedStories.clear()
      }
    }
  }
  
  override def createOrUpdateFeed(url: String): Feed = {
    val feed = createOrGetFeedDescriptor(url)
    feed.sync()
    write(feed) 
    feed
  }

  //TODO: Should take the latest stories according to time stamp
  override def read(feed: Feed, count: Int = Int.MaxValue): Iterable[Story] = 
    database withSession { implicit session =>
      stories.filter(_.feedId === feed.feedId).list.take(count)
    }
  
  def getOpmlStories(opml:Opml, pageSize: Int = 10, pageNo: Int = 0): List[Story] = {
    database withTransaction { implicit session =>
	  opml.allFeedsUrl.foldLeft(List[Story]())((acc, node) => {
	    val fd = loadFeedFromUrl(node)
	    fd match {
	      case Some(ffd) => {
	        val ss = stories.where( _.feedId === ffd.feedId).drop(pageSize * pageNo).take(pageSize)
		    acc ++ ss.buildColl
	      }
	      case _ => acc
	    }
	  })
    }
  }
  
  def getFeedStories(feedUrl: String, pageSize: Int = 10, pageNo: Int = 0): List[Story] = {
    database withTransaction { implicit session =>

      val fd = loadFeedFromUrl(feedUrl)
      fd match {
        case Some(feed) => stories.filter(_.feedId === feed.feedId).drop(pageSize * pageSize).take(pageSize).buildColl
        case None => List.empty[Story]
      }
    }
  }
  
  def getStoryById(storyId: String): Story = {
    database withTransaction { implicit session =>
      stories.filter( _.link === storyId ).first
    }
  }
  
  def getStoryByLink( sl: String): Story = {
    database withTransaction { implicit session =>
      stories.filter( _.link === sl ).first
    }
  }
  
  def getStoryContentById(storyId: String): String = {
    database withTransaction { implicit session =>
      stories.filter( _.link === storyId ).first.content
    }
  }
  
  def getStoryContentByLink( sl: String): String = {
    database withTransaction { implicit session =>
      stories.filter( _.link === sl ).first.content
    }
  }
  
}

object SlickFeedDAO {
  def apply(profile: JdbcProfile) = new SlickFeedDAO(profile)
  
}