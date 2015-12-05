package mining.io

import mining.parser.FeedParser

trait FeedReader {
    /** Read a certain number of stories from the feed */
    def read(feed: Feed, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story]
}