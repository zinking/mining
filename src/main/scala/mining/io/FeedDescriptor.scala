package mining.io

import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.nio.file.FileSystems
import mining.util.DateUtil
import mining.util.DirectoryUtil

class FeedDescriptor(val feedUrl: String) extends Serializable {
  import FeedDescriptor._

  val feedUID = urlToUid(feedUrl) 

  val filePath = DirectoryUtil.pathFromPaths(System.getProperty("mining.ser.path"), feedUID + ".ser")
  
  var lastEtag = "" 
    
  //use the util method to reduce the size after serialization
  var lastParseTimestamp = DateUtil.getParser.format(new Date(0))

  var lastEntryUrl = ""
    
  var encoding = "UTF-8"
  
  override def toString = s"FeedDescriptor[$feedUID]"
}

object FeedDescriptor {
  /** 
   *  Creating instance of feed descriptor from url.
   *  Clients should try getting the feed descriptor from FeedManager instead of creating new ones.
   */
  def apply(feedUrl: String) = new FeedDescriptor(feedUrl)
  
  //TODO:still not perfect, best be fixed lenght format
  def urlToUid(url: String) = url.replaceAll("""http://""", "").replaceAll("[^a-zA-Z0-9]+","");
  
} 