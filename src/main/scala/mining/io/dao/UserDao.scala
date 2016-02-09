package mining.io.dao

import java.io.{PrintWriter, File}
import java.sql.{SQLException, Timestamp, ResultSet, Connection}
import java.util
import java.util.Date

import com.typesafe.config.{ConfigFactory, Config}

import org.slf4j.{LoggerFactory, Logger}

import scala.io.Source
import scala.xml.XML
import scala.collection.JavaConverters._

import mining.io._

/**
 * user related database object operation
 * Created by awang on 5/12/15.
 */
object UserDao {
    def apply() = {
        new UserDao()
    }

    /**
     * convert database result to user object
     * @param rs result set 
     * @return user object
     */
    def resultToUser(rs: ResultSet): User = {
        User(
            rs.getLong("USER_ID"),
            rs.getString("EMAIL"),
            rs.getString("PREF")
        )
    }

    /**
     * convert database result to user story stats
     * @param rs database result set
     * @return user story stats
     */
    def resultToUserStory(rs: ResultSet): UserStat = {
        UserStat(
            rs.getLong("USER_ID"),
            rs.getLong("FEED_ID"),
            rs.getLong("STORY_ID"),
            rs.getInt("HASREAD"),
            rs.getInt("HASLIKE"),
            rs.getString("COMMENT")
        )
    }

    /**
     * convert database result to user opml 
     * @param rs database result set
     * @return user opml object
     */
    def resultToOpml(rs:ResultSet):Opml={
        val os=OpmlStorage(
            rs.getLong("USER_ID"),
            rs.getString("RAW")
        )
        os.toOpml
    }
}

class UserDao() extends Dao {
    import UserDao._
    override def log: Logger = LoggerFactory.getLogger(classOf[UserDao])

