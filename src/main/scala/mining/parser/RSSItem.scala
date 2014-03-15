package mining.parser

import java.util.Date

case class RSSItem(val feed: RSSFeed, val title: String, val description: String, val link: String, val content: String = "",
			  	   val author: String = "", val category: String = "", val comments: String = "",
			  	   val enclosure: String = "", val guid: String = "", val pubDate: Date = null, val source: String = "") 
			  	   extends Ordered[RSSItem] {
  override def compare(that: RSSItem) = this.pubDate.compareTo(that.pubDate)
}

