
import java.nio.file.FileSystems
import java.io.File
import org.slf4j.LoggerFactory
import mining.io.FeedDescriptor
import mining.io.SerFeedReader
import mining.parser.RSSFeed


class FeedManager {
  
  def CreateOrUpdateFeed( url: String ){
    val fd = LoadFeedDescriptor( url )
    val serReader = SerFeedReader(fd)
    val map = serReader.read()
    val feed0 = map.get(fd)
    
    val feed1 = RSSFeed( fd )
    
    //feed0.update_feed
    
    
  }
  
  def CreateOrUpdateFeedOPML( ){
    
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