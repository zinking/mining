package mining.io.slick

import mining.io.dao.UserDao
import org.scalatest.FunSuite
import org.scalatest.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import mining.util.{DaoTestUtil, UrlUtil}
import scala.xml.Elem
import mining.io.{OpmlOutline, Opml, UserFactory, FeedTestPrepare}

@RunWith(classOf[JUnitRunner])
class SlickUserDAOSpec extends FunSuite
with ShouldMatchers
with BeforeAndAfterAll
with FeedTestPrepare {
    val db = "test"

    val userDAO = UserDao()

    val userId = 2L

    override def afterAll = {
        DaoTestUtil.truncateAllTables
    }


    test("User info should be saved") {
        val user = UserFactory.newUser(userId, "sth@g.com")
        userDAO.saveUser(user)
    }

    test("User should be able to save opml ") {
        val dom: Elem =
            <opml version="1.0">
                <head>
                    <title>Sample</title>
                </head>
                <body>
                    <outline text="We need more..." title="We need more..." type="rss"
                             xmlUrl="http://blog.csdn.net/zhuliting/rss/list" htmlUrl="http://blog.csdn.net/zhuliting"/>
                    <outline title="FlexBlogs" text="FlexBlogs">
                        <outline text="AdobeAll-Bee" title="AdobeAll-Bee" type="rss"
                                 xmlUrl="http://www.beedigital.net/blog/?feed=rss2" htmlUrl="http://www.beedigital.net/blog"/>
                    </outline>
                </body>
            </opml>

        val opml1: Opml = Opml(userId, dom)
        userDAO.setUserOpml(opml1)
    }

    test("User should be able to retrieve opml ") {
        val opml = userDAO.getUserOpml(userId).get

        opml.id should be(userId)
        val o1 = opml.outlines.head
        o1.title should be("We need more...")
    }

    test("The opml should be able to get updated") {
        val dom: Elem =
            <opml version="1.0">
                <head>
                    <title>Sample</title>
                </head>
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
        userDAO.setUserOpml(opml2)
        userDAO.getUserOpml(userId).get.outlines.head.title should be("We need less...")
    }

    test("User should be able to add opml outline") {
        val newFeedUrl = "http://add.mine.co"
        val opmlOutline = OpmlOutline(List.empty,"AddedOutline",newFeedUrl,"rss","addblog",newFeedUrl)
        userDAO.addOmplOutline(userId,opmlOutline)
        val newOpml = userDAO.getUserOpml(userId).get
        newOpml.outlines.last.xmlUrl should be(newFeedUrl)
    }

    test("User should be able to remove opml outline") {
        val newFeedUrl = "http://add.mine.co"
        val curOpml = userDAO.getUserOpml(userId).get
        val outlineCount = curOpml.outlines.length
        userDAO.removeOmplOutline(userId,newFeedUrl)
        val newOpml = userDAO.getUserOpml(userId).get
        newOpml.outlines.last.xmlUrl should not be(newFeedUrl)
        newOpml.outlines.length should be(outlineCount-1)
    }


}