package mining.io

import java.util.Date
import com.sun.syndication.feed.synd.SyndEntry
import scala.collection.JavaConverters._
import com.sun.syndication.feed.synd.SyndContent

case class Story(id: Long,
                 feedId: Long,
                 title: String,
                 link: String,
                 published: Date,
                 updated: Date,
                 author: String,
                 description: String,
                 content: String) {
}

object StoryFactory {
    def fromSyndFeed(synd: SyndEntry, feed: Feed): Story = {
        val now = new Date
        val publishDate = Option(synd.getPublishedDate).getOrElse(now)
        val updateDate = Option(synd.getUpdatedDate).getOrElse(publishDate)

        Story(-1,
            feed.feedId,
            synd.getTitle,
            synd.getLink,
            publishDate,
            updateDate,
            synd.getAuthor,
            if (synd.getDescription != null) synd.getDescription.getValue else "",
            getSyndContent(synd)
        )
    }
    
    def getSyndContent(synd: SyndEntry): String = {
        synd.getContents.asScala.map(_.asInstanceOf[SyndContent]).headOption match {
            case Some(content) => content.getValue
            case None => ""
        }
    }
}