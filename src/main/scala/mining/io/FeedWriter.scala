package mining.io

import mining.parser.RSSFeed
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import org.slf4j.LoggerFactory

trait FeedWriter {
  def rssFeed: RSSFeed
  
  def feedDescriptor: FeedDescriptor
  
  def write()
}

class SerFeedWriter(val rssFeed: RSSFeed) extends FeedWriter {
  
  private val logger = LoggerFactory.getLogger(classOf[SerFeedWriter])

  val feedDescriptor = FeedDescriptor(rssFeed.url)

  override def write() = {
    val objOS = new ObjectOutputStream(new FileOutputStream(feedDescriptor.filePath))
    try {
      //Ser write cannot append, have to read and rewrite
      val serReader = SerFeedReader(feedDescriptor) 
	  val feedsMap = serReader.read()
	  val allFeeds = feedsMap.get(feedDescriptor).get ++ rssFeed.rssItems

      objOS.writeInt(allFeeds.size)
      allFeeds.foreach(objOS.writeObject(_))
    } 
    catch {
      case ex: Exception => logger.error(s"Writing feeds to ser file failed for $feedDescriptor", ex)
    		  				throw ex
    }
    finally {
      objOS.close()
    }
  }
}

object SerFeedWriter {
  def apply(rssFeed: RSSFeed) = new SerFeedWriter(rssFeed)
}