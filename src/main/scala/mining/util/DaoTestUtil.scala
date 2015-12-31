package mining.util

import java.sql.SQLException

import com.typesafe.config.{Config, ConfigFactory}
import mining.io.dao.JdbcConnectionFactory

/**
 * Created by awang on 5/12/15.
 */
object DaoTestUtil {
    def truncateAllTables(): Unit ={
        val conf:Config = ConfigFactory.load
        val connection = JdbcConnectionFactory.getPooledConnection

        val tables = List("FEED_SOURCE","FEED_STORY","USER_INFO","USER_OPML","USER_STORY","AUTH_USER")
        try{
            val statement = connection.createStatement()
            connection.setAutoCommit(false)
            for(table<-tables){
                val q=s"truncate $table"
                statement.executeUpdate(q)
            }
            connection.commit()
            connection.setAutoCommit(true)
            Thread.sleep(1000)
            statement.close()
            connection.close()
        } catch{
            case e:SQLException =>
                e.printStackTrace()
        }
    }
}
