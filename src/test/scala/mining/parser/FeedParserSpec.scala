package mining.parser

import mining.exception.PageNotChangedException
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.ShouldMatchers
import mining.io.FeedFactory
import scala.concurrent.{Future, Await}
import scalaj.http.Http
import scalaj.http.HttpResponse
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class FeedParserSpec extends FunSuite
with ShouldMatchers {

    test("Parser should be able to parse letitcrash RSS") {
        val url = "http://letitcrash.com/rss"
        val feedParser = FeedParser(FeedFactory.newFeed(url))
        Await.result(feedParser.syncFeed(), 5 seconds) .unsavedStories.size should (be >= 10)
    }

    test("Parser should be able to parse cppblog RSS UTF8 encoding") {
        val url = "http://www.cppblog.com/7words/Rss.aspx"
        val feedParser = FeedParser(FeedFactory.newFeed(url))
        Await.result(feedParser.syncFeed(), 5 seconds) .unsavedStories.size should (be >= 10)
    }

    test("Parser should be able to parse csdn RSS and deal with pubDate updateDate") {
        val url = "http://blog.csdn.net/zhuliting/rss/list"
        //TODO: for the feed above, rome didn't parse out correct date information investigate
        val feedParser = FeedParser(FeedFactory.newFeed(url))
        val syncedFeed = Await.result(feedParser.syncFeed(), 5 seconds)
        syncedFeed.unsavedStories.size should (be >= 10)
        val story = syncedFeed.unsavedStories.head
        story.published should not be null
        story.updated should not be null
    }


    test("Parser should be able to parse smth RSS GB2312 encoding") {
        val url = "http://www.newsmth.net/nForum/rss/topten"
        val feedParser = FeedParser(FeedFactory.newFeed(url))
        Await.result(feedParser.syncFeed(), 5 seconds) .unsavedStories.size should (be >= 10)
    }

    test("Parser return 0 if nothing returned or timeout") {
        val url = "http://great-way1.appspot.com/"
        val feedParser = FeedParser(FeedFactory.newFeed(url))
        val feed = Await.result(feedParser.syncFeed(), 15 seconds)
        feed.unsavedStories.size should be(0)
        feed.errorCount should be (1L)
    }

    test("Parse coolshell RSS should work well for Chinese") {
        val url = "http://coolshell.cn/feed"
        val feedParser = FeedParser(FeedFactory.newFeed(url))
        Await.result(feedParser.syncFeed(), 5 seconds) .unsavedStories.size should (be >= 10)
    }

    test("RSS SyndEntry should be sorted as reversed time order") {
        val url = "http://coolshell.cn/feed"
        val feedParser = FeedParser(FeedFactory.newFeed(url))
        val stories = Await.result(feedParser.syncFeed(), 5 seconds) .unsavedStories
        stories.head.published.after(stories.tail.head.published) should be(right = true)
    }

    test("if the rss feed haven't been changed then no need to parse again") {
        val url = "http://coolshell.cn/feed"
        //note some site might don't have standard http server so might don't support this
        //coolshell did support
        val response: HttpResponse[String] = Http(url).asString
        response.code should be(200)
        response.body.length() should (be > 0)
        val last_modified = response.headers.getOrElse("Last-Modified", "")
        val latest_etag = response.headers.getOrElse("ETag", "")

        val s1 = new Spider()
        val rawFeed = FeedFactory.newFeed(url)
        val newFeed = rawFeed.copy(lastEtag=latest_etag)
        val stringFuture = s1.syncFeedForContent(newFeed).map{ response=>
            s1.getResponseString(newFeed,response)
        }
        val resultFuture = stringFuture recover{
            case e1:PageNotChangedException =>
                e1.message should be(url)
                //Future.successful(url)
        }

        Await.result(resultFuture, 15 seconds)
    }

}
