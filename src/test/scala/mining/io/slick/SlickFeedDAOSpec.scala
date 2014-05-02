package mining.io.slick

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.slick.driver.H2Driver
import mining.util.UrlUtil
import java.util.Date
import scala.util.Properties
import java.sql.Timestamp
import mining.util.DirectoryUtil
import scala.xml.XML
import mining.io.Opml

@RunWith(classOf[JUnitRunner])
class SlickFeedDAOSpec extends FunSuite 
			           with ShouldMatchers 
			           with BeforeAndAfterAll {
  Properties.setProp("runMode", "test")

  val feedDAO = SlickFeedDAO(H2Driver)
  val url1 = "http://coolshell.cn/feed"
  val url2 = "http://letitcrash.com/rss"

  override def beforeAll = {
    feedDAO.manageDDL()
  }

  test("Save new feed to db should be able to get auto inc id") {
    val feed1 = feedDAO.createOrGetFeedDescriptor(url1)
    val feed2 = feedDAO.createOrGetFeedDescriptor(url2)
    
    feed1.feedId should be (1) 
    feed2.feedId should be (2)
  }
  
  test("Load all feeds and load feed from URL/UID method should get the correct feed") {
    val feedsMap = feedDAO.loadFeeds()
    feedsMap.values.size should be (2)
    feedsMap.get(UrlUtil.urlToUid(url1)).get.url should be (url1)
    feedDAO.loadFeedFromUrl(url1).get.feedId should be (1)
    feedDAO.loadFeedFromUid(UrlUtil.urlToUid(url2)).get.feedId should be (2)
  }
  
  test("Write feed method should be able to persist changes") {
    val feed = feedDAO.loadFeedFromUrl(url1).get
    val testUrl = """http://test"""
    val testTime = new Date
    feed.lastUrl = testUrl
    feed.checked = testTime

    feedDAO.write(feed)
    val updatedFeed = feedDAO.loadFeeds.get(UrlUtil.urlToUid(url1)).get
    updatedFeed.checked should be (testTime)
    updatedFeed.lastUrl should be (testUrl)
  }
  
  test("Create or update feed method should be able to sync and persist the feed/stories") {
    val feed = feedDAO.createOrUpdateFeed(url2)
    feed.unsavedStories.size should be (0) 
    feed.checked should be > (new Date(0))
    feed.lastUrl should not be ("")
  }
  
  test("Read method should be able to read those stories just persisted") {
    val feed = feedDAO.loadFeedFromUrl(url2).get
    val stories = feedDAO.read(feed)
    stories.size should be > (10)
    val s1 = stories.head
    val s2 = stories.drop(8).head
    s1.link should not be s2.link
  }
  
  test("FeedManager should be able to parse opml format") {
    val tmpPath = DirectoryUtil.pathFromProject("config", "zhen_opml.xml")
    val xml = XML.loadFile(tmpPath)
    val opml = Opml("testOpml", xml)
    feedDAO.createOrUpdateFeedOPML(opml)
    
    Thread.sleep(5000)
    feedDAO.feedsMap.size should be > (5)
  }
}