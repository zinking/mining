package mining.io.slick

import java.util.Date
import java.sql.Timestamp
import slick.jdbc.meta.MTable
import scala.concurrent.duration._
import scala.concurrent.Await
import slick.driver.H2Driver.api._

object SlickUtil {
  def tablesMap(db: Database): Map[String, MTable] = {
    
    val tableList = Await.result(db.run(MTable.getTables), 1.seconds).toList
      tableList.map(t => (t.name.name, t)).toMap
  }
}
