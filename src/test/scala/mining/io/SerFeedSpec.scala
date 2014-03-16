package mining.io

import java.io.File
import java.nio.file.FileSystems
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import mining.parser.RSSFeed
import org.scalatest.junit.JUnitRunner
import mining.io.ser.SerFeedWriter
import mining.io.ser.SerFeedReader

@RunWith(classOf[JUnitRunner])
class SerFeedSpec extends FunSuite 
			      with ShouldMatchers 
			      with BeforeAndAfterAll {
  override def beforeAll = {
    val sep = FileSystems.getDefault().getSeparator() 
    val tmpPath = List(new File(".").getCanonicalPath(), "tmp", "ser") mkString(sep)
    val tmpFolder = new File(tmpPath)
    if (!tmpFolder.exists())
      tmpFolder.mkdir()

    System.setProperty("mining.ser.path",  tmpPath)
  }

  test("Ser feed write should be able to write rss items to file system") {
    val fd1 = FeedDescriptor("http://coolshell.cn/feed")
    val feed = RSSFeed(fd1) 
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
    val fd = FeedDescriptor("http://coolshell.cn/feed") 
    val serReader = SerFeedReader(fd)
    val rssFeed = serReader.read()
    
    rssFeed.rssItems.size should (be > 10)
  }
}