    /**
     * create user object in data store
     * @param user user object to be created
     */
    def saveUser(user: User): Unit = {
        val q = "INSERT INTO USER_INFO (user_id,email,pref) VALUES (?,?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, user.userId)
                statement.setString(2, user.email)
                statement.setString(3, user.prefData)
                statement.executeUpdate()
            }
        }

        //User will also follow his own favorite
        setUserFollow(user.userId, user.userId)
    }

    /**
     * update user object in data store
     * @param user user object to be updated
     */
    def updateUser(user: User): Unit = {
        val q = "update USER_INFO set pref=? where user_id = ?"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, user.prefData)
                statement.setLong(2, user.userId)
                statement.executeUpdate()
            }
        }
    }

    /**
     * get user by user id
     * @param uid user id
     * @return user object option
     */
    def getUser(uid: Long): Option[User] = {
        val q = "select * from USER_INFO where user_id = ? "
        val result = new util.ArrayList[User]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uid)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val user = resultToUser(rs)
                        result.add(user)
                    }
                }
            }
        }
        if (result.size() > 0) {
            Some(result.get(0))
        }
        else {
            None
        }
    }

    /**
     * get user by user email
     * @param email user email
     * @return user object option
     */
    def getUserByEmail(email: String): Option[User] = {
        val q = "select * from USER_INFO where email = ? "
        val result = new util.ArrayList[User]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, email)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val user = resultToUser(rs)
                        result.add(user)
                    }
                }
            }
        }
        if (result.size() > 0) {
            Some(result.get(0))
        }
        else {
            None
        }
    }

    /**
     * get user stats
     * @param uid user id
     * @param fdid feed id
     * @param sid story id
     * @return user stat info
     */
    def getUserStat(uid: Long, fdid:Long, sid: Long): Option[UserStat] = {
        val q = "select * from USER_STAT where user_id = ? and story_id = ? and feed_id = ?"
        val result = new util.ArrayList[UserStat]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uid)
                statement.setLong(2, sid)
                statement.setLong(3, fdid)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val userStory = resultToUserStory(rs)
                        result.add(userStory)
                    }
                }
            }
        }
        if (result.size() > 0) {
            Some(result.get(0))
        }
        else {
            None
        }
    }

    /**
     * get the user following id list
     * @param userId the user id
     * @return a list of the user ids that user is following
     */
    def getFollowing(userId: Long): List[Long] = {
        val q = "select following from USER_FOLLOW where user_id = ?"
        val result = new util.ArrayList[Long]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, userId)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        result.add(rs.getLong(1))
                    }
                }
            }
        }
        result.asScala.toList
    }

    /**
     * get the user following id list
     * @param userId the user id
     * @return a list of the user ids that user is following
     */
    def getFollowingUsers(userId: Long): List[User] = {
        val q = "select ui.* from USER_FOLLOW uf join USER_INFO ui on uf.following = ui.user_id where uf.user_id = ?"
        val result = new util.ArrayList[User]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, userId)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val user = resultToUser(rs)
                        result.add(user)
                    }
                }
            }
        }
        result.asScala.toList
    }

    /**
     * get the specified user follow record
     * @param uf user following record
     * @return option of the user following record
     */
    def getUserFollow(uf: UserFollow) = {
        val q = "select * from USER_FOLLOW where user_id = ? and following = ?"
        val result = new util.ArrayList[UserFollow]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uf.userId)
                statement.setLong(2, uf.following)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val userFollowing = UserFollow(rs.getLong(1), rs.getLong(2))
                        result.add(userFollowing)
                    }
                }
            }
        }
        if (result.size() > 0) {
            Some(result.get(0))
        }
        else {
            None
        }
    }

    /**
     * create user following record 
     * @param uf user following record
     */
    def insertUserFollow(uf: UserFollow) = {
        val q = "INSERT INTO USER_FOLLOW (user_id,following) VALUES (?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uf.userId)
                statement.setLong(2, uf.following)
                statement.executeUpdate()
            }
        }
    }

    /**
     * set the user follow record
     * @param userId user id
     * @param follow following user id
     */
    def setUserFollow(userId:Long, follow:Long): Unit = {
        val uf = UserFollow(userId, follow)
        getUserFollow(uf) match {
            case Some(us) =>
            case None =>
                insertUserFollow(uf)
        }
    }

    /**
     * create user stat object
     * @param us user stat
     */
    def insertUserStat(us:UserStat)={
        val q = "INSERT INTO USER_STAT (user_id,story_id,hasread,haslike,comment,feed_id) VALUES (?,?,?,?,?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, us.userId)
                statement.setLong(2, us.storyId)
                statement.setInt(3, us.hasRead)
                statement.setInt(4, us.hasLike)
                statement.setString(5, us.comment)
                statement.setLong(6, us.feedId)
                statement.executeUpdate()
            }
        }
    }

    /**
     * update user story to `read`
     * @param uid user id
     * @param fdid feed id
     * @param sid story id
     * @param hasRead read flag: 1 read, 0 unread
     */
    def updateUserStoryRead(uid: Long, fdid: Long, sid: Long, hasRead: Int) = {
        val q = "update USER_STAT set hasread=? where user_id=? and story_id=? and feed_id=?"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setInt(1, hasRead)
                statement.setLong(2, uid)
                statement.setLong(3, sid)
                statement.setLong(4, fdid)
                statement.executeUpdate()
            }
        }
    }

    /**
     * update user story to `like`
     * @param uid user id
     * @param fdid feed id
     * @param sid story id
     * @param haslike like flag: 1 like, 0 default
     */
    def updateUserStoryLike(uid: Long, fdid: Long, sid: Long, haslike: Int) = {
        val q = "update USER_STAT set haslike=? where user_id=? and story_id=? and feed_id=?"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setInt(1, haslike)
                statement.setLong(2, uid)
                statement.setLong(3, sid)
                statement.setLong(4, fdid)
                statement.executeUpdate()
            }
        }
    }

    /**
     * set user has read the story, create record if absent
     * @param userId user id
     * @param fdid feed id
     * @param storyId story id
     * @param read read flag
     */
    def setUserReadStat(userId: Long, fdid: Long, storyId: Long, read: Int): Unit = {
        getUserStat(userId, fdid, storyId) match {
            case Some(us) =>
                updateUserStoryRead(userId, fdid, storyId, read)
            case None =>
                insertUserStat(UserStat(userId, fdid, storyId, read, 0, ""))
        }
    }

    /**
     * get user starred stories
     * @param userId user id
     * @param pageSize page size of the starred stories
     * @param pageNo page number
     * @return a list of starred stories
     */
    def getUserStarStories(userId: Long, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
        val q =
            s"""select fs.* from FEED_STORY fs join USER_STAT us on fs.story_id=us.story_id
               where us.user_id=$userId and us.haslike>0
               order by fs.updated desc limit ${pageNo * pageSize},$pageSize""".stripMargin
        val result = new util.ArrayList[Story]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                using(statement.executeQuery(q)) { rs =>
                    while (rs.next) {
                        val story = FeedDao.resultToStory(rs)
                        //story.copy(feedId = -userId)
                        result.add(story)
                    }
                }
            }
        }
        result.asScala.toList
    }

    /**
     * get the user opml object
     * @param uid user id
     * @return the user opml
     */
    def getUserOpml(uid: Long): Option[Opml] = {
        val q = "select USER_ID,RAW from USER_OPML where user_id = ?"
        val result = new util.ArrayList[Opml]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uid)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val userOpml = resultToOpml(rs)
                        result.add(userOpml)
                    }
                }
            }
        }
        if (result.size() > 0) {
            Some(result.get(0))
        }
        else {
            None
        }
    }

    /**
     * create the the user opml object
     * @param uo user opml object
     */
    def insertUserOmpl(uo: Opml) = {
        val uos = uo.toStorage
        val q = "INSERT INTO USER_OPML (user_id,raw) VALUES (?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uos.id)
                statement.setString(2, uos.raw)
                statement.executeUpdate()
            }
        }
    }

    /**
     * update the user opml
     * @param uo user opml object
     */
    def updateUserOpml(uo: Opml) = {
        val uos = uo.toStorage
        val q = "update USER_OPML set raw=? where user_id=?"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, uos.raw)
                statement.setLong(2, uos.id)
                statement.executeUpdate()
            }
        }
    }

    /**
     * update the user opml object, create if absent
     * @param uo user opml object
     */
    def setUserOpml(uo: Opml): Unit = {
        getUserOpml(uo.id) match {
            case Some(uoo) => updateUserOpml(uo)
            case None => insertUserOmpl(uo)
        }
    }

    /**
     * merge with user's current opml object
     * @param uo new user opml object
     * @return the merged user opml object
     */
    def mergeWithUserOpml(uo: Opml): Opml = {
        getUserOpml(uo.id) match {
            case Some(uoo) =>
                val merged = uoo.mergeWith(uo)
                updateUserOpml(merged)
                merged
            case None =>
                setUserOpml(uo)
                uo
        }
    }

    /**
     * add one opml outine to the opml object
     * TODO: Opml structure should be updated in client side and save the whole Opml here
     * @param uid  user id
     * @param ol opml outline
     */
    def addOmplOutline(uid: Long, ol: OpmlOutline): Unit = {
        getUserOpml(uid) match {
            case Some(uo) =>
                if (!ol.xmlUrl.isEmpty) {
                    val curOpmlOutlines = uo.outlines.filter(_.xmlUrl == ol.xmlUrl)
                    if (curOpmlOutlines.isEmpty) {
                        val curopml = Opml(uid, uo.outlines :+ ol)
                        updateUserOpml(curopml)
                    }
                }
            case None =>
                val newopml = Opml(uid, List(ol))
                insertUserOmpl(newopml)
        }
    }

    /**
     * remove the specified user opml outline from opml
     * @param uid user id
     * @param xmlUrl url of the outline
     */
    def removeOmplOutline(uid: Long, xmlUrl: String): Unit = {
        getUserOpml(uid) match {
            case Some(uo) =>
                val newOpmlOutlines = uo.outlines.filter(!_.xmlUrl.startsWith(xmlUrl))
                val curopml = Opml(uid, newOpmlOutlines)
                updateUserOpml(curopml)
            case _ =>
        }
    }

    def markUserReadFeed(uid: Long, feedId: Long): Unit = {
        val q = "update USER_STAT set START_FROM=? where user_id=? and feed_id = ? and story_id = 0"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setTimestamp(1,new Timestamp(new Date().getTime))
                statement.setLong(2,uid)
                statement.setLong(3,feedId)
                statement.executeUpdate()
            }
        }
    }

    def getUserStarStories(uid: Long, storyIds: List[Long]): List[Long] = {
        if (storyIds.isEmpty) {
            List.empty
        } else{
            val qStoryIds = storyIds.mkString(",")
            val q = s"select story_id from USER_STAT where user_id = ? and haslike=1 and story_id in ($qStoryIds)"
            val result = new util.ArrayList[Long]
            using(JdbcConnectionFactory.getPooledConnection) { connection =>
                using(connection.prepareStatement(q)) { statement =>
                    statement.setLong(1, uid)
                    using(statement.executeQuery()) { rs =>
                        while (rs.next) {
                            result.add(rs.getLong(1))
                        }
                    }
                }
            }
            result.asScala.toList
        }

    }
    def getUserReadStories(uid: Long, storyIds: List[Long]): List[Long] = {
        if (storyIds.isEmpty) {
            List.empty
        }
        else{
            val qStoryIds = storyIds.mkString(",")
            val q =
                s"""
                   |SELECT
                   |  U.STORY_ID
                   |FROM
                   |  FEED_STORY F
                   |  JOIN
                   |  USER_STAT U
                   |  ON
                   |    F.FEED_ID = U.FEED_ID AND
                   |    F.STORY_ID = U.STORY_ID
                   |  JOIN
                   |  USER_STAT UU
                   |  ON
                   |    F.FEED_ID = UU.FEED_ID AND
                   |    UU.STORY_ID = 0
                   |WHERE
                   |  F.PUBLISHED > UU.START_FROM AND
                   |  U.USER_ID = ? AND U.STORY_ID IN ($qStoryIds)
          """.stripMargin
            val result = new util.ArrayList[Long]
            using(JdbcConnectionFactory.getPooledConnection) { connection =>
                using(connection.prepareStatement(q)) { statement =>
                    statement.setLong(1, uid)
                    using(statement.executeQuery()) { rs =>
                        while (rs.next) {
                            result.add(rs.getLong(1))
                        }
                    }
                }
            }

            result.asScala.toList
        }
    }

    /**
     * get the user subscribed feeds read summary
     * @param uid user id
     * @return user feed read stats
     */
    def getUserFeedUnreadSummary(uid: Long): List[UserFeedReadStat] = {
      val q =
        """
          |SELECT
          |  TS.FEED_ID,
          |  TS.STORY_COUNT,
          |  COALESCE(US.READ_COUNT,0) AS READ_COUNT,
          |  TS.STORY_COUNT - COALESCE(US.READ_COUNT,0) AS UNREAD_COUNT,
          |  TS.START_FROM
          |FROM
          |((
          |SELECT -- get user subscribed feeds and its story count
          |  F.FEED_ID,
          |  COUNT(*) AS STORY_COUNT,
          |  MAX(UU.START_FROM) AS START_FROM
          |FROM
          |  FEED_STORY F
          |  JOIN
          |  USER_STAT UU
          |  ON
          |    F.FEED_ID = UU.FEED_ID AND
          |    UU.STORY_ID = 0
          |WHERE
          |  F.PUBLISHED > UU.START_FROM AND
          |  UU.USER_ID = ?
          |GROUP BY
          |  F.FEED_ID
          |) TS
          |LEFT JOIN
          |(SELECT
          |  F.FEED_ID,
          |  COUNT(*) AS READ_COUNT
          |FROM
          |  FEED_STORY F
          |  JOIN
          |  USER_STAT U
          |  ON
          |    F.FEED_ID = U.FEED_ID AND
          |    F.STORY_ID = U.STORY_ID
          |  JOIN
          |  USER_STAT UU
          |  ON
          |    F.FEED_ID = UU.FEED_ID AND
          |    UU.STORY_ID = 0
          |WHERE
          |  F.PUBLISHED > UU.START_FROM AND
          |  U.USER_ID = ?
          |GROUP BY
          |	F.FEED_ID
          |) US
          |ON
          |  TS.FEED_ID = US.FEED_ID
          |)
        """.stripMargin

        val result = new util.ArrayList[UserFeedReadStat]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uid)
                statement.setLong(2, uid)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        //val startFromTS = Option(rs.getTimestamp(5)).getOrElse(0)
                        val readStat:UserFeedReadStat = UserFeedReadStat(
                            uid,  rs.getLong(1),  rs.getInt(4),  new Date(rs.getTimestamp(5).getTime)
                        )
                        result.add(readStat)
                    }
                }
            }
        }
        result.asScala.toList
    }
}
