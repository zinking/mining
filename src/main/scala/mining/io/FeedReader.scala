package mining.io

import mining.parser.FeedParser

trait FeedReader {

  /** Read a certain number of stories from the feed */
  def read(feed: Feed, count: Int = Int.MaxValue): Iterable[Story] 
}

