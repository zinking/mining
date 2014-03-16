package mining

import java.io.File
import java.nio.file.FileSystems
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import mining.io.FeedDescriptor
import mining.io.SerFeedReader
import scala.xml.XML
import org.scalatest.junit.JUnitRunner
import mining.FeedManager

@RunWith(classOf[JUnitRunner])
class FeedManagerSpec extends FunSuite 
			      with ShouldMatchers 
			      with BeforeAndAfterAll {
  val feedManager = FeedManager()
  override def beforeAll = {
    val sep = FileSystems.getDefault().getSeparator() 
    val tmpPath = new File(".").getCanonicalPath() + sep + "tmp" + sep
    val tmpFolder = new File(tmpPath)
    if (!tmpFolder.exists()) tmpFolder.mkdir()
    
    ////A LITTLE DANGEROUS THOUGH
    //TODO: we need to delete all the files produced instead of using this
    for ( subfile <- tmpFolder.listFiles() ){ 
      subfile.delete()
    }

    System.setProperty("mining.ser.path",  tmpPath) //setting the path to temp under testing
  }
  
  test("FeedManager should be able to store feed descriptor") {
    
  }
  
  test("FeedManager should be able to load feed descriptor") {
    
  }

  test("FeedManager should be able to parse single url") {
    val url="http://coolshell.cn/feed"
    feedManager.createOrUpdateFeed( url )
    val fd = FeedDescriptor(url)
    val rssFeed = SerFeedReader(fd).read()
    
    rssFeed.rssItems.size should (be > 10)
  }
  
  test("FeedManager should be able to parse opml format") {
    val sep = FileSystems.getDefault().getSeparator() 
    val tmpPath = new File(".").getCanonicalPath() + sep + "config" + sep + "zhen_opml.xml"
    val xml = XML.loadFile(tmpPath)
    feedManager.createOrUpdateFeedOPML( xml )
  }
  
 
}