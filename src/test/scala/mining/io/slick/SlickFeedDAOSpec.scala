package mining.io.slick

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.slick.driver.H2Driver
import mining.util.UrlUtil
import java.util.Date

@RunWith(classOf[JUnitRunner])
class SlickFeedDAOSpec extends FunSuite 
			           with ShouldMatchers 
			           with BeforeAndAfterAll {
  System.setProperty("runMode", "test")

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
  
  test("Load all feeds from db should be able to get correct feeds") {
    val feedsMap = feedDAO.loadFeeds()
    feedsMap.values.size should be (2)
    feedsMap.get(UrlUtil.urlToUid(url1)).get.feedId should be (1)
    feedsMap.get(UrlUtil.urlToUid(url2)).get.url should be (url2)
  }
  
  test("Update existing feed should be able to update the old entry") {
    val feed = feedDAO.loadFeedFromUrl(url1).get
    val testUrl = """http://test"""
    val testTime = new Date
    feed.lastUrl = testUrl
    feed.checked = testTime 

    feedDAO.saveFeed(feed)
    val updatedFeed = feedDAO.loadFeeds().get(UrlUtil.urlToUid(url1)).get
    updatedFeed.checked should be (testTime)
    updatedFeed.lastUrl should be (testUrl)
  }
}