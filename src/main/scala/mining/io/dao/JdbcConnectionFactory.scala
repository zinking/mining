package mining.io.dao

import java.sql.{SQLException, Connection}

import com.typesafe.config.Config
import org.apache.commons.dbcp2.BasicDataSource
import org.slf4j.{LoggerFactory, Logger}
import scala.language.reflectiveCalls

/**
 * Created by awang on 4/12/15.
 */
object JdbcConnectionFactory{
    def apply(config:Config) = {
        new JdbcConnectionFactory(config)
    }
}
class JdbcConnectionFactory(config: Config){
    private val logger: Logger = LoggerFactory.getLogger(classOf[JdbcConnectionFactory])
    val url = config.getString("properties.url")
    val driver = config.getString("properties.driver")
    val user = config.getString("properties.user")
    val pass = config.getString("properties.pass")
    val pool = new BasicDataSource()
    pool.setDriverClassName(driver)
    pool.setUrl(url)
    pool.setUsername(user)
    pool.setPassword(pass)
    pool.setInitialSize(3)

    def getPooledConnection():Connection={
        try{
            pool.getConnection
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
