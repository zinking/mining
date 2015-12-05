package mining.io

import mining.parser.FeedParser

trait FeedWriter {

    /** Persist the feed's information and unsaved stories */
    def write(feed: Feed)
}