package mining.io

import mining.parser.RSSFeed

trait FeedReader {
  def feedDescriptor: FeedDescriptor
  
  def read(): RSSFeed 
}

