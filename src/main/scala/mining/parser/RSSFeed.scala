package mining.parser

import scala.xml._

class RSSFeed(val url: String) {
	lazy val root = XML.load(url)

	lazy val channelNode = root \\ "channel"
	
	lazy val itemNode = root \\ "item"
	
	lazy val channel = channelNode.headOption match {
	  case Some(node) => buildRSSChannel(node)
	  case None => throw new Exception("")
	}
	
	lazy val rssItems: Iterable[RSSItem] = itemNode.map(buildRSSItem(_))
	
	protected[this] def buildRSSChannel(node: Node) = RSSChannel((node \ "title").text, (node \ "description").text, (node \ "link").text)

	protected[this] def buildRSSItem(node: Node) = 
	  RSSItem(this, 
	          (node \ "title").text, 
	          (node \ "description").text, 
	          (node \ "link").text,
			  (node \ "encoded").text, 
			  (node \ "author").text, 
			  (node \ "category").text, 
			  (node \ "comments").text, 
			  (node \ "enclosure").text, 
			  (node \ "guid").text, 
			  DateParser.parseRSSDate((node \ "pubDate").text), 
			  (node \ "source").text) 
	
	override def toString = s"Feed($url)"
}

object RSSFeed {
  def apply(url: String) = new RSSFeed(url)
}

case class RSSChannel(val title: String, val description: String, val link: String)
