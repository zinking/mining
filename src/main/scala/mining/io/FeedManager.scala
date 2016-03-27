package mining.io

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem

import mining.util.UrlUtil

trait FeedManager extends FeedReader with FeedWriter {

    /** Map from Feed UID to Feed Descriptor */
    //def feedsMap: mutable.Map[String, Feed]

    /** Get the descriptor from feed URL */
    def loadFeedFromUrl(url: String): Option[Feed]

    /** Get the descriptor from feed UID */
    def loadFeedFromUid(uid: String): Option[Feed]

    /** Load the map of all the feed descriptors */
    def loadFeeds(): mutable.Map[String, Feed]

    /** Create a new feed if the UID of the URL doesn't exist. Sync and persist after that */
    def createOrUpdateFeed(url: String): Future[Option[Feed]]



    /** Check all feeds in OPML file */
    def createOrUpdateFeedOPML(opml: Opml):Future[Iterable[Feed]] = {
        //dada I am doing functional programming
        val r1:List[Future[Option[Feed]]] = opml.allFeedsUrl.map{ url=>
            createOrUpdateFeed(url)
        }
        val r2:Future[List[Option[Feed]]] = Future.sequence(r1)
        r2.map{ rr=>
            rr.flatten
        }
    }
}

