package mining.io.slick

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.meta._

object SlickUtil {
  def tablesMap(conn: SlickDBConnection): Map[String, MTable] = {
    import conn.profile.simple._
    
    conn.database withSession { implicit session =>
      val tableList = MTable.getTables.list()
      tableList.map(t => (t.name.name, t)).toMap
    }
  }
}