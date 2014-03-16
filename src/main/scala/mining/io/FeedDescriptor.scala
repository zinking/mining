package mining.io

import java.util.Date
import mining.parser.Spider

class FeedDescriptor(val feedUrl: String) extends Serializable {
  import FeedDescriptor._

  val feedUID = urlToUid(feedUrl) 

  val filePath = System.getProperty("mining.ser.path") + feedUID + ".ser" 
  
  var lastEtag = "" 
    
  var lastParseTimestamp = Spider.TIME_FORMAT.format(new Date(0) )
  var lastEntryUrl = ""
  
  override def toString = s"FeedDescriptor[$feedUID]"
}

object FeedDescriptor {
  /** 
   *  Creating instance of feed descriptor from url.
   *  Clients should try getting the feed descriptor from FeedManager instead of creating new ones.
   */
  def apply(feedUrl: String) = new FeedDescriptor(feedUrl)
  
  def urlToUid(url: String) = url.replaceAll("""http://""", "").replaceAll("""/""", "")
} 