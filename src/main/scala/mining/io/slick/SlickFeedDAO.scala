package mining.io.slick

import slick.driver.H2Driver.api._
import java.sql.Timestamp
import java.util.Date
import scala.collection.mutable
import mining.parser.FeedParser
import mining.util.UrlUtil
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory
import mining.io.FeedManager
import mining.io.FeedWriter
import mining.io.FeedReader
import mining.io.Feed
import mining.io.Story
import mining.io.Opml

object SlickFeedDAO {
  def apply(db:Database) = new SlickFeedDAO(db)
}

class SlickFeedDAO(db:Database) extends SlickUserFeedDDL(db)
  with FeedManager
  with FeedWriter 
  with FeedReader {
  
  val logger = LoggerFactory.getLogger(classOf[SlickFeedDAO])

  override lazy val feedsMap = loadFeeds() 

  override def loadFeeds() = {
    val map = mutable.Map.empty[String, Feed] 
    Await.result(
        db.run(feeds.result), 
        Duration.Inf
    ).map{ f=>
      map += (UrlUtil.urlToUid(f.url) -> f)
    }
    map
  }
  
  override def write(feed: Feed) = {
    feed.synchronized {
      Await.result(
          db.run( (feeds returning feeds.map(_.feedId)).insertOrUpdate(feed) ),
          Duration.Inf
      ).map{ feedId =>
          feed.feedId = feedId;
      }
      
      //Persist unsaved stories
      val unsaved = Await.result(
          db.run( stories++= feed.unsavedStories ),
          Duration.Inf
      )
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

  //TODO: Should take the latest stories according to time stamp
  override def read(feed: Feed, count: Int = Int.MaxValue): Iterable[Story] = {
    Await.result(
        db.run(stories.filter(_.feedId === feed.feedId).result), 
        Duration.Inf
    ).take(count)
  }
  
  def getOpmlStories(opml:Opml, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
	  opml.allFeedsUrl.foldLeft(List[Story]())((acc, node) => {
	    val fd = loadFeedFromUrl(node)
	    fd match {
	      case Some(ffd) => {
          val ss = Await.result(
              db.run(stories.filter( _.feedId === ffd.feedId).result),
              Duration.Inf
          ).drop(pageSize * pageNo).take(pageSize)
		      acc ++ ss
	      }
	      case _ => acc
	    }
	  })
  }
  
  def getFeedStories(feedUrl: String, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
      //val fd = loadFeedFromUrl(feedUrl)
      val fd = feedsMap.get( UrlUtil.urlToUid(feedUrl) )
      fd match {
        case Some(feed) => {
          Await.result(
              db.run(stories.filter(_.feedId === feed.feedId).result),
              Duration.Inf
          ).drop(pageNo * pageSize).take(pageSize)
        }
        case None => List.empty[Story]
      }
  }
  
  
  
  def getStoryById(storyId: String): Story = {
    Await.result(
      db.run(stories.filter( _.link === storyId ).result.head),
      Duration.Inf
    )
  }
  
  def getStoryByLink( sl: String): Story = {
    Await.result(
      db.run(stories.filter( _.link === sl ).result.head),
      Duration.Inf
    )
  }
  
  def getStoryContentById(storyId: String): String = {
    Await.result(
      db.run(stories.filter( _.link === storyId ).result.head),
      Duration.Inf
    ).content
  }
  
  def getStoryContentByLink( sl: String): String = {
    Await.result(
      db.run(stories.filter( _.link === sl ).result.head),
      Duration.Inf
    ).content
  }
  
}