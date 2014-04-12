package mining.io.ser

import mining.io.FeedReader
import mining.io.Feed
import com.sun.syndication.feed.synd.SyndEntry
import java.io.ObjectInputStream
import java.io.File
import scala.collection.mutable
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import mining.io.Story
import mining.parser.FeedParser

class SerFeedReader(val feedDescriptor: Feed) extends FeedReader {

  private val logger = LoggerFactory.getLogger(classOf[SerFeedReader])

  override def read(): FeedParser = {
    val entries = mutable.Buffer.empty[Story]
    val feedFile = new File(feedDescriptor.filePath)
    if (feedFile.exists()) {
      val objIS = new ObjectInputStream(new FileInputStream(feedFile))
      try {
        val counts = objIS.readInt()
        for (i <- 0 until counts) {
          objIS.readObject() match {
            case story: Story => entries += story 
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
    
    val feed = FeedParser(feedDescriptor)
    feed.addFeedEntries(entries)
    feed
  }
  
}

object SerFeedReader {
  def apply(feedDescriptor: Feed) = new SerFeedReader(feedDescriptor)
}