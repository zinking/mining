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

