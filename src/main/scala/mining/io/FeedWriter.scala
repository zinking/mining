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
    try {
      //Ser write cannot append, have to read and rewrite
      val serReader = SerFeedReader(feedDescriptor) 
	  val feedsMap = serReader.read()
	  val allFeeds = feedsMap.get(feedDescriptor).get ++ rssFeed.rssItems

      val fileOS = new FileOutputStream(feedDescriptor.filePath)
      val objOS = new ObjectOutputStream(fileOS)
      
      objOS.writeInt(allFeeds.size)
      allFeeds.foreach(objOS.writeObject(_))
      objOS.close()
    } 
    catch {
      case ex: Exception => logger.error(s"Writing feeds to ser file failed for $feedDescriptor", ex)
    		  				throw ex
    }
  }
}

object SerFeedWriter {
  def apply(rssFeed: RSSFeed) = new SerFeedWriter(rssFeed)
}