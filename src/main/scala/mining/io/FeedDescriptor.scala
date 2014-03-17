package mining.io

import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class FeedDescriptor(val feedUrl: String) extends Serializable {
  import FeedDescriptor._

  val feedUID = urlToUid(feedUrl) 

  val filePath = System.getProperty("mining.ser.path") + feedUID + ".ser" 
  
  val TIME_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
  
  var lastEtag = "" 
    
  var lastParseTimestamp = TIME_FORMAT.format(new Date(0))
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