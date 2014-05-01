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
    if (!tablesMap.contains("USER_SETTING")) database.withSession(implicit session => userSettings.ddl.create)
    if (!tablesMap.contains("USER_READSTORY")) database.withSession(implicit session => userReadStories.ddl.create)
  }
  

  
  def getOpmlById( id:String ):Option[Opml] = database withSession { implicit session =>
    val opml1 = opmls.filter( _.userId === id ).firstOption
    opml1.map( _.toOpml )
  }
  
  def getUserStarStories( id:String , pagesz:Int = 10, pageno:Int = 0):List[String] = database withSession { implicit session =>
    val storyUrls = userReadStories
        .filter( s => (s.userId === id && s.star === "STAR") )
        .map( _.storyId ).drop( pageno* pagesz ).take(pagesz)
    storyUrls.buildColl
  }
  
  def setUserStarStory( uid:String, sid:String, star:String ):Unit = database withSession { implicit session =>
    //implication is that User cannot star a story before reading it
    val uo = userReadStories.filter( s => (s.userId === uid && s.storyId === sid ) ).first
    userReadStories.update( ReadStory(uo.userId, uo.storyId, uo.read, star))    
  }
  
  def saveUserReadStory( uid:String, sid:String, read:String):Unit = database withSession { implicit session =>
    val uo = userReadStories.filter( s => (s.userId === uid && s.storyId === sid ) ).firstOption
    uo match{
     case Some(uoo) => userReadStories.update( ReadStory(uoo.userId, uoo.storyId, read, uoo.star))   
     case None => userReadStories.update( ReadStory(uid, sid, read, ""))  
   }
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
       val curopml = Opml( uid, uoo.toOpml.outline :+ ol )//TODO: whatif this feed is already subscribed
       opmls.update(curopml.toStorage)
     }
     case None =>{
       val newopml = Opml( uid, List( ol ) )
       opmls.insert(newopml.toStorage)
     }
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
  
  class UserReadStory(tag: Tag) extends Table[ReadStory](tag, "USER_READSTORY") {
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