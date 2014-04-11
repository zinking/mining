package mining.io.slick

import scala.slick.driver.JdbcProfile
import java.sql.Date
import scala.xml.Elem
import scala.collection.mutable
import mining.io._
import mining.util.UrlUtil
import java.sql.Blob

class SlickUserDAO(override val profile: JdbcProfile) extends SlickDBConnection(profile) {
  import profile.simple._
  

  val opmls = TableQuery[UserOpml]
  
  def manageDDL() = {
    val tablesMap = SlickUtil.tablesMap(this)
    if (!tablesMap.contains("USER_OPML")) database.withSession(implicit session => opmls.ddl.create)
  }
  

  
  def getOpmlById( id:String ):Option[Opml] = database withSession { implicit session =>
  	val opml1 = opmls.filter( _.userId === id ).firstOption
  	opml1.map( _.toOpml )
  }
  
  def saveOpml( uo:Opml) = database withSession { implicit session =>
   val r1 =  opmls.filter( _.userId === uo.id ).firstOption
   r1 match{
     case Some(uoo) => opmls.update( uo.toStorage )
     case None => opmls.insert(uo.toStorage)
   }
  }



  class UserOpml(tag: Tag) extends Table[OpmlStorage](tag, "USER_OPML") {
    def userId = column[String]("USER_ID", O.PrimaryKey )
    def raw    = column[Blob]("RAW")
  
    def * = (userId, raw) <> (OpmlStorage.tupled, OpmlStorage.unapply) 
  }    


  
}

object SlickUserDAO {
  def apply(profile: JdbcProfile) = new SlickUserDAO(profile)
}