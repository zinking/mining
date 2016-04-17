package mining.io.slick

import java.util.Date

import mining.io.dao.{FeedDao, UserDao}
import mining.io._
import mining.util.DaoTestUtil
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FunSuite, ShouldMatchers}
import org.scalatest.junit.JUnitRunner


import scala.xml.Elem

@RunWith(classOf[JUnitRunner])
class SlickUserDAOSpec extends FunSuite
with ShouldMatchers
with BeforeAndAfterAll
with FeedTestPrepare {
    val db = "test"

    val userDAO = UserDao()
    val feedDAO = FeedDao()

    val userId = 2L

    override def afterAll = {
        DaoTestUtil.truncateAllTables
    }

    def opml2FolderMap(uo:Opml):Map[String,OpmlOutline] = {
        uo.outlines.map{opmlFeed=>
            if (opmlFeed.isFolder) {
                (opmlFeed.title, opmlFeed)
            } else {
                ("",opmlFeed)
            }
        }.toMap

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

    test("get suggested user should return") {
        val users = userDAO.getSuggestedUsersToFollow("sth")
        users.length should be(1)
        users.head.contains(UserDao.IdPrefix) should be(right = true)
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

                    <outline title="d1" text="d1">
                        <outline text="f1" title="f1" type="rss" xmlUrl="http://f1" htmlUrl="http://f1"/>
                        <outline text="f2" title="f2" type="rss" xmlUrl="http://f2" htmlUrl="http://f2"/>
                        <outline text="f5" title="f5" type="rss" xmlUrl="http://f5" htmlUrl="http://f5"/>
                    </outline>

                    <outline title="d2" text="d2">
                        <outline text="f3" title="f3" type="rss" xmlUrl="http://f3" htmlUrl="http://f3"/>
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
        newOpml.outlines(1).allOutlines.size should be(1)
        newOpml.outlines.last.xmlUrl should be(newFeedUrl)
    }

    test("User should be able to add opml outline with specified folder") {
        val newFeedUrl = "http://addflex.mine.co"
        val opmlOutline = OpmlOutline(List.empty,"AddedOutline",newFeedUrl,"rss","addblog",newFeedUrl)
        userDAO.addOmplOutline(userId,opmlOutline, "FlexBlogs")
        val newOpml = userDAO.getUserOpml(userId).get

        val f2O = opml2FolderMap(newOpml)
        f2O.get("FlexBlogs").get.outlines.size should be(2)
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

    test("User should be albe to apply a sequence of changes to opml") {
        val changes:List[OpmlChange] = List(
            OpmlChange("http://f5","NA","NA",true),
            OpmlChange("http://f2","d2","NA",false),
            OpmlChange("http://f3","d2","f3rename",false)
        )
        userDAO.applyOpmlChanges(userId,changes)
        val newOpml = userDAO.getUserOpml(userId).get
        val f2O = opml2FolderMap(newOpml)
        f2O.get("d1").get.outlines.size should be(1)
        f2O.get("d2").get.outlines.size should be(2)
    }

    test("User should be able to append his actions stats") {
        val userActStats = List(
            UserActionStat(new Date, "READ STORY", 1L, 10L, 100L, "DURATION,100"),
            UserActionStat(new Date, "READ STORY", 1L, 10L, 100L, "DURATION,100"),
            UserActionStat(new Date, "READ STORY", 1L, 10L, 100L, "DURATION,100")
        )
        userDAO.appendUserActStats(userActStats)

        val stats = userDAO.getUserActStatsByUser(1L)

        stats.size should be(3)
    }

    test("User should be albe to read his reading stats") {
        val userUnreads = userDAO.getUserFeedUnreadSummary(userId)
        userUnreads.size should be(0)
        val nowts = new Date().getTime
        val ts1 = new Date(nowts+10000000)
        val ts2 = new Date(nowts+20000000)
        val ts3 = new Date(nowts+30000000)
        val ts4 = new Date(nowts+40000000)

        val feed = Feed( // really serious test data
            "http://mining.com/users/zinking/rss",
            "user zinking's feed",
            "way too far",
            "http://mining.com/users/zinking",
            "RSS",
            0L,
            "SOMEETAG",
            ts1,
            "http://mining.com/users/zinking/posts/1",
            "UTF-8",0, 0, 0, 0, 0, 0
        )
        val createdFeed = feedDAO.insertOrUpdateFeed(feed)
        val story1 = Story( 0L, createdFeed.feedId, "post1", "link1", ts1, ts1, "a1", "", "" )
        val story2 = Story( 0L, createdFeed.feedId, "post2", "link2", ts2, ts2, "a2", "", "" )
        val story3 = Story( 0L, createdFeed.feedId, "post3", "link3", ts3, ts3, "a3", "", "" )
        val createdStory1 = feedDAO.insertFeedStory(createdFeed,story1)
        val createdStory2 = feedDAO.insertFeedStory(createdFeed,story2)
        val createdStory3 = feedDAO.insertFeedStory(createdFeed,story3)

        //val userFeedStat = UserFeedReadStat(userId,createdFeed.feedId,0,ts1)
        val userFeedStat = UserStat(userId,createdFeed.feedId,0L,0,0,"")
        userDAO.insertUserStat(userFeedStat)

        // all stories created but not read
        val currentUnreads1 = userDAO.getUserFeedUnreadSummary(userId)
        currentUnreads1.size should be(1)
        val currentUnread = currentUnreads1.head
        currentUnread.unreadCount should be(3)

        // user read 1 story
        val userFeedStat1 = UserStat(userId,createdFeed.feedId,createdStory1.id,1,0,"")
        userDAO.insertUserStat(userFeedStat1)
        val currentUnread2 = userDAO.getUserFeedUnreadSummary(userId).head
        currentUnread2.unreadCount should be(2)

        // user mark feed all read at ts1
        userDAO.markUserReadFeedAt(userId, createdFeed.feedId, ts1)
        val currentUnread3 = userDAO.getUserFeedUnreadSummary(userId).head
        currentUnread3.unreadCount should be(2)

        // user mark feed all read at ts3
        userDAO.markUserReadFeedAt(userId, createdFeed.feedId, ts3)
        val currentUnread4 = userDAO.getUserFeedUnreadSummary(userId).head
        currentUnread4.unreadCount should be(0)
    }


}