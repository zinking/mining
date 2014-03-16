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

class SerFeedManager extends FeedManager {

  private val logger = LoggerFactory.getLogger(classOf[SerFeedManager])
  
  private val serLocation = System.getProperty("mining.feedmgr.path") + FileSystems.getDefault().getSeparator() + "feed.manager.ser"
  
  val feedsMap = loadFeedDescriptors()  
  
  def saveFeedDescriptors() = {
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

  def loadFeedDescriptors() = {
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
  
  def createOrGetFeedDescriptor(url: String): FeedDescriptor = {
    val uid = FeedDescriptor.urlToUid(url)
    if(!feedsMap.contains(uid))
      feedsMap += (uid -> FeedDescriptor(url))
    feedsMap.get(uid).get
  }
  
    
  def createOrUpdateFeedOPML(root:Elem) = ???
  
}

object SerFeedManager {

  def apply() = new SerFeedManager
}