package mining.io

import mining.parser.FeedParser

trait FeedWriter {
  def rssFeed: FeedParser
  
  def feedDescriptor: Feed
  
  def write()
}

