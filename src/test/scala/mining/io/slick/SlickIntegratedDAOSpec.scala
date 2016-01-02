package mining.io.slick

import mining.io.dao.{FeedDao, UserDao}
import mining.util.DaoTestUtil
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import mining.io.Opml
import scala.xml.Elem
import mining.io.UserFactory
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class SlickIntegratedDAOSpec extends FunSuite
with ShouldMatchers
with BeforeAndAfterAll {
    val userDAO = UserDao()
    val feedDAO = FeedDao()

    val userId = 1L
    val feed1 = "http://coolshell.cn/feed"
    val feed2 = "http://www.beedigital.net/blog/?feed=rss2"

    override def beforeAll() = {
        DaoTestUtil.truncateAllTables()
    }

    test("User info and his opml should be able to be saved correctly") {
        //Save new user
        val user = UserFactory.newUser(userId, "sth@gmail.com")
        userDAO.saveUser(user)

        val dom: Elem =
            <opml version="1.0">
                <head>
                    <title>Sample</title>
                </head>
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
        userDAO.setUserOpml(opml1)

        userDAO.getUser(userId).get.userId should be(userId)
        userDAO.getUserOpml(userId).get.containsFeedUrl(feed1) should be(true)
        userDAO.getUserOpml(userId).get.containsFeedUrl(feed2) should be(true)
    }

    test("Should be able to sync feeds and get its stories") {
        val opml = userDAO.getUserOpml(userId).get
        feedDAO.createOrUpdateFeedOPML(opml) onSuccess {
            case feeds =>
                val feed = feeds.head
                feedDAO.getStoriesFromFeed(feed).size should be > (5)
                feedDAO.getOpmlStories(opml).size should be > (5)
        }
    }

}