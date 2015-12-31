package mining.io.dao

import java.sql.{SQLException, Connection}

import com.typesafe.config.{ConfigFactory, Config}
import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.dbcp2.BasicDataSource
import org.slf4j.{LoggerFactory, Logger}
import scala.language.reflectiveCalls

/**
 * Created by awang on 4/12/15.
 */
class JdbcConnectionFactory
object JdbcConnectionFactory{
    private val logger: Logger = LoggerFactory.getLogger(classOf[JdbcConnectionFactory])
    val conf:Config = ConfigFactory.load
    val db = conf.getString("db")
    val config = conf.getConfig(db)
    val url = config.getString("properties.url")
    val driver = config.getString("properties.driver")
    val user = config.getString("properties.user")
    val pass = config.getString("properties.pass")
//    val pool = new BasicDataSource()
//    pool.setDriverClassName(driver)
//    pool.setUrl(url)
//    pool.setUsername(user)
//    pool.setPassword(pass)
//    pool.setInitialSize(3)

    val hpool = new HikariDataSource()
    hpool.setJdbcUrl(url)
    hpool.setUsername(user)
    hpool.setPassword(pass)
    hpool.setDriverClassName(driver)
    hpool.setMaximumPoolSize(5)

    def getPooledConnection():Connection={
        try{
            //pool.getConnection
            hpool.getConnection
        } catch{
            case e:SQLException=>
                logger.error(e.getMessage,e)
                throw e
        }
    }
}


trait Dao{
    def log: Logger
    def using[T <: { def close() }](resource: T)(block: T => Unit) {
        try {
            block(resource)
        } catch{
            case e:SQLException =>
                log.error(e.getMessage,e)
        } finally {
            resource.close()
        }
    }
}
