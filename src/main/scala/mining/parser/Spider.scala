package mining.parser

import scalaj.http.Http
import scalaj.http.HttpOptions
import org.slf4j.LoggerFactory
import mining.io.Feed
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import mining.util.DateUtil
import java.sql.Timestamp
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

    def getFeedContent(feed: Feed): String = {
        val url = feed.url
        val lastEtag = feed.lastEtag
        logger.info(s"Spider parsing $url")
        val browsingHeaders = Map(
            "User-Agent" -> "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36",
            "If-Modified-Since" -> DateUtil.getParser.format(feed.checked),
            "If-None-Match" -> lastEtag
        )

        try {
            //TODO:this process is not reactive at all, maybe refactoring using future
            //TODO: request response header only

            val response: HttpResponse[String] =
                Http(url).headers(browsingHeaders)
                    .option(HttpOptions.connTimeout(5500))
                    .option(HttpOptions.readTimeout(5000))
                    .asString

            val (retcode, responseHeadersMap, resultString) =
                (response.code, response.headers, response.body)



            if (retcode == 304) return EMPTY_RSS_FEED
            if (retcode != 200) return EMPTY_RSS_FEED

            //val ts  = lastupdate.toGMTString()
            //If-None-Match:"c7f731d5d3dce2e82282450c4fcae4d6"
            //not sure about last-modified
            //like test case1 (last-modified reponse header:,List(Tue, 09 Jul 2013 02:33:23 GMT))
            responseHeadersMap.get("Last-Modified") match {
                case Some(latest_ts) => {
                    //val latest_ts = value(0)
                    val t1 = DateUtil.getParser.parse(latest_ts)
                    val t0 = feed.checked
                    //TODO: time parsing is not stable, is it related with timezone issue as FA in CST

//                    if (t1.compareTo(t0) < 0) {
//                        logger.info(s"Spider parsing $url skip as timestamp $t1 still old ")
//                        return EMPTY_RSS_FEED
//                    }
                }
                case _ =>
            }

            responseHeadersMap.get("Content-Type") match {
                case Some(charset) => {
                    //val charset = value(0)
                    val pat = "charset="
                    val i = charset.indexOf(pat)
                    if (i > 0) {
                        val encoding = charset.substring(i + pat.length(), charset.length)
                        feed.encoding = encoding
                    }
                }
                case _ =>
            }

            feed.checked = new Date()

            //if ETag [some hash like c7f731d5d3dce2e82282450c4fcae4d6 ] didn't change, then content didn't change
            responseHeadersMap.get("ETag") match {
                case Some(etag) => {
                    feed.lastEtag = etag //update etag
                    if (etag == lastEtag) {
                        logger.info(s"Spider parsing $url skip as ETag[$lastEtag] didn't change ")
                        return EMPTY_RSS_FEED
                    }
                }
                case _ =>
            }

            return resultString
        }
        catch {
            //FIXED: timeout, connection exception handling

            case ex: Throwable => {
                logger.error(s"Spider parsing $url exception as $ex: ")
                return EMPTY_RSS_FEED
            }
        }
    }
}