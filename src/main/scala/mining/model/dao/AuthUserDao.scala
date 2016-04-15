package mining.model.dao

import java.security.MessageDigest
import java.sql._
import java.util
import java.util.Date

import com.typesafe.config.{Config, ConfigFactory}
import mining.io.dao.{JdbcConnectionFactory, Dao}
import mining.model.AuthUser
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Await, Future}
import ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
 * Created by awang on 5/12/15.
 */
object AuthUserDao {
    def apply()= {
        new AuthUserDao()
    }

    def resultToAuthUser(rs: ResultSet): AuthUser = {
        AuthUser(
            rs.getLong("USER_ID"),
            rs.getString("EMAIL"),
            rs.getString("NAME"),
            rs.getString("PASS"),
            rs.getString("APIKEY"),
            rs.getString("LASTLOGIN_FROM"),
            new Date(rs.getTimestamp("LASTLOGIN_TIME").getTime)
        )
    }

}

class AuthUserDao() extends Dao {
    import AuthUserDao._
    override def logger: Logger = LoggerFactory.getLogger(classOf[AuthUserDao])

    def md5(s: String):String = {
        val digest = MessageDigest.getInstance("MD5")
        digest.digest(s.getBytes).map("%02x".format(_)).mkString
    }

    def updateAuthUser(user: AuthUser) : Future[Unit]=Future{
        val q = "update AUTH_USER set name=?,pass=?,apikey=?,lastlogin_from=?,lastlogin_time=? where user_id = ?"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, user.name)
                statement.setString(2, user.pass)
                statement.setString(3, user.apiKey)
                statement.setString(4, user.lastLoginFrom)
                statement.setTimestamp(5, new Timestamp(user.lastLoginTime.getTime))
                statement.setLong(6, user.userId)
                statement.executeUpdate()
            }
        }
    }

    def addNewUser(newUser: AuthUser): AuthUser ={
        val newKey = md5( newUser.email + newUser.pass + System.currentTimeMillis.toString )
        val hashedPass = md5( newUser.pass )
        val newUser2 = newUser.copy(apiKey=newKey,pass=hashedPass)
        var newUserId = 0L
        val q = "INSERT INTO AUTH_USER(EMAIL,NAME,PASS,APIKEY,LASTLOGIN_FROM,LASTLOGIN_TIME) VALUES (?,?,?,?,?,?)"
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) { statement =>
                statement.setString(1, newUser.email)
                statement.setString(2, newUser.name)
                statement.setString(3, hashedPass)
                statement.setString(4, newKey)
                statement.setString(5, newUser.lastLoginFrom)
                statement.setTimestamp(6, new Timestamp(newUser.lastLoginTime.getTime))
                statement.executeUpdate()
                val newUserIdRS = statement.getGeneratedKeys
                if (newUserIdRS.next) {
                    newUserId = newUserIdRS.getLong(1)
                } else {
                    throw new SQLException("Auth User Insertion failed")
                }
            }
        }
        newUser2.copy(userId=newUserId)
    }

    def getUserById(userId: Long): Future[Option[AuthUser]]=Future{
        val q = "select * from AUTH_USER where user_id = ? "
        val result = new util.ArrayList[AuthUser]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setLong(1, userId)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val feed = resultToAuthUser(rs)
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

    def getUserByApikey(key:String):Option[AuthUser] ={
        val q = "select * from AUTH_USER where apikey = ? "
        val result = new util.ArrayList[AuthUser]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, key)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val feed = resultToAuthUser(rs)
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

    def getUserByEmail(email: String): Future[Option[AuthUser]]=Future{
        val q = "select * from AUTH_USER where email = ? "
        val result = new util.ArrayList[AuthUser]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, email)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val feed = resultToAuthUser(rs)
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

    def authUser( email:String, rawPass:String): Future[Option[AuthUser]]=Future{
        val hashedPass = md5(rawPass)
        val q = "select * from AUTH_USER where email=? and pass=? "
        val result = new util.ArrayList[AuthUser]
        using(JdbcConnectionFactory.getPooledConnection) { connection =>
            using(connection.prepareStatement(q)) { statement =>
                statement.setString(1, email)
                statement.setString(2, hashedPass)
                using(statement.executeQuery()) { rs =>
                    while (rs.next) {
                        val feed = resultToAuthUser(rs)
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
}
