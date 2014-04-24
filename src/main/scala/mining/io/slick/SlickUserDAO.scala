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
  val userSettings = TableQuery[UserSetting]
  val userReadStories = TableQuery[UserReadStory]
  
  
  def manageDDL() = {
    val tablesMap = SlickUtil.tablesMap(this)
    if (!tablesMap.contains("USER_OPML")) database.withSession(implicit session => opmls.ddl.create)
  }
  

  
  def getOpmlById( id:String ):Option[Opml] = database withSession { implicit session =>
    val opml1 = opmls.filter( _.userId === id ).firstOption
    opml1.map( _.toOpml )
  }
  
  def getUserStarStories( id:String):List[String] = database withSession { implicit session =>
    val storyUrls = userReadStories
        .filter( s => (s.userId === id && s.star === "STAR") )
        .map( _.storyId )
    storyUrls.buildColl
  }
  
  def setUserStarStory( uid:String, sid:String, star:String ):Unit = database withSession { implicit session =>
    val uo = userReadStories
        .filter( s => (s.userId === uid && s.storyId === sid ) ).first
    userReadStories.update( ReadStory(uo.userId, uo.storyId, uo.read, star))    
  }
  
  def saveOpml( uo:Opml) = database withSession { implicit session =>
    saveOpmlStorage(uo.toStorage )
  }
  
  def saveOpmlStorage( uo:OpmlStorage) = database withSession { implicit session =>
   val r1 =  opmls.filter( _.userId === uo.id ).firstOption
   r1 match{
     case Some(uoo) => opmls.update( uo )
     case None => opmls.insert(uo)
   }
  }
  
  def addOmplOutline( uid:String, ol:OpmlOutline ) = database withSession { implicit session =>
   val r1 =  opmls.filter( _.userId === uid ).firstOption
   r1 match{
     case Some(uoo) => {
       val newopml = Opml( uid, uoo.toOpml.outline :+ ol )
       opmls.update(newopml.toStorage)
     }
     case None => ???
   }
  }
  
  def saveUserSetting( s:Setting) = database withSession { implicit session =>
   val r1 =  opmls.filter( _.userId === s.userId ).firstOption
   r1 match{
     case Some(uoo) => userSettings.update( s )
     case None => userSettings.insert(s)
   }
  }


  class UserOpml(tag: Tag) extends Table[OpmlStorage](tag, "USER_OPML") {
    def userId = column[String]("USER_ID", O.PrimaryKey )
    def raw    = column[Blob]("RAW")
  
    def * = (userId, raw) <> (OpmlStorage.tupled, OpmlStorage.unapply) 
  }
  
  class UserSetting(tag: Tag) extends Table[Setting](tag, "USER_SETTING") {
    def userId    = column[String]("USER_ID", O.PrimaryKey )
    def hideEmpty = column[String]("HIDE_EMTPY")
    def sort = column[String]("SORT")
    def display = column[String]("DISPLAY")

    def * = (userId, hideEmpty, sort, display ) <> (Setting.tupled, Setting.unapply) 
  }
  
  class UserReadStory(tag: Tag) extends Table[ReadStory](tag, "USER_SETTING") {
    def userId    = column[String]("USER_ID", O.PrimaryKey )
    def storyId   = column[String]("STORY_ID")
    def star      = column[String]("STAR")
    def read      = column[String]("READ")

    def * = (userId, storyId, star, read ) <> (ReadStory.tupled, ReadStory.unapply) 
  }

}

object SlickUserDAO {
  def apply(profile: JdbcProfile) = new SlickUserDAO(profile)
}