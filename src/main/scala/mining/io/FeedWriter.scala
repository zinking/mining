package mining.io

import mining.parser.RSSFeed

trait FeedWriter {
  def rssFeed: RSSFeed
  
  def feedDescriptor: FeedDescriptor
  
  def write()
}

