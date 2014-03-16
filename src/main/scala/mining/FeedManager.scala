
import java.nio.file.FileSystems
import java.io.File
import org.slf4j.LoggerFactory
import mining.io.FeedDescriptor
import mining.io.SerFeedReader
import mining.parser.RSSFeed
import mining.io.SerFeedWriter

import scala.xml._


class FeedManager {
  
  def CreateOrUpdateFeed( url: String ){
    val fd = LoadFeedDescriptor( url )
    val feed0 = SerFeedReader(fd).read_feed();
    feed0.sync_feed(fd)
    SerFeedWriter(feed0).write();
    StoreFeedDescriptor(fd)
  }
  
  def CreateOrUpdateFeedOPML( root:Elem ){
    
  }
  
  def StoreFeedDescriptor( fd:FeedDescriptor ){//serialize
    //TODO:
  }
  
  def LoadFeedDescriptor( url:String ):FeedDescriptor={//deserialize stub
    //TODO:
    return FeedDescriptor( url )
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