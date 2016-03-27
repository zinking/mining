package mining.io.slick

import mining.io.dao.FeedDao
import org.scalatest.FunSuite
import org.scalatest.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import mining.util.{DaoTestUtil, UrlUtil, DirectoryUtil}
import java.util.Date
import scala.collection.mutable
import scala.concurrent.Await
import scala.xml.XML
import mining.io.{Story, Opml}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


@RunWith(classOf[JUnitRunner])
class SlickFeedDAOSpec extends FunSuite
with ShouldMatchers
with BeforeAndAfterAll {
    val url0 = "http://coolshell.cn/feed0"
    val url1 = "http://coolshell.cn/feed"
    val url2 = "http://letitcrash.com/rss"


    val feedDAO = FeedDao()

    override def beforeAll() = {
        DaoTestUtil.truncateAllTables()
    }

    test("Save new feed to db should be able to get auto inc id") {
        //val feed0 = feedDAO.createOrGetFeedDescriptor(url0)
        val feed1 = Await.result(feedDAO.createOrUpdateFeed(url1), 5 seconds)
        val feed2 = Await.result(feedDAO.createOrUpdateFeed(url2), 5 seconds)

        feed1.get.feedId should be(1)
        feed2.get.feedId should be(2)
    }

    test("Load all feeds and load feed from URL method should get the correct feed") {
        val feedsMap = feedDAO.loadFeeds()
        feedsMap.values.size should be(2)
        feedsMap.get(url1).get.xmlUrl should be(url1)
        feedDAO.loadFeedFromUrl(url1).get.feedId should be(1)
        //need to revisit the need of uid
        //feedDAO.loadFeedFromUid(UrlUtil.urlToUid(url2)).get.feedId should be(2)
    }

    test("Write feed method should be able to persist changes") {
        val feed = feedDAO.loadFeedFromUrl(url1).get
        val testUrl = """http://test"""
        val testTime = new Date
        val newFeed = feed.copy(lastUrl = testUrl, checked = testTime)

        feedDAO.write(newFeed)
        val updatedFeed = feedDAO.loadFeeds().get(url1).get
        updatedFeed.checked.toString should be(testTime.toString)
        updatedFeed.lastUrl should be(testUrl)
    }

    test("Create or update feed method should be able to sync and persist the feed/stories") {
        val feed = Await.result(feedDAO.createOrUpdateFeed(url2), 5 seconds)
        feed.get.unsavedStories.size should be(0)
        feed.get.checked should be > new Date(0)
        feed.get.lastUrl should not be ""
    }

    test("Read method should be able to read those stories just persisted") {
        val feed = feedDAO.loadFeedFromUrl(url2).get
        val stories = feedDAO.getStoriesFromFeed(feed)
        stories.size should be(10)
        val s1 = stories.head
        val s2 = stories.drop(8).head
        s1.link should not be s2.link

        //stats need to be refreshed
        feed.visitCount should be(1)
        feed.refreshCount should be(1)
        feed.refreshItemCount should be > 0L


    }

    test("parse a url second time won't duplicate stories") {
        val feed2 = Await.result(feedDAO.createOrUpdateFeed(url2), 5 seconds).get
        val stories = feedDAO.getStoriesFromFeed(feed2)
        stories.size should be(10)

        //stats need to be refreshed
        feed2.visitCount should be(2)
        feed2.refreshCount should be(1)
    }

    test("FeedManager should be able to parse opml format") {
        val tmpPath = DirectoryUtil.pathFromProject("config", "zhen_opml.xml")
        val xml = XML.loadFile(tmpPath)
        val opml = Opml(1L, xml)
        feedDAO.createOrUpdateFeedOPML(opml) onSuccess{
            case feeds =>
                feeds.size  should be > 5
        }
    }

    test("Create or update feed method should be able to create correct outline") {
        val feed = Await.result(feedDAO.createOrUpdateFeed(url1), 5 seconds)
        feed.get.unsavedStories.size should be(0)
        feed.get.checked should be > new Date(0)
        feed.get.lastUrl should not be ""
        feed.get.getOpmlOutline.title should not be ""
        feed.get.getOpmlOutline.text should not be ""
    }

    test("get new story only should distinguish new stories"){
        val link2 = "http://coolshell.com/last2"
        val link1 = "http://coolshell.com/last1"
        val story2 = Story(2L,1L,"SubTask",link2,new Date(1000), new Date(10000),"CH","nothing","nothing")
        val story1 = Story(1L,1L,"SubTask",link1,new Date(100000), new Date(101000),"CH","nothing","nothing")
        val newlyParsedStories = mutable.ListBuffer[Story](story1,story2)

        val lastUrl = "http://coolshell.com/last2"
        val onlyNewStories = feedDAO.getOnlyNewStories(newlyParsedStories,lastUrl)
        onlyNewStories.size should be(1)
        onlyNewStories.head.link should be(link1)
    }
}