package mining.parser

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class FeedParserSpec extends FunSuite 
					 with ShouldMatchers {
	
  test("Parser should be able to parse letitcrash RSS") {
    val feed = RSSFeed("http://letitcrash.com/rss")

    val rssItemSize = feed.rssItems.size
    println(s"Loaded $rssItemSize items from $feed")
    feed.rssItems foreach println

    rssItemSize should (be > 10)
  }
  
  test("Parse coolshell RSS should work well for Chinese") {
    val feed = RSSFeed("http://coolshell.cn/feed")
    
    val rssItemSize = feed.rssItems.size
    println(s"Loaded $rssItemSize items from $feed")
    feed.rssItems foreach println

    rssItemSize should (be > 10)
  }
}