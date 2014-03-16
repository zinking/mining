package mining.io

class FeedDescriptor(val feedUrl: String) extends Serializable {
  import FeedDescriptor._

  //simply remove invalid characters now, enhance it to be a GUID
  val feedUID = urlToUid(feedUrl) 

  val filePath = feedLocation + feedUID + ".ser" 
  
  var lastEtag = "" 
  var lastParseTimestamp = ""
  var lastEntryUrl = ""
  
  override def toString = s"FeedDescriptor[$feedUID]"
}

object FeedDescriptor {
  val feedLocation = System.getProperty("mining.ser.path")
  
  /** 
   *  Creating instance of feed descriptor from url.
   *  Clients should try getting the feed descriptor from FeedManager instead of creating new ones.
   */
  def apply(feedUrl: String) = new FeedDescriptor(feedUrl)
  
  def urlToUid(url: String) = url.replaceAll("""http://""", "").replaceAll("""/""", "")
} 