package mining.io.slick

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import scala.util.Properties
import scala.slick.driver.H2Driver
import mining.io.Opml
import scala.xml.Elem
import mining.io.UserFactory

@RunWith(classOf[JUnitRunner])
class SlickIntegratedDAOSpec extends FunSuite
                             with ShouldMatchers 
                             with BeforeAndAfterAll {
  Properties.setProp("runMode", "test")

  val userDAO = SlickUserDAO(H2Driver)
  val feedDAO = SlickFeedDAO(H2Driver)

  val userId = "integUser"
  val feed1 = "http://coolshell.cn/feed"
  val feed2 = "http://www.beedigital.net/blog/?feed=rss2"

  override def beforeAll = {
    userDAO.manageDDL()
  }
  
  test("User info and his opml should be able to be saved correctly") {
    //Save new user
    val user = UserFactory.newUser(userId, "sth@gmail.com")
    userDAO.saveUser(user)

    val dom: Elem = 
    <opml version="1.0">
        <head><title>Sample</title></head>
        <body>
            <outline text="Coolshell" title="Coolshell" type="rss"
                xmlUrl="http://coolshell.cn/feed" htmlUrl="http://blog.csdn.net/zhuliting"/>
            <outline title="FlexBlogs" text="FlexBlogs">
                <outline text="AdobeAll-Bee" title="AdobeAll-Bee" type="rss"
                    xmlUrl="http://www.beedigital.net/blog/?feed=rss2" htmlUrl="http://www.beedigital.net/blog"/>
            </outline>
        </body>
    </opml>
      
    //Save user OPML
    val opml1: Opml = Opml(userId, dom);
    userDAO.saveOpml(opml1)    
    
    userDAO.getUserById(userId).get.userId should be (userId)
    userDAO.getOpmlById(userId).get.containsFeedUrl(feed1) should be (true)
    userDAO.getOpmlById(userId).get.containsFeedUrl(feed2) should be (true)
  }
  
  test("Should be able to sync feeds and get its stories") {
    val opml = userDAO.getOpmlById(userId).get 
    feedDAO.createOrUpdateFeedOPML(opml)
    Thread.sleep(3000) //TODO: Should use future
    feedDAO.read(feedDAO.loadFeedFromUrl("http://coolshell.cn/feed").get).size should be > (5)
    feedDAO.getOpmlStories(opml).size should be > (5)
  }
  
}