
import java.nio.file.FileSystems
import java.io.File
import org.slf4j.LoggerFactory
import mining.io.FeedDescriptor
import mining.io.SerFeedReader
import mining.parser.RSSFeed
import mining.io.SerFeedWriter

import scala.xml._


class FeedManager {
  
  def createOrUpdateFeed(url: String){
    val fd = loadFeedDescriptor( url )
    val feed0 = SerFeedReader(fd).read_feed();
    feed0.syncFeed(fd)
    SerFeedWriter(feed0).write();
    storeFeedDescriptor(fd)
  }
  
  def createOrUpdateFeedOPML(root:Elem){
    
  }
  
  def storeFeedDescriptor(fd:FeedDescriptor){//serialize
    //TODO:
  }
  
  def loadFeedDescriptor(url:String):FeedDescriptor={//deserialize stub
    //TODO:
    return FeedDescriptor(url)
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