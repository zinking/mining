package mining.io.dao

import java.sql.{Timestamp, ResultSet}
import java.util
import java.util.{Calendar, Date}


import org.slf4j.{LoggerFactory, Logger}

import scala.collection.JavaConverters._
import scala.collection.mutable

import mining.io._

/**
 * user related database object operation
 * Created by awang on 5/12/15.
 */
object UserDao {
    val IdPrefix = "http://readmine.co/users?email="

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

    def resultToUserActStat(rs:ResultSet):UserActionStat = {
        UserActionStat(
            new Date(rs.getTimestamp("TS").getTime),
            rs.getString("ACT"),
            rs.getLong("USER_ID"),
            rs.getLong("FEED_ID"),
            rs.getLong("STORY_ID"),
            rs.getString("CONTENT")
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
    override def logger: Logger = LoggerFactory.getLogger(classOf[UserDao])

    /**
     * create user object in data store
     * @param user user object to be created
     */
    def saveUser(user: User): Unit = {
        val q = "INSERT INTO USER_INFO (user_id,email,pref) VALUES (?,?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
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
                logger.debug(q)
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
                logger.debug(q)
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
                logger.debug(q)
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
                logger.debug(q)
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
                logger.debug(q)
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
                logger.debug(q)
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
                logger.debug(q)
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
                logger.debug(q)
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
        val q = "INSERT INTO USER_STAT (user_id,story_id,hasread,haslike,comment,feed_id,start_from) VALUES (?,?,?,?,?,?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                val now = new Date()
                statement.setLong(1, us.userId)
                statement.setLong(2, us.storyId)
                statement.setInt(3, us.hasRead)
                statement.setInt(4, us.hasLike)
                statement.setString(5, us.comment)
                statement.setLong(6, us.feedId)
                statement.setTimestamp(7, new Timestamp(now.getTime))
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
                logger.debug(q)
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
                logger.debug(q)
                statement.setInt(1, haslike)
                statement.setLong(2, uid)
                statement.setLong(3, sid)
                statement.setLong(4, fdid)
                statement.executeUpdate()
            }
        }
    }
    
