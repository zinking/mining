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

@RunWith(classOf[JUnitRunner])
class SlickUserDAOSpec extends FunSuite 
			           with ShouldMatchers 
			           with BeforeAndAfterAll {
  Properties.setProp("runMode", "test")

  val userDAO = SlickUserDAO(H2Driver)
  val userID = "user1"

  override def beforeAll = {
    userDAO.manageDDL()
  }

  test("user should be able to save opml ") {
    val dom:Elem  = 
	<opml version="1.0">
		<head><title>Sample</title></head>
		<body>
			<outline text="We need more¡­¡­" title="We need more¡­¡­" type="rss"
				xmlUrl="http://blog.csdn.net/zhuliting/rss/list" htmlUrl="http://blog.csdn.net/zhuliting"/>
			<outline title="FlexBlogs" text="FlexBlogs">
				<outline text="AdobeAll-Bee" title="AdobeAll-Bee" type="rss"
					xmlUrl="http://www.beedigital.net/blog/?feed=rss2" htmlUrl="http://www.beedigital.net/blog"/>
			</outline>
		</body>
	</opml>
      
    val opml1:Opml =  Opml( userID, dom );
    userDAO.saveOpml( opml1 )

  }
  
  test("user should be able to retrieve opml ") {
	val opml = userDAO.getOpmlById("user1").get
	
	opml.id should be ( userID )
	val o1 = opml.outline.head
	o1.title should be ("We need more¡­¡­")
  }
  
  
}