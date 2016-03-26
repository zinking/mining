package mining.parser

import scalaj.http.Http
import scalaj.http.HttpOptions
import org.slf4j.LoggerFactory
import mining.io.Feed
import java.util.Date
import mining.util.DateUtil
import scalaj.http.HttpResponse

object Spider {

    def apply() = new Spider()

    val EMPTY_RSS_FEED =
        """
	<rss version="2.0">
	<channel>
		<title></title>
		<description></description>
		<link></link>
		<pubDate></pubDate>
	</channel>
	</rss>
        """
}

class Spider {

    import Spider._

    private val logger = LoggerFactory.getLogger(classOf[Spider])

    def syncFeedForContent(feed: Feed): (Feed,String) = {
        logger.info(s"Spider parsing ${feed.xmlUrl}")
        val browsingHeaders = Map(
            "User-Agent" -> "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36",
            "If-Modified-Since" -> DateUtil.getParser.format(feed.checked),
            "If-None-Match" -> feed.lastEtag
        )

        try {
            //TODO:this process is not reactive at all, maybe refactoring using future
            //TODO: request response header only

            val response: HttpResponse[String] =
                Http(feed.xmlUrl).headers(browsingHeaders)
                    .option(HttpOptions.connTimeout(5500))
                    .option(HttpOptions.readTimeout(5000))
                    .asString

            if (response.code != 200) {
                logger.info("parse error, with {}", response.code)
                return (feed,EMPTY_RSS_FEED)
            }

            //val ts  = lastupdate.toGMTString()
            //If-None-Match:"c7f731d5d3dce2e82282450c4fcae4d6"
            //not sure about last-modified
            //like test case1 (last-modified reponse header:,List(Tue, 09 Jul 2013 02:33:23 GMT))
            response.headers.get("Last-Modified") match {
                case Some(latest_ts) =>
                    //val latest_ts = value(0)
                    val t1 = DateUtil.getParser.parse(latest_ts)
                    val t0 = feed.checked
                    //TODO: time parsing is not stable, is it related with timezone issue as FA in CST

//                    if (t1.compareTo(t0) < 0) {
//                        logger.info(s"Spider parsing $url skip as timestamp $t1 still old ")
//                        return EMPTY_RSS_FEED
//                    }
                case _ =>
            }

            val newEncoding = response.headers.get("Content-Type").map{ charset=>
                val pat = "charset="
                val i = charset.indexOf(pat)
                if (i > 0) {
                    val encoding = charset.substring(i + pat.length(), charset.length)
                    encoding
                }
                else{
                    None
                }
            }.getOrElse("UTF-8")

            val newChecked = new Date
            val newEtag = response.headers.getOrElse("ETag", "")

            if (!newEtag.isEmpty && newEtag==feed.lastEtag) {
                logger.info("parse nothing, as etag didn't change {}", newEtag)
                return (feed,EMPTY_RSS_FEED)
            }
            (feed.copy(checked = newChecked),response.body)
        } catch {
            //FIXED: timeout, connection exception handling
            case ex: Throwable =>
                logger.error(s"Spider parsing ${feed.xmlUrl} exception as $ex: ")
                (feed,EMPTY_RSS_FEED)
        }
    }
}