    def getUserActStatsByUser(userId: Long): List[UserActionStat] = {
        val q = "select * from USER_ACTION ua where ua.user_id = ?"
        val result = new util.ArrayList[UserActionStat]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, userId)
                logger.debug(q)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val userAct = resultToUserActStat(rs)
                        result.add(userAct)
                    }
                }
            }
        }
        result.asScala.toList
    }

    def getSuggestedUsersToFollow(query: String): List[String] = {
        val q = "select EMAIL from USER_INFO ui where ui.email like ? limit 10"
        val result = new util.ArrayList[String]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                val queryPhrase = s"%$query%"
                statement.setString(1, queryPhrase)
                logger.debug(q)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val email = rs.getString(1)
                        result.add(IdPrefix + email)
                    }
                }
            }
        }
        result.asScala.toList
    }



    def appendUserActStats(stats:List[UserActionStat]) = {
        val q = "insert into USER_ACTION values(?, ?, ?, ?, ?, ?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            connection.setAutoCommit(false)
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                stats.foreach({stat=>
                    statement.setTimestamp(1, new Timestamp(stat.timeStamp.getTime))
                    statement.setString(2, stat.action)
                    statement.setLong(3, stat.userId)
                    statement.setLong(4, stat.feedId)
                    statement.setLong(5, stat.storyId)
                    statement.setString(6, stat.content)
                    statement.executeUpdate()
                })
            }
            connection.commit()
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
     * set user has read the story, create record if absent
     * @param userId user id
     * @param fdid feed id
     */
    def setUserFeedStat(userId: Long, fdid: Long): Unit = {
        getUserStat(userId, fdid, 0) match {
            case Some(us) =>
            case None =>
                insertUserStat(UserStat(userId, fdid, 0, 0, 0, ""))
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
                    logger.debug(q)
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

    def getUserFeed(userId: Long, feedId: Long): Option[Long]= {
        val q = "select USER_ID,FEED_ID from USER_FEED where user_id = ? AND FEED_ID = ?"
        val result = new util.ArrayList[Long]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, userId)
                statement.setLong(2, feedId)
                using(statement.executeQuery()) { rs =>
                    logger.debug(q)
                    while (rs.next) {
                        result.add(rs.getLong(2))
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
     * update the user opml object, create if absent
     * @param userId user id
     * @param feedId feed id
     */
    def setUserFeed(userId: Long, feedId: Long): Unit = {
        getUserFeed(userId, feedId) match {
            case None => insertUserFeed(userId, feedId)
            case Some(uoo) =>

        }
    }

    /**
     * create a record to indicate user subscribed a feed
     * @param userId the user
     * @param feedId the feed
     */
    def insertUserFeed(userId: Long, feedId:Long) = {
        val q = "INSERT INTO USER_FEED (user_id,feed_id) VALUES (?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                statement.setLong(1, userId)
                statement.setLong(2, feedId)
                statement.executeUpdate()
            }
        }
    }


    /**
     * remove the record if user unsubscribed a feed
     * @param userId the user
     * @param feedId the feed
     */
    def removeUserFeed(userId:Long, feedId:Long) = {
        val q = "DELETE FROM USER_FEED WHERE USER_ID = ? AND FEED_ID = ?"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                statement.setLong(1, userId)
                statement.setLong(2, feedId)
                statement.executeUpdate()
            }
        }
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
                    logger.debug(q)
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
                logger.debug(q)
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
                logger.debug(q)
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
    def addOmplOutline(uid: Long, ol: OpmlOutline, folder: String = ""): Unit = {
        getUserOpml(uid) match {
            case Some(uo) =>
                if (!ol.xmlUrl.isEmpty) { //validation, valid opmlOutlineItem
                    val curOpmlOutlines = uo.outlines.filter(_.xmlUrl == ol.xmlUrl)
                    if (curOpmlOutlines.isEmpty) { //if current ol doesn't exist yet
                        if (folder.isEmpty) {
                            //if the opml is not included in folder structure
                            val curopml = Opml(uid, uo.outlines :+ ol)
                            updateUserOpml(curopml)
                        } else {
                            val outlineFolder = OpmlOutline(List(ol),folder,folder,"","","")
                            val deltaOpml = Opml(uid,List(outlineFolder))
                            val newOpml = uo.mergeWith(deltaOpml)
                            updateUserOpml(newOpml)
                        }
                    }
                }
            case None =>
                val newopml = Opml(uid, List(ol))
                insertUserOmpl(newopml)
        }
    }

    /**
     * apply a series of opml changes on user's opml
     * @param uid user id
     * @param opmlChanges the opml changes
     */
    def applyOpmlChanges(uid: Long, opmlChanges: List[OpmlChange]): Unit = {
        getUserOpml(uid) match {
            case Some(uo) =>
                val xmUrl2ChangeMap = opmlChanges.map{change=>
                    (change.xmlUrl,change)
                }.toMap

                //uo.opml => xml -> outline, folder
                val outlineAndFolderList = uo.outlines.flatMap{opmlFeed=>
                    if (opmlFeed.isFolder) {
                        opmlFeed.outlines.map{ feed=>
                            //(feed.xmlUrl, (feed, opmlFeed.title))
                            (feed, opmlFeed.title)
                        }
                    } else {
                        List(
                            //(opmlFeed.xmlUrl, (opmlFeed,""))
                            (opmlFeed,"")
                        )
                    }
                }

                //filter those that have marked as deleted
                val remainings = outlineAndFolderList.filter{case(outline,folder)=>
                    xmUrl2ChangeMap.get(outline.xmlUrl) match {
                        case Some(change) => !change.delete
                        case _ => true
                    }
                }

                //apply title and folder changes
                val result:List[(OpmlOutline,String)] = remainings.map{case(outline,folder)=>
                    xmUrl2ChangeMap.get(outline.xmlUrl) match {
                        case Some(change) => (outline.copy(title = change.title),change.folder)
                        case _ => (outline,folder)
                    }
                }

                val allFolders:List[OpmlOutline] = result.groupBy(_._2).map{case(folder,oaf) =>
                    val opmlOutlines = oaf.map(_._1)
                    val folderOpml = OpmlOutline.makeFolder(opmlOutlines,folder)
                    folderOpml
                }.toList

                val uo2 = new Opml(uid, allFolders)
                updateUserOpml(uo2)
            case _ =>
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
                //val newOpmlOutlines = uo.outlines.filter(!_.xmlUrl.startsWith(xmlUrl))
                //val curopml = Opml(uid, newOpmlOutlines)
                val curopml = uo.removeOutline(xmlUrl)
                updateUserOpml(curopml)
            case _ =>
        }
    }

    def markUserReadFeed(uid: Long, feedId: Long): Unit = {
        markUserReadFeedAt(uid, feedId, new Date())
    }

    def markUserReadFeedAt(uid: Long, feedId: Long, ts:Date): Unit = {
        val q = "update USER_STAT set START_FROM=? where user_id=? and feed_id = ? and story_id = 0"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                statement.setTimestamp(1,new Timestamp(ts.getTime))
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
                    logger.debug(q)
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

    def getUserInActiveFeedStats(uid: Long):List[FeedReadStat] = {
        val q =
            s"""
               SELECT
               |	UF.FEED_ID,FS.TITLE,FS.XML_URL, MIN(FDS.PUBLISHED) AS LAST_UPDATE
               |FROM
               |	USER_FEED UF
               |	JOIN
               |	FEED_SOURCE FS
               |	ON UF.FEED_ID = FS.FEED_ID
               |	JOIN
               |	FEED_STORY FDS
               |	ON UF.FEED_ID = FDS.FEED_ID
               |WHERE
               |	UF.USER_ID = ?
               |GROUP BY
               |	UF.FEED_ID, FS.TITLE,FS.XML_URL
               |ORDER BY
               |	LAST_UPDATE
               |LIMIT 20
         """.stripMargin

        val result = new util.ArrayList[FeedReadStat]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                statement.setLong(1, uid)

                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val ts = Option(rs.getTimestamp(4)) match {
                            case Some(ts) => new Date(ts.getTime)
                            case _ => new Date(0)
                        }
                        val readStats = FeedReadStat(
                            rs.getLong(1),
                            rs.getString(2),
                            rs.getString(3),
                            ts,
                            0,
                            0,
                            "",
                            0,
                            "InActive"
                        )
                        result.add(readStats)
                    }
                }
            }
        }
        result.asScala.toList
    }

    def getUserLastMonthActiveFeedStats(uid: Long):List[FeedReadStat] = {
        val q =
            s"""
               |SELECT
               |T.FEED_ID, T.TITLE, T.XML_URL, T.LAST_UPDATE, R.RCOUNT,T.TOTAL,
               |concat(round((  R.RCOUNT/T.TOTAL * 100 ),0),'%') AS RPERCENT,
               |ROUND(T.TOTAL/30) AS IPD
               |FROM
               |(
               |SELECT
               |	FS.FEED_ID, FS.TITLE, FS.XML_URL, FS.CHECKED AS LAST_UPDATE, COUNT(FDS.STORY_ID) AS TOTAL
               |FROM
               |	USER_FEED UF
               |	JOIN
               |	FEED_SOURCE FS
               |	ON UF.FEED_ID = FS.FEED_ID
               |	JOIN
               |	FEED_STORY FDS
               |	ON UF.FEED_ID = FDS.FEED_ID
               |WHERE
               |	UF.USER_ID = ? AND
               |	FDS.PUBLISHED > DATE_SUB(now(), INTERVAL 30 DAY)
               |GROUP BY
               |	FS.FEED_ID, FS.TITLE, FS.XML_URL, FS.CHECKED
               |) T
               |JOIN
               |(
               |SELECT
               |	US.FEED_ID, COUNT(US.STORY_ID) AS RCOUNT
               |FROM
               |	USER_FEED UF
               |	JOIN
               |	USER_STAT US
               |	ON UF.USER_ID = US.USER_ID AND
               |	   UF.FEED_ID = US.FEED_ID
               |WHERE
               |	UF.USER_ID = ? AND
               |  US.HASREAD = 1 AND
               |	US.START_FROM > DATE_SUB(now(), INTERVAL 30 DAY)
               |GROUP BY
               |	US.FEED_ID
               |) R
               |ON T.FEED_ID = R.FEED_ID
               |ORDER BY IPD DESC
               |LIMIT 20
         """.stripMargin

        val result = new util.ArrayList[FeedReadStat]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                statement.setLong(1, uid)
                statement.setLong(2, uid)

                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val readStats = FeedReadStat(
                            rs.getLong(1),
                            rs.getString(2),
                            rs.getString(3),
                            new Date(rs.getTimestamp(4).getTime),
                            rs.getInt(5),
                            rs.getInt(6),
                            rs.getString(7),
                            rs.getInt(8),
                            "Active"
                        )
                        result.add(readStats)
                    }
                }
            }
        }
        result.asScala.toList
    }

    def getUserLastMonthReadStats(uid: Long):List[FeedReadStat] = {
        val q =
            s"""
               |SELECT
               |T.FEED_ID, T.TITLE, T.XML_URL, T.LAST_UPDATE, R.RCOUNT,T.TOTAL,
               |concat(round((  R.RCOUNT/T.TOTAL * 100 ),0),'%') AS RPERCENT,
               |ROUND(T.TOTAL/30) AS IPD
               |FROM
               |(
               |SELECT
               |	FS.FEED_ID, FS.TITLE, FS.XML_URL, FS.CHECKED AS LAST_UPDATE, COUNT(FDS.STORY_ID) AS TOTAL
               |FROM
               |	USER_FEED UF
               |	JOIN
               |	FEED_SOURCE FS
               |	ON UF.FEED_ID = FS.FEED_ID
               |	JOIN
               |	FEED_STORY FDS
               |	ON UF.FEED_ID = FDS.FEED_ID
               |WHERE
               |	UF.USER_ID = ? AND
               |	FDS.PUBLISHED > DATE_SUB(now(), INTERVAL 30 DAY)
               |GROUP BY
               |	FS.FEED_ID, FS.TITLE, FS.XML_URL, FS.CHECKED
               |) T
               |JOIN
               |(
               |SELECT
               |	US.FEED_ID, COUNT(US.STORY_ID) AS RCOUNT
               |FROM
               |	USER_FEED UF
               |	JOIN
               |	USER_STAT US
               |	ON UF.USER_ID = US.USER_ID AND
               |	   UF.FEED_ID = US.FEED_ID
               |WHERE
               |	UF.USER_ID = ? AND
               |  US.HASREAD = 1 AND
               |	US.START_FROM > DATE_SUB(now(), INTERVAL 30 DAY)
               |GROUP BY
               |	US.FEED_ID
               |) R
               |ON T.FEED_ID = R.FEED_ID
               |ORDER BY R.RCOUNT DESC
               |LIMIT 20
         """.stripMargin

        val result = new util.ArrayList[FeedReadStat]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                statement.setLong(1, uid)
                statement.setLong(2, uid)

                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val readStats = FeedReadStat(
                            rs.getLong(1),
                            rs.getString(2),
                            rs.getString(3),
                            new Date(rs.getTimestamp(4).getTime),
                            rs.getInt(5),
                            rs.getInt(6),
                            rs.getString(7),
                            rs.getInt(8),
                            "Read"
                        )
                        result.add(readStats)
                    }
                }
            }
        }
        result.asScala.toList
    }

    def getUserLastMonthStarStats(uid: Long):List[FeedReadStat] = {
        val q =
            s"""
               |SELECT
               |T.FEED_ID, T.TITLE, T.XML_URL, T.LAST_UPDATE, R.RCOUNT,T.TOTAL,
               |concat(round((  R.RCOUNT/T.TOTAL * 100 ),0),'%') AS RPERCENT,
               |ROUND(T.TOTAL/30) AS IPD
               |FROM
               |(
               |SELECT
               |	FS.FEED_ID, FS.TITLE, FS.XML_URL, FS.CHECKED AS LAST_UPDATE, COUNT(FDS.STORY_ID) AS TOTAL
               |FROM
               |	USER_FEED UF
               |	JOIN
               |	FEED_SOURCE FS
               |	ON UF.FEED_ID = FS.FEED_ID
               |	JOIN
               |	FEED_STORY FDS
               |	ON UF.FEED_ID = FDS.FEED_ID
               |WHERE
               |	UF.USER_ID = ? AND
               |	FDS.PUBLISHED > DATE_SUB(now(), INTERVAL 30 DAY)
               |GROUP BY
               |	FS.FEED_ID, FS.TITLE, FS.XML_URL, FS.CHECKED
               |) T
               |JOIN
               |(
               |SELECT
               |	US.FEED_ID, COUNT(US.STORY_ID) AS RCOUNT
               |FROM
               |	USER_FEED UF
               |	JOIN
               |	USER_STAT US
               |	ON UF.USER_ID = US.USER_ID AND
               |	   UF.FEED_ID = US.FEED_ID
               |WHERE
               |	UF.USER_ID = ? AND
               |  US.HASLIKE = 1 AND
               |	US.START_FROM > DATE_SUB(now(), INTERVAL 30 DAY)
               |GROUP BY
               |	US.FEED_ID
               |) R
               |ON T.FEED_ID = R.FEED_ID
               |ORDER BY R.RCOUNT DESC
               |LIMIT 20
         """.stripMargin

        val result = new util.ArrayList[FeedReadStat]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                statement.setLong(1, uid)
                statement.setLong(2, uid)

                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val readStats = FeedReadStat(
                            rs.getLong(1),
                            rs.getString(2),
                            rs.getString(3),
                            new Date(rs.getTimestamp(4).getTime),
                            rs.getInt(5),
                            rs.getInt(6),
                            rs.getString(7),
                            rs.getInt(8),
                            "STAR"
                        )
                        result.add(readStats)
                    }
                }
            }
        }
        result.asScala.toList
    }

    def getUserLastMonthStatsHistograms(uid: Long):List[HistCounter] = {
        val q =
        s"""
           |SELECT
           |	'Read' AS ACT,
           |	START_FROM AS TS
           |FROM
           |	USER_FEED UF
           |	JOIN
           |	USER_STAT US
           |	ON
           |	UF.USER_ID = US.USER_ID AND
           |	UF.FEED_ID = US.FEED_ID
           |WHERE
           |	UF.USER_ID = ?
           |	AND US.START_FROM > DATE_SUB(now(), INTERVAL 30 DAY)
           |	AND US.HASREAD = 1
           |UNION ALL
           |SELECT
           |	'Like' AS ACT,
           |	START_FROM AS TS
           |FROM
           |	USER_FEED UF
           |	JOIN
           |	USER_STAT US
           |	ON
           |	UF.USER_ID = US.USER_ID AND
           |	UF.FEED_ID = US.FEED_ID
           |WHERE
           |	UF.USER_ID = ?
           |	AND US.START_FROM > DATE_SUB(now(), INTERVAL 30 DAY)
           |	AND US.HASLIKE = 1
           |UNION ALL
           |SELECT
           |	'Post' AS ACT,
           |	US.PUBLISHED AS TS
           |FROM
           |	USER_FEED UF
           |	JOIN
           |	FEED_STORY US
           |	ON
           |	UF.FEED_ID = US.FEED_ID
           |WHERE
           |	UF.USER_ID = ?
           |	AND US.PUBLISHED > DATE_SUB(now(), INTERVAL 30 DAY)
         """.stripMargin

        val actionDates = new util.ArrayList[(String,Date)]()
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                logger.debug(q)
                statement.setLong(1, uid)
                statement.setLong(2, uid)
                statement.setLong(3, uid)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val act = rs.getString(1)
                        val ts = new Date(rs.getTimestamp(2).getTime)
                        actionDates.add((act,ts))
                    }
                }
            }
        }
        aggregateToHistCounter(actionDates.asScala.toList)
    }

    def aggregateToHistCounter(actionDates :List[(String,Date)]): List[HistCounter] = {
        val monthlyHist = HistCounter("monthly",31)
        val weeklyHist = HistCounter("weekly",7)
        val dailyHist = HistCounter("daily",24)
        val cal = Calendar.getInstance()
        actionDates.foreach{case(act,ts)=>
            cal.setTime(ts)
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val wd = cal.get(Calendar.DAY_OF_WEEK)
            val h = cal.get(Calendar.HOUR_OF_DAY)

            monthlyHist.incCounter(act, d)
            weeklyHist.incCounter(act, wd)
            dailyHist.incCounter(act, h)
        }
        List(monthlyHist, weeklyHist, dailyHist)
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
                    logger.debug(q)
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
          |  UU.FEED_ID,
          |  COALESCE(COUNT(F.STORY_ID),0) AS STORY_COUNT, -- every feed will have story count even 0
          |  MAX(UU.START_FROM) AS START_FROM
          |FROM
          |  USER_STAT UU  -- for every feed in stats
          |  LEFT JOIN
          |  FEED_STORY F  -- find its story count
          |  ON
          |    F.FEED_ID = UU.FEED_ID AND
          |    F.PUBLISHED > UU.START_FROM  -- only count stories occur after lastRead
          |WHERE
          |  UU.STORY_ID = 0 AND
          |  UU.USER_ID = ?
          |GROUP BY
          |  F.FEED_ID
          |) TS
          |LEFT JOIN
          |(
          |SELECT -- get user read story count
          |   U.FEED_ID,
          |   COALESCE(COUNT(F.STORY_ID),0) AS READ_COUNT
          |   -- U and UU will always join
          |FROM
          |  USER_STAT U -- the feed
          |  LEFT JOIN
          |  USER_STAT UU -- the story
          |  ON
          |  	U.FEED_ID = UU.FEED_ID AND
          |  	UU.HASREAD = 1
          |  LEFT JOIN -- find the story date, it has to be published after the last read
          |  FEED_STORY F
          |  ON
          |  	UU.FEED_ID = F.FEED_ID AND
          |  	UU.STORY_ID = F.STORY_ID  AND
          |  	F.PUBLISHED > U.START_FROM
          |WHERE
          |  U.STORY_ID = 0 AND
          |  UU.STORY_ID <> 0 AND
          |  U.USER_ID = ?
          |GROUP BY
          |U.FEED_ID
          |) US
          |ON
          |  TS.FEED_ID = US.FEED_ID
          |)
          |WHERE
          |TS.FEED_ID IN (SELECT FEED_ID FROM USER_FEED WHERE USER_ID = ?)
        """.stripMargin

        val result = new util.ArrayList[UserFeedReadStat]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uid)
                statement.setLong(2, uid)
                statement.setLong(3, uid)
                logger.debug(q)
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
