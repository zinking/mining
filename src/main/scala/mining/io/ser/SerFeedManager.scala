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
import mining.io.FeedDescriptor
import mining.io.FeedManager
import mining.parser.RSSFeed
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import mining.util.DirectoryUtil

class SerFeedManager extends FeedManager {

  private val logger = LoggerFactory.getLogger(classOf[SerFeedManager])
  
  private val serLocation = DirectoryUtil.pathFromPaths(System.getProperty("mining.feedmgr.path"), "feedmanager.ser")
  
  override val feedsMap = loadFeedDescriptors()  
  
  override def saveFeedDescriptors() = {
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

  override def loadFeedDescriptors() = {
    val map = mutable.Map.empty[String, FeedDescriptor]
    val mgrFile = new File(serLocation)

    if (mgrFile.exists()) {
      val objIS = new ObjectInputStream(new FileInputStream(mgrFile))
      try {
        val counts = objIS.readInt()
        for (i <- 0 until counts) {
          objIS.readObject() match {
            case fd: FeedDescriptor => map += fd.feedUID -> fd 
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
    saveFeedDescriptors()

    rssFeed
  }
  
  //TODO:Put simple concurrent here. Add a return type to the method. 
  override def createOrUpdateFeedOPML(root: Elem) = {
    val rssOutNodes = root \\ "outline" filter{node => (node \ "@type").text == "rss"}
    for (rssOutNode <- rssOutNodes) {
      val url = (rssOutNode \ "@xmlUrl").text 
      Future{ createOrUpdateFeed(url) }
    }
  }
  
  /** Create a new feed descriptor if it doesn't exist. Also sync to ser file. */
  protected[io] def createOrGetFeedDescriptor(url: String): FeedDescriptor = {
    val uid = FeedDescriptor.urlToUid(url)
    if (!feedsMap.contains(uid)) {
      feedsMap += (uid -> FeedDescriptor(url))
      saveFeedDescriptors()
    }
    feedsMap.get(uid).get
  }
}

object SerFeedManager {

  def apply() = new SerFeedManager
}