package mining.io.slick

import java.sql.SQLException

import com.typesafe.config.{ConfigFactory, Config}
import mining.io.dao.JdbcConnectionFactory

/**
 * Created by awang on 5/12/15.
 */
object DaoTestUtil {
    def truncateAllTables(db:String): Unit ={
        val conf:Config = ConfigFactory.load
        val connection = JdbcConnectionFactory(conf.getConfig(db)).getPooledConnection()

        val tables = List("FEED_SOURCE","FEED_STORY","USER_INFO","USER_OPML","USER_STORY")
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
