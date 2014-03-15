package mining.parser
import java.text.SimpleDateFormat;
import java.util.Date;
import scalaj.http.Http



class Spider {
  
  def getRssFeed( url:String, md:Map[String,String]):String = {
     
    //val ts  = lastupdate.toGMTString()
    //If-None-Match:"c7f731d5d3dce2e82282450c4fcae4d6"
    //ETag:"c7f731d5d3dce2e82282450c4fcae4d6"
    val lastEtag = md.getOrElse("ETag","-1" )
    
    val browsing_headers = Map(
        "User-Agent"->"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36",
        "If-Modified-Since"-> md.getOrElse("Last-Modified","" ),
        "If-None-Match" -> lastEtag
        )
    
   // val request = Http(url).headers(browsing_headers)
    val (retcode, response_headersMap, resultString) = 
      Http(url).headers(browsing_headers).asHeadersAndParse(Http.readString)
    
    if( retcode == 304 ) return ""
    
    //not sure about last-modified
    response_headersMap.get("Last-Modified") match{
      case Some( value ) => {
        println( "last-modified reponse header:",value )
      }
      case _ =>
    }
    
    //if etag didn't change, then content didn't change
    response_headersMap.get("ETag") match{
      case Some( value ) => {
        if ( value(0) == lastEtag ) return ""
      }
      case _ =>
    }
    
    //val last_modified = headersMap("Last-Modified")(0)
    

    //else if ( )
    //else return request.asString;
    return resultString
  }
  
  

}