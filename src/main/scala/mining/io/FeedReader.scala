package mining.io

import com.sun.syndication.feed.synd.SyndEntry
import java.io.FileInputStream
import java.io.ObjectInputStream
import scala.collection.mutable
import org.slf4j.LoggerFactory
import java.io.File
import mining.parser.RSSFeed

trait FeedReader {
  def feedDescriptor: FeedDescriptor
  
  def read(): RSSFeed 
}

class SerFeedReader(val feedDescriptor: FeedDescriptor) extends FeedReader {
  //import mining.parser.SyndEntryOrdering._

  private val logger = LoggerFactory.getLogger(classOf[SerFeedReader])

  override def read(): RSSFeed = {
    val entries = mutable.Buffer.empty[SyndEntry]
    val feedFile = new File(feedDescriptor.filePath)
    if (feedFile.exists()) {
      val objIS = new ObjectInputStream(new FileInputStream(feedFile))
      try {
        val counts = objIS.readInt()
        for (i <- 0 until counts) {
          objIS.readObject() match {
            case feedEntry: SyndEntry => entries += feedEntry
          }
        }
      }
      catch {
        case ex: Exception => logger.error(s"Reading feeds from ser file failed for $feedDescriptor", ex); throw ex
      }
      finally {
        objIS.close()
      }
    }
    
    val feed = RSSFeed(feedDescriptor)
    feed.addFeedEntries(entries)
    feed
  }
  
}

object SerFeedReader {
  def apply(feedDescriptor: FeedDescriptor) = new SerFeedReader(feedDescriptor)
}