package mining.io.slick

import scala.slick.driver.JdbcProfile
import mining.util.EnvUtil

class SlickDBConnection(val profile: JdbcProfile) {
  import profile.simple._
  
  val configs = EnvUtil.configs  
  
  val database = Database.forURL(url, driver = driver)
  
  protected def url = configs.getString("db.url") 

  protected def username = configs.getString("db.username")
  
  protected def password = configs.getString("db.password")

  protected def driver = configs.getString("db.driver")
  
}

object SlickDBConnection {
  def apply(profile: JdbcProfile) = new SlickDBConnection(profile)
}

