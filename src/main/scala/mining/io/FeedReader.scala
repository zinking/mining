package mining.io

trait FeedReader {
  def feedDescriptor: Feed
  
  def read(): RSSFeed 
}

