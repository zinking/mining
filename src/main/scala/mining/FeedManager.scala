package mining

import java.nio.file.FileSystems
import java.io.File
import org.slf4j.LoggerFactory
import mining.io.FeedDescriptor
import mining.io.SerFeedReader
import mining.parser.RSSFeed
import mining.io.SerFeedWriter

import scala.xml._


class FeedManager {
  
  def createOrUpdateFeed(url: String) = {
    val fd = loadFeedDescriptor(url)
    val rssFeed = SerFeedReader(fd).read()
    rssFeed.syncFeed()
    SerFeedWriter(rssFeed).write()
    storeFeedDescriptor(fd)
  }
  
  def createOrUpdateFeedOPML(root:Elem){
    val rssOutNodes = root \\ "outline" filter{node => (node \ "@type").text == "rss"}
    for(  rssOutNode <- rssOutNodes ){
      val url = ( rssOutNode \ "@xmlUrl" ).text 
      createOrUpdateFeed( url )
    }

  }
  
  def storeFeedDescriptor(fd: FeedDescriptor) = {

  }
  
  def loadFeedDescriptor(url: String): FeedDescriptor = {
    FeedDescriptor(url)
  }

}


object FeedManager {
  private val logger = LoggerFactory.getLogger(classOf[FeedManager])
  def apply( ) = {
    try{
	    val sep = FileSystems.getDefault().getSeparator() 
	    //TODO: maybe make this configurable
	    val RepoPath = new File(".").getCanonicalPath() + sep + "feedrepo" + sep
	    val RepoFolder = new File(RepoPath)
	    if (!RepoFolder.exists()) RepoFolder.mkdir()
	    System.setProperty("mining.ser.path",  RepoPath)
	}
	catch {
	   case ex: Exception => logger.error(s"Initializing feed manager error", ex)
		  				     throw ex
	}
	finally {
	   
	}
    new FeedManager()
  }
}