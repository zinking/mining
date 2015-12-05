package mining.parser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.ShouldMatchers
import mining.io.FeedFactory
import scalaj.http.Http
import scalaj.http.HttpResponse

@RunWith(classOf[JUnitRunner])
class FeedParserSpec extends FunSuite
with ShouldMatchers {

    test("Parser should be able to parse letitcrash RSS") {
        val url = "http://letitcrash.com/rss"
        val feed = FeedParser(FeedFactory.newFeed(url))
        feed.syncFeed.size should (be >= 10)
    }

    test("Parser should be able to parse cppblog RSS UTF8 encoding") {
        val url = "http://www.cppblog.com/7words/Rss.aspx"
        val feed = FeedParser(FeedFactory.newFeed(url))
        feed.syncFeed.size should (be >= 10)
    }


    test("Parser should be able to parse smth RSS GB2312 encoding") {
        val url = "http://www.newsmth.net/nForum/rss/topten"
        val feed = FeedParser(FeedFactory.newFeed(url))
        feed.syncFeed.size should (be >= 10)
    }

    test("Parser return 0 if nothing returned or timeout") {
        val url = "http://great-way1.appspot.com/"
        val feed = FeedParser(FeedFactory.newFeed(url))
        feed.syncFeed.size should be(0)
    }

    test("Parse coolshell RSS should work well for Chinese") {
        val url = "http://coolshell.cn/feed"
        val feed = FeedParser(FeedFactory.newFeed(url))
        feed.syncFeed.size should (be >= 10)
    }

    test("RSS SyndEntry should be sorted as reversed time order") {
        val url = "http://coolshell.cn/feed"
        val feed = FeedParser(FeedFactory.newFeed(url))
        val stories = feed.syncFeed
        stories.head.published.after(stories.tail.head.published) should be(true)
    }

    test("if the rss feed haven't been changed then no need to parse again") {
        val url = "http://coolshell.cn/feed"
        //note some site might don't have standard http server so might don't support this
        //coolshell did support
        val response: HttpResponse[String] = Http(url).asString
        val (responseCode, headersMap, resultString) = (response.code, response.headers, response.body)
        resultString.length() should (be > 0)
        val last_modified = headersMap.getOrElse("Last-Modified", "")
        val last_etag = headersMap.getOrElse("ETag", "")

        val s1 = new Spider()
        val md = FeedFactory.newFeed(url)
        md.lastEtag = last_modified
        md.lastEtag = last_etag
        val content = s1.getFeedContent(md)

        content should equal(Spider.EMPTY_RSS_FEED)
    }

}
