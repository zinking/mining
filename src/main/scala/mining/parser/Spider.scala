package mining.parser

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scalaj.http.{HttpResponse, Http, HttpOptions}
import org.slf4j.LoggerFactory
import mining.io.Feed
import java.util.Date
import mining.util.DateUtil
import mining.exception.{PageNotChangedException, ServerErrorException, ServerNotExistException}


object Spider {

    def apply() = new Spider()
}

class Spider {

    private val logger = LoggerFactory.getLogger(classOf[Spider])

    val MozillaUA = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 Chrome/28.0.1500.95 Safari/537.36"

    def getResponseString(feed: Feed, response:HttpResponse[String]):String = {
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


        val newEtag = response.headers.getOrElse("ETag", "")
        if (!newEtag.isEmpty && newEtag==feed.lastEtag) {
            logger.info("parse nothing, as etag didn't change {}", newEtag)
            throw PageNotChangedException(feed.xmlUrl)
        }
        response.body
    }

    def getEncoding(feed: Feed, response:HttpResponse[String]):String = {
        response.headers.get("Content-Type").map{ charset=>
            val pat = "charset="
            val i = charset.indexOf(pat)
            if (i > 0) {
                val encoding = charset.substring(i + pat.length(), charset.length)
                encoding
            }
            else{
                feed.encoding
            }
        }.getOrElse(feed.encoding)
    }

    def syncFeedForContent(feed: Feed): Future[HttpResponse[String]] = {
        val url = feed.xmlUrl
        logger.info("parsing {}", url)
        val browsingHeaders = Map(
            "User-Agent" -> MozillaUA,
            "If-Modified-Since" -> DateUtil.getParser.format(feed.checked),
            "If-None-Match" -> feed.lastEtag
        )


        Future{
            try{
                val response =
                    Http(feed.xmlUrl).headers(browsingHeaders)
                      .option(HttpOptions.connTimeout(5500))
                      .option(HttpOptions.readTimeout(5000))
                      .asString

                if (response.code == 404) {
                    logger.error("404 request {}, {}, {}", url, response.headers, response.body)
                    throw ServerNotExistException(url)
                }

                if (response.code != 200) {
                    logger.error("error request {}, {}, {}", url, response.headers, response.body)
                    throw ServerErrorException(feed.xmlUrl)
                }

                response
            } catch {
                case ex:ServerNotExistException   =>
                    throw ex
                case ex:ServerErrorException =>
                    throw ex
                case ex:Throwable =>
                    logger.error("error request {}, {}", url, ex.getMessage, ex)
                    throw ex
            }
        }
    }
}