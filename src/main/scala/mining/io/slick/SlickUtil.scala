package mining.io.slick

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.meta._
import java.util.Date
import java.sql.Timestamp
import scala.slick.driver.H2Driver.simple.MappedColumnType

object SlickUtil {
  def tablesMap(conn: SlickDBConnection): Map[String, MTable] = {
    import conn.profile.simple._
    
    conn.database withSession { implicit session =>
      val tableList = MTable.getTables.list()
      tableList.map(t => (t.name.name, t)).toMap
    }
  }
}
