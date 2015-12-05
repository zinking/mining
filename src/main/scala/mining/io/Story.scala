package mining.io

import java.util.Date
import com.sun.syndication.feed.synd.SyndEntry
import mining.util.DateUtil
import scala.collection.JavaConverters._
import com.sun.syndication.feed.synd.SyndContent

case class Story(var id: Long,
                 val feedId: Long,
                 val title: String,
                 val link: String,
                 val published: Date,
                 val updated: Date,
                 val author: String,
                 val description: String,
                 val content: String) {
}

object StoryFactory {
    def fromSyndFeed(synd: SyndEntry, feed: Feed): Story =
        Story(-1,
            feed.feedId,
            synd.getTitle(),
            synd.getLink(),
            synd.getPublishedDate(),
            if (synd.getUpdatedDate() != null) synd.getUpdatedDate() else synd.getPublishedDate(),
            synd.getAuthor(),
            if (synd.getDescription() != null) synd.getDescription().getValue() else "",
            getSyndContent(synd)
        )
    
    def getSyndContent(synd: SyndEntry): String = {
        synd.getContents().asScala.map(_.asInstanceOf[SyndContent]).headOption match {
            case Some(content) => content.getValue()
            case None => ""
        }
    }
}