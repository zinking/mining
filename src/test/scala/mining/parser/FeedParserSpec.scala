package mining.parser

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers


import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scalaj.http.Http

@RunWith(classOf[JUnitRunner])
class FeedParserSpec extends FunSuite 
                     with ShouldMatchers {
  
  test("Parser should be able to parse letitcrash RSS") {
    val url = "http://letitcrash.com/rss"
    val feed = RSSFeed( url )

    val rssItemSize = feed.rssItems.size
    println(feed.rssItems.head)

    rssItemSize should (be > 10)
  }
  
  test("Parse coolshell RSS should work well for Chinese") {
    val feed = RSSFeed("http://coolshell.cn/feed")
    feed.rssItems.size should (be > 10)
  }
  
  test("if the rss feed haven't been changed then no need to parse again") {
     val url = "http://coolshell.cn/feed"
       //note some site might don't have standard http server so might don't support this
       //coolshell did support
     val  (responseCode, headersMap, resultString) = Http(url).asHeadersAndParse(Http.readString)
     resultString.length() should ( be > 0 )
     val last_modified = headersMap.getOrElse("Last-Modified", List(""))(0)
     val last_etag     = headersMap.getOrElse("ETag", List(""))(0)
       
     val s1 = new Spider()
     val md = Map(
           "Last-Modified" -> last_modified,
           "ETag" -> last_etag
         )
     val content = s1.getRssFeed(url, md)
     
     content should equal ( "" )
  }
}