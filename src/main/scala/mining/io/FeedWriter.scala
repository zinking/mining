package mining.io

trait FeedWriter {
  def rssFeed: RSSFeed
  
  def feedDescriptor: Feed
  
  def write()
}

