package mining.io.dao

import java.sql.{SQLException, Connection}

import com.typesafe.config.{ConfigFactory, Config}
import com.zaxxer.hikari.HikariDataSource
import mining.util.EnvUtil
import org.slf4j.{LoggerFactory, Logger}
import scala.language.reflectiveCalls
import scala.util.Properties

/**
 * Jdbc related factory method
 * Created by awang on 4/12/15.
 */
class JdbcConnectionFactory
object JdbcConnectionFactory{
    private val logger: Logger = LoggerFactory.getLogger(classOf[JdbcConnectionFactory])
    val conf:Config = ConfigFactory.load
    val venv = conf.getString("env")
    val env = Option(System.getProperty("env")).getOrElse(venv)
    val config = conf.getConfig(env).getConfig("db")
    val url = config.getString("properties.url")
    val driver = config.getString("properties.driver")
    val user = config.getString("properties.user")
    val pass = config.getString("properties.pass")
    val timeit = config.getBoolean("properties.timeit")


    logger.info("Starting up connection pool, using {} config",env)


    val hpool = new HikariDataSource()
    hpool.setJdbcUrl(url)
    hpool.setUsername(user)
    hpool.setPassword(pass)
    hpool.setDriverClassName(driver)
    hpool.setMaximumPoolSize(5)

    def getPooledConnection:Connection={
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
    def logger: Logger
    def timeit:Boolean =
        JdbcConnectionFactory.timeit

    def using[T <: { def close() }](resource: T)(block: T => Unit) {
        try {
            if (timeit) {
                EnvUtil.time{block(resource)}
            } else {
                block(resource)
            }
        } catch{
            case e:SQLException =>
                logger.error(e.getMessage,e)
        } finally {
            resource.close()
        }
    }
}
