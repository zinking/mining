package mining.parser

import scalaj.http.Http
import scalaj.http.HttpOptions
import org.slf4j.LoggerFactory
import mining.io.FeedDescriptor
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object Spider {
  val EMPTY_RSS_FEED = """
	<rss version="2.0">
	<channel>
		<title></title>
		<description></description>
		<link></link>
		<pubDate></pubDate>
	</channel>
	</rss>
    """
  val TIME_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
} 

class Spider {
  import Spider._
  private val logger = LoggerFactory.getLogger(classOf[Spider])
  
  def getRssFeed(url: String, md: FeedDescriptor):String = {
    val lastEtag = md.lastEtag
    logger.info(s"Spider parsing $url  ")
    val browsingHeaders = Map(
        "User-Agent"->"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36",
        "If-Modified-Since"-> md.lastParseTimestamp,
        "If-None-Match" -> lastEtag
        )

    try{
    	//TODO:this process is not reactive at all, maybe refactoring using future
        //TODO: request response header only
	    val (retcode, responseHeadersMap, resultString) = 
	      Http(url).headers(browsingHeaders)
	      		   .option(HttpOptions.connTimeout(2500))
	      		   .option(HttpOptions.readTimeout(5000))
	      		   .asHeadersAndParse(Http.readString)
	    
	    
	      		   
	    if( retcode == 304 ) return EMPTY_RSS_FEED
	    if( retcode != 200 ) return EMPTY_RSS_FEED
	    
	    //val ts  = lastupdate.toGMTString()
	    //If-None-Match:"c7f731d5d3dce2e82282450c4fcae4d6"
	    //not sure about last-modified
	    //like test case1 (last-modified reponse header:,List(Tue, 09 Jul 2013 02:33:23 GMT))
	    responseHeadersMap.get("Last-Modified") match{
	      case Some( value ) => {
	         val latest_ts = value(0)
	         val t1 = TIME_FORMAT.parse(latest_ts)
	         val t0 = TIME_FORMAT.parse(md.lastParseTimestamp)
	         if( t1.compareTo(t0) < 0  ){
	           logger.info(s"Spider parsing $url skip as timestamp $t1 still old ")
	           return EMPTY_RSS_FEED 
	         }
	      }
	      case _ =>
	    }
	    
	    md.lastParseTimestamp = TIME_FORMAT.format( new Date() )
	    
	    //if ETag [some hash like c7f731d5d3dce2e82282450c4fcae4d6 ] didn't change, then content didn't change
	    responseHeadersMap.get("ETag") match{
	      case Some( value ) => {
	        md.lastEtag = value(0) //update etag
	        if ( value(0) == lastEtag ) {
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
        
        case ex:Throwable => {
          logger.error(s"Spider parsing $url exception as $ex: ")
          return EMPTY_RSS_FEED     
        }
        
        
        
    }
  }
  
  

}