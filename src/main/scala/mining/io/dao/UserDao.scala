package mining.io.dao

import java.io.{PrintWriter, File}
import java.sql.{SQLException, Timestamp, ResultSet, Connection}
import java.util

import com.typesafe.config.{ConfigFactory, Config}

import org.slf4j.{LoggerFactory, Logger}

import scala.io.Source
import scala.xml.XML
import scala.collection.JavaConverters._

import mining.io._

/**
 * Created by awang on 5/12/15.
 */
object UserDao {
    def apply() = {
        new UserDao()
    }

    def resultToUser(rs: ResultSet): User = {
        User(
            rs.getLong("USER_ID"),
            rs.getString("EMAIL"),
            rs.getString("PREF")
        )
    }

    def resultToUserStory(rs: ResultSet): UserStory = {
        UserStory(
            rs.getLong("USER_ID"),
            rs.getLong("STORY_ID"),
            rs.getInt("HASREAD"),
            rs.getInt("HASLIKE"),
            rs.getString("COMMENT")
        )
    }

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
    }

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



    def getUser(uid: Long): Option[User] = {
        val q = "select * from USER_INFO where user_id = ? "
        val result = new util.ArrayList[User]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uid)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val feed = resultToUser(rs)
                        result.add(feed)
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

    def getUserStory(uid: Long, sid: Long): Option[UserStory] = {
        val q = "select * from USER_STORY where user_id = ? and story_id = ? "
        val result = new util.ArrayList[UserStory]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, uid)
                statement.setLong(2, sid)
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

    def insertUserStory(us:UserStory)={
        val q = "INSERT INTO USER_STORY (user_id,story_id,hasread,haslike,comment) VALUES (?,?,?,?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, us.userId)
                statement.setLong(2, us.storyId)
                statement.setInt(3, us.hasRead)
                statement.setInt(4, us.hasLike)
                statement.setString(5, us.comment)
                statement.executeUpdate()
            }
        }
    }

    def updateUserStoryRead(uid:Long,sid:Long,hasRead:Int)={
        val q = "update USER_STORY set hasread=? where user_id=? and story_id=?"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setInt(1, hasRead)
                statement.setLong(2, uid)
                statement.setLong(3, sid)
                statement.executeUpdate()
            }
        }
    }

    def updateUserStoryLike(uid:Long,sid:Long,haslike:Int)={
        val q = "update USER_STORY set haslike=? where user_id=? and story_id=?"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setInt(1, haslike)
                statement.setLong(2, uid)
                statement.setLong(3, sid)
                statement.executeUpdate()
            }
        }
    }


    def setUserStarStory(userId: Long, storyId: Long, starred: Int): Unit = {
        //implication is that User cannot star a story before reading it
        getUserStory(userId,storyId) match {
            case Some(us) =>
                updateUserStoryLike(userId,storyId,starred)
            case None =>
                //or you cannot star it before you read it
                //insertUserStory(UserStory(userId,storyId,1,starred,""))
        }
    }

    def setUserReadStory(userId: Long, storyId: Long, read: Int): Unit = {
        getUserStory(userId,storyId) match {
            case Some(us) =>
                updateUserStoryRead(userId,storyId,read)
            case None =>
                insertUserStory(UserStory(userId,storyId,read,0,""))
        }
    }

    def getUserStarStories(userId: Long, pageSize: Int = 10, pageNo: Int = 0): Iterable[Story] = {
        val q = s"""select fs.* from FEED_STORY fs join USER_STORY us on fs.story_id=us.story_id
               where us.user_id=$userId and us.haslike>0 order by fs.updated desc limit ${(pageNo)*pageSize},$pageSize""".stripMargin
        val result = new util.ArrayList[Story]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                using(statement.executeQuery(q)) { rs =>
                    while (rs.next) {
                        val story = FeedDao.resultToStory(rs)
                        result.add(story)
                    }
                }
            }
        }
        result.asScala.toList
    }

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

    def insertUserOmpl(uo:Opml)={
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

    def updateUserOpml(uo:Opml)={
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

    def setUserOpml(uo: Opml): Unit = {
        getUserOpml(uo.id) match {
            case Some(uoo) => updateUserOpml(uo)
            case None => insertUserOmpl(uo)
        }
    }

    //Opml structure should be updated in client side and save the whole Opml here
    def addOmplOutline(uid: Long, ol: OpmlOutline):Unit = {
        getUserOpml(uid) match {
            case Some(uo) =>
                if (!ol.xmlUrl.isEmpty){
                    val curOpmlOutlines = uo.outline.filter(_.xmlUrl==ol.xmlUrl)
                    if (curOpmlOutlines.isEmpty){
                        val curopml = Opml(uid, uo.outline :+ ol)
                        updateUserOpml(curopml)
                    }
                }
            case None =>
                val newopml = Opml(uid, List(ol))
                insertUserOmpl(newopml)
        }
    }

    def removeOmplOutline(uid: Long, xmlUrl:String):Unit = {
        getUserOpml(uid) match {
            case Some(uo) =>
                val newOpmlOutlines = uo.outline.filter(_.xmlUrl!=xmlUrl)
                val curopml = Opml(uid, newOpmlOutlines)
                updateUserOpml(curopml)
            case _ =>
        }
    }
}
