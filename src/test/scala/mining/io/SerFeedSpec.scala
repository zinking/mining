package mining.io

import java.io.File
import java.nio.file.FileSystems
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import mining.io.ser.SerFeedWriter
import mining.io.ser.SerFeedReader
import mining.io.ser.SerFeedManager
import mining.parser.FeedParser

@RunWith(classOf[JUnitRunner])
class SerFeedSpec extends FunSuite 
			      with ShouldMatchers 
			      with BeforeAndAfterAll 
			      with FeedTestPrepare {

  override def beforeAll = {
    prepareFolders()
  }

  test("Ser feed write should be able to write rss items to file system") {
    val feed = FeedParser(FeedFactory.newFeed("http://coolshell.cn/feed"))
    feed.syncFeed()
    val serWriter = SerFeedWriter(feed)
    
    //do the clean up first
    val serFile = new File(serWriter.feedDescriptor.filePath)
    if (serFile.exists())
      serFile.delete() 

    serWriter.write()
    
    serFile.exists() should be (true)
  }
  
  test("Ser feed reader should be able to read rss items according to feed descriptor") {
    val serReader = SerFeedReader(FeedFactory.newFeed("http://coolshell.cn/feed"))
    val rssFeed = serReader.read()
    
    rssFeed.stories.size should (be > 10)
  }
}