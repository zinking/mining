package mining.io.slick

import java.sql.Blob

import scala.slick.driver.JdbcProfile

import mining.io._

class SlickUserDAO(override val profile: JdbcProfile) extends SlickUserFeedDDL(profile) {
  import profile.simple._

  def saveUser(user: User) = database withTransaction { implicit session => 
    userInfo.filter(_.userId === user.userId).firstOption match {
      case Some(user) => userInfo.update(user)
      case None       => userInfo.insert(user)
    }
  } 

  def getUserById(userId: Long): Option[User] = database withSession { implicit session =>
    userInfo.filter(_.userId === userId).firstOption 
  }
  
  def setUserStarStory(userId:Long, storyId: Long, starred: Boolean): Unit = database withTransaction { implicit session =>
    //implication is that User cannot star a story before reading it
    val userStory = userReadStories.filter(s => (s.userId === userId && s.storyId === storyId)).firstOption
     userStory match{
      case Some(us) =>userReadStories.update(ReadStory(us.userId, us.storyId, "", starred, us.read))  
      case _ => 
     }
  }
  
  def setUserStarStoryWithLink(userId:Long, link: String, starred: Boolean): Unit = database withTransaction { implicit session =>
    //implication is that User cannot star a story before reading it
    val userStory = userReadStories.filter(s => (s.userId === userId && s.storyLink === link)).firstOption
    userStory match{
      case Some(us) => userReadStories.update(ReadStory(us.userId, 0, link , starred, us.read)) 
      case _ => 
    }
       
  }
  
  def getUserStarStories(userId: Long  , pagesz:Int = 10, pageno:Int = 0): List[Story] = database withSession { implicit session =>
    val query = for {
      user <- userInfo
      userStory <- userReadStories if (user.userId === userStory.userId && userStory.star === true)
      story <- stories if userStory.storyId === story.id
    } yield (story) 
    query.list.drop( pageno* pagesz ).take(pagesz)
  }
  
  def saveUserReadStory(userId: Long, storyId: Long, read: String):Unit = database withSession { implicit session =>
    val uo = userReadStories.filter(s => (s.userId === userId && s.storyId === storyId)).firstOption
    uo match{
     case Some(uoo) => userReadStories.update(ReadStory(uoo.userId, uoo.storyId, "", uoo.star, read))   
     case None      => userReadStories.update(ReadStory(userId, storyId, "",  false, read))  
    }
  }
  
  def saveUserReadStoryWithLink(userId: Long, link: String, read: String):Unit = database withSession { implicit session =>
    val uo = userReadStories.filter(s => (s.userId === userId && s.storyLink === link)).firstOption
    uo match{
     case Some(uoo) => userReadStories.update(ReadStory(uoo.userId, uoo.storyId, uoo.storyLink, uoo.star, read))   
     case None      => userReadStories.update(ReadStory(userId, 0, link, false, read))  
    }
  }

  def saveOpml(uo: Opml) = saveOpmlStorage( uo.toStorage )
      
  def saveOpmlStorage(opmlStorage: OpmlStorage) = database withTransaction { implicit session =>
    val userStorage = opmls.filter(_.userId === opmlStorage.id).firstOption
    userStorage match {
      case Some(uoo) => {
        val q = for { o <- opmls if o.userId === opmlStorage.id } yield o.raw
        q.update(opmlStorage.raw )
      }
      case None      => opmls.insert(opmlStorage)
    }
  }
  
  def getOpmlById(userId: Long): Option[Opml] = database withSession { implicit session =>
    val opml1 = opmls.filter(_.userId === userId).firstOption
    opml1.map(_.toOpml)
  }
  
  //Opml structure should be updated in client side and save the whole Opml here
  def addOmplOutline(uid: Long, ol:OpmlOutline) = database withSession { implicit session =>
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
}

object SlickUserDAO {
  def apply(profile: JdbcProfile) = new SlickUserDAO(profile)
}