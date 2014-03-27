package mining.io.ser

import org.slf4j.LoggerFactory
import java.io.ObjectOutputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.File
import scala.collection.mutable
import java.io.FileInputStream
import scala.xml.Elem
import java.nio.file.FileSystems
import mining.io.Feed
import mining.io.FeedManager
import mining.io.RSSFeed
import mining.util.DirectoryUtil
import mining.util.UrlUtil
import java.util.concurrent.atomic.AtomicInteger
import mining.io.Feed

class SerFeedManager extends FeedManager {

  private val logger = LoggerFactory.getLogger(classOf[SerFeedManager])
  
  private val serLocation = DirectoryUtil.pathFromPaths(System.getProperty("mining.feedmgr.path"), "feedmanager.ser")
  
  override lazy val feedsMap = loadFeeds()  
  
  //For simple SER implementation, each time all the feeds need to be written again
  override def saveFeed(feed: Feed) = {
    val objOS = new ObjectOutputStream(new FileOutputStream(serLocation))
    try {
      objOS.writeInt(feedsMap.size)
      feedsMap.values.foreach(objOS.writeObject(_))
    } 
    catch {
      case ex: Exception => logger.error(s"Writing feeds descriptors to ser file failed", ex); throw ex
    }
    finally {
      objOS.close()
    }
  }

  override def loadFeeds() = {
    val map = mutable.Map.empty[String, Feed]
    val mgrFile = new File(serLocation)

    if (mgrFile.exists()) {
      val objIS = new ObjectInputStream(new FileInputStream(mgrFile))
      try {
        val counts = objIS.readInt()
        for (i <- 0 until counts) {
          objIS.readObject() match {
            case fd: Feed => map += fd.uid -> fd 
          }
        }
      }
      catch {
        case ex: Exception => logger.error(s"Reading feed descriptors from ser files failed", ex); throw ex
      }
      finally {
        objIS.close()
      }
    }
    map
  }
  
  override def createOrUpdateFeed(url: String): RSSFeed = {
    val fd = createOrGetFeedDescriptor(url)
    val rssFeed = SerFeedReader(fd).read()
    rssFeed.syncFeed()
    SerFeedWriter(rssFeed).write()
    saveFeed(fd)

    rssFeed
  }
  
  

}

object SerFeedManager {

  def apply() = new SerFeedManager
}