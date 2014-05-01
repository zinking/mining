package mining.io.slick

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.slick.driver.H2Driver
import mining.util.UrlUtil
import java.util.Date
import scala.xml.Elem
import mining.io.Opml
import scala.util.Properties
import mining.io.User
import mining.io.UserFactory

@RunWith(classOf[JUnitRunner])
class SlickUserDAOSpec extends FunSuite 
			           with ShouldMatchers 
			           with BeforeAndAfterAll {
  Properties.setProp("runMode", "test")

  val userDAO = SlickUserDAO(H2Driver)
  val userId = "user1"

  override def beforeAll = {
    userDAO.manageDDL()
  }
  

  test("User info should be saved") {
    val user = UserFactory.newUser(userId, "sth@g.com") 
    userDAO.saveUser(user)
  }

  test("User should be able to save opml ") {
    val dom: Elem  = 
	<opml version="1.0">
		<head><title>Sample</title></head>
		<body>
			<outline text="We need more..." title="We need more..." type="rss"
				xmlUrl="http://blog.csdn.net/zhuliting/rss/list" htmlUrl="http://blog.csdn.net/zhuliting"/>
			<outline title="FlexBlogs" text="FlexBlogs">
				<outline text="AdobeAll-Bee" title="AdobeAll-Bee" type="rss"
					xmlUrl="http://www.beedigital.net/blog/?feed=rss2" htmlUrl="http://www.beedigital.net/blog"/>
			</outline>
		</body>
	</opml>
      
    val opml1: Opml = Opml(userId, dom);
    userDAO.saveOpml(opml1)
  }
  
  test("User should be able to retrieve opml ") {
	val opml = userDAO.getOpmlById(userId).get
	
	opml.id should be (userId)
	val o1 = opml.outline.head
	o1.title should be ("We need more...")
  }
  
  test("The opml should be able to get updated") {
    val dom: Elem  = 
    <opml version="1.0">
        <head><title>Sample</title></head>
        <body>
            <outline text="We need more..." title="We need less..." type="rss"
                xmlUrl="http://blog.csdn.net/zhuliting/rss/list" htmlUrl="http://blog.csdn.net/zhuliting"/>
            <outline title="FlexBlogs" text="FlexBlogs">
                <outline text="AdobeAll-Bee" title="AdobeAll-Bee" type="rss"
                    xmlUrl="http://www.beedigital.net/blog/?feed=rss2" htmlUrl="http://www.beedigital.net/blog"/>
            </outline>
        </body>
    </opml>
    val opml2: Opml = Opml(userId, dom) 
    userDAO.saveOpml(opml2)
    userDAO.getOpmlById(userId).get.outline.head.title should be ("We need less...")
  }
  
  
}