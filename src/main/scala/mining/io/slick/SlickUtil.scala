package mining.io.slick

import scala.slick.driver.JdbcProfile
import java.util.Date
import java.sql.Timestamp
import scala.slick.driver.H2Driver.simple.MappedColumnType
import slick.jdbc.meta.MTable
import scala.concurrent.duration._
import scala.concurrent.Await

object SlickUtil {
  def tablesMap(implicit conn: SlickDBConnection): Map[String, MTable] = {
    
    val tableList = Await.result(conn.database.run(MTable.getTables), 1.seconds).toList
      tableList.map(t => (t.name.name, t)).toMap
  }
}
