package mining.parser

import scala.xml._
import com.sun.syndication.feed.synd.SyndEntry
import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.io.SyndFeedInput;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import java.io.StringReader;

class RSSFeed(val url: String ) {
    val metadata:Map[String,String] = Map() 
	val root = {
	  val content = new Spider().getRssFeed(url, metadata) //implicit dependency on metadata
	  val dom = new SAXBuilder().build(
	      new StringReader(content)
	  )
	  new SyndFeedInput().build( dom );
	}
	
	lazy val rssItems = root.getEntries()
	
	
	
	override def toString = s"Feed($url)"
}

object RSSFeed {
  def apply(url: String ) = new RSSFeed(url)
}

case class RSSChannel(val title: String, val description: String, val link: String)
