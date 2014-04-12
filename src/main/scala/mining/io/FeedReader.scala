package mining.io

import mining.parser.FeedParser

trait FeedReader {
  def feedDescriptor: Feed
  
  def read(): FeedParser 
}

