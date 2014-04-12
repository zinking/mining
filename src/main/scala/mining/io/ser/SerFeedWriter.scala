package mining.io.ser

import mining.io.FeedWriter
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import org.slf4j.LoggerFactory
import mining.parser.FeedParser

class SerFeedWriter(val rssFeed: FeedParser) extends FeedWriter {
  
  private val logger = LoggerFactory.getLogger(classOf[SerFeedWriter])

  val feedDescriptor = rssFeed.feed

  override def write() = {
    val objOS = new ObjectOutputStream(new FileOutputStream(feedDescriptor.filePath))
    try {
      val allFeeds = rssFeed.stories
      objOS.writeInt(allFeeds.size)
      allFeeds.foreach(objOS.writeObject(_))
    } 
    catch {
      case ex: Exception => logger.error(s"Writing feeds to ser file failed for $feedDescriptor", ex); throw ex
    }
    finally {
      objOS.close()
    }
  }
}

object SerFeedWriter {
  def apply(rssFeed: FeedParser) = new SerFeedWriter(rssFeed)
}