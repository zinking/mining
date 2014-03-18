package mining.io

import java.io.File
import java.nio.file.FileSystems
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import mining.io.ser.SerFeedManager
import mining.io.ser.SerFeedReader
import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class FeedManagerSpec extends FunSuite 
                   	  with ShouldMatchers 
                   	  with BeforeAndAfterAll 
                   	  with FeedTestPrepare {

  override def beforeAll = {
	prepareFolders()
  }
  
  test("SerFeedManager should be able to store feed descriptor") {
	val sfm = SerFeedManager()
    val fd = sfm.createOrGetFeedDescriptor("http://coolshell.cn/feed")
  
    new File(fmPath).exists() should be (true)
  }
  
  test("SerFeedManager should be able to load feed descriptor and get descriptor from URL") {
    val feedUrl = "http://coolshell.cn/feed"
    val sfm = SerFeedManager()
    sfm.feedsMap.values.size should be (1) 
    sfm.loadDescriptorFromUrl(feedUrl).get.feedUrl should be (feedUrl)
  }
  
  test("FeedManager should be able to parse single url") {
    val url="http://coolshell.cn/feed"
    val feedManager = SerFeedManager()
    val rssFeed = feedManager.createOrUpdateFeed(url)
    
    rssFeed.rssItems.size should (be > 10)
  }
  
  test("SerFeedManager should be able to record the latest etag of a website"){
    val feedUrl = "http://coolshell.cn/feed"
    val sfm = SerFeedManager()
    sfm.feedsMap.values.size should be (1) 
    val letag = sfm.loadDescriptorFromUrl(feedUrl).get.lastEtag should not equal ("")
  }
  
  test("FeedManager should be able to parse opml format") {
    val feedManager = SerFeedManager()
    val sep = FileSystems.getDefault().getSeparator() 
    val tmpPath = new File(".").getCanonicalPath() + sep + "config" + sep + "zhen_opml.xml"
    val xml = XML.loadFile(tmpPath)
    feedManager.createOrUpdateFeedOPML(xml)
    
    Thread.sleep(10000)
    feedManager.feedsMap.size should be > (30)
  }
  
}