package mining.io

import java.util.Date
import mining.parser.Spider

class FeedDescriptor(val feedUrl: String) {
  import FeedDescriptor._

  //simply remove invalid characters now, enhance it to be a GUID
  val feedUID = feedUrl.replaceAll("""http://""", "").replaceAll("[^a-zA-Z0-9]+","");

  val filePath = feedLocation + feedUID + ".ser" 
  
  var lastEtag = "" 
    
  var lastParseTimestamp = Spider.TIME_FORMAT.format(new Date(0) )
  var lastEntryUrl = ""
  
  override def toString = s"FeedDescriptor[$feedUID]"
}

object FeedDescriptor {
  val feedLocation = System.getProperty("mining.ser.path")
  
  def apply(feedUrl: String) = new FeedDescriptor(feedUrl)
} 