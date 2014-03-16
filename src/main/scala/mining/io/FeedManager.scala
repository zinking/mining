package mining.io

import java.io.File
import java.nio.file.FileSystems
import scala.xml._
import org.slf4j.LoggerFactory
import java.io.ObjectInputStream
import java.io.FileInputStream
import scala.collection.mutable
import java.io.ObjectOutputStream
import java.io.FileOutputStream

trait FeedManager {
  
  /** Map from Feed UID to Feed Descriptor */
  def feedsMap: mutable.Map[String, FeedDescriptor]
  
  /** If the URL(UID) doesn't exist, create a new descriptor, else return directly */
  def createOrGetFeedDescriptor(url: String): FeedDescriptor
  
  def createOrUpdateFeedOPML(root:Elem)
  
  /** Persist current feed descriptors */
  def saveFeedDescriptors()
  
  /** Get the descriptor from feed URL */
  def loadDescriptorFromUrl(url: String): Option[FeedDescriptor] = feedsMap.get(FeedDescriptor.urlToUid(url))
  
  /** Get the descriptor from feed UID */
  def loadDescriptorFromUid(uid: String): Option[FeedDescriptor] = feedsMap.get(uid) 
  
  /** Load the map of all the feed descriptors */
  def loadFeedDescriptors(): mutable.Map[String, FeedDescriptor]
}

class SerFeedManager extends FeedManager {
  import SerFeedManager._ 

  private val logger = LoggerFactory.getLogger(classOf[SerFeedManager])
  
  val feedsMap = loadFeedDescriptors()  
  
  def saveFeedDescriptors() = {
    val objOS = new ObjectOutputStream(new FileOutputStream(SER_FEED_LOCATOIN))
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

  def loadFeedDescriptors() = {
    val map = mutable.Map.empty[String, FeedDescriptor]
    val mgrFile = new File(SER_FEED_LOCATOIN)

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
  
  def createOrGetFeedDescriptor(url: String): FeedDescriptor = {
    val uid = FeedDescriptor.urlToUid(url)
    if(!feedsMap.contains(uid))
      feedsMap += (uid -> FeedDescriptor(url))
    feedsMap.get(uid).get
  }
  
    
  def createOrUpdateFeedOPML(root:Elem) = ???
  
}

object SerFeedManager {

  val SER_FEED_MANAGER_NAME = "feed.manager.ser"
  val SER_FEED_MANAGER_PATH = System.getProperty("mining.feedmgr.path")
  val SER_FEED_LOCATOIN = SER_FEED_MANAGER_PATH + FileSystems.getDefault().getSeparator() + SER_FEED_MANAGER_NAME

  def apply() = new SerFeedManager
}