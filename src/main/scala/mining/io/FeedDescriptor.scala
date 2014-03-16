package mining.io

class FeedDescriptor(val feedUrl: String) {
  import FeedDescriptor._

  //simply remove invalid characters now, enhance it to be a GUID
  val feedUID = feedUrl.replaceAll("""http://""", "").replaceAll("""/""", "")

  val filePath = feedLocation + feedUID + ".ser" 
  
  var last_etag = "" 
  var lastparse_timestamp = ""
  var last_entry_url = ""
  
  override def toString = s"FeedDescriptor[$feedUID]"
}

object FeedDescriptor {
  val feedLocation = System.getProperty("mining.ser.path")
  
  def apply(feedUrl: String) = new FeedDescriptor(feedUrl)
} 