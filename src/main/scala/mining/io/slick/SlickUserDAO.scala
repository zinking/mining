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

  def getUserById(userId: String): Option[User] = database withSession { implicit session =>
    userInfo.filter(_.userId === userId).firstOption 
  }
  
  def setUserStarStory(userId:String, storyId: Long, starred: Boolean): Unit = database withTransaction { implicit session =>
    //implication is that User cannot star a story before reading it
    val userStory = userReadStories.filter(s => (s.userId === userId && s.storyId === storyId)).first
    userReadStories.update(ReadStory(userStory.userId, userStory.storyId, starred, userStory.read))    
  }
  
  def getUserStarStories(userId: String): List[Story] = database withSession { implicit session =>
    val query = for {
      user <- userInfo
      userStory <- userReadStories if (user.userId === userStory.userId && userStory.star === true)
      story <- stories if userStory.storyId === story.id
    } yield (story) 
    query.list
  }
  
  def saveUserReadStory(userId: String, storyId: Long, read: String):Unit = database withSession { implicit session =>
    val uo = userReadStories.filter(s => (s.userId === userId && s.storyId === storyId)).firstOption
    uo match{
     case Some(uoo) => userReadStories.update(ReadStory(uoo.userId, uoo.storyId, uoo.star, read))   
     case None      => userReadStories.update(ReadStory(userId, storyId, false, read))  
    }
  }

  def saveOpml(uo: Opml) = database withTransaction { implicit session =>
    val opmlStorage = uo.toStorage() 
    val userStorage = opmls.filter(_.userId === opmlStorage.id).firstOption
    userStorage match {
      case Some(uoo) => opmls.update(opmlStorage)
      case None      => opmls.insert(opmlStorage)
    }
  }
  
  def getOpmlById(userId: String): Option[Opml] = database withSession { implicit session =>
    val opml1 = opmls.filter(_.userId === userId).firstOption
    opml1.map(_.toOpml)
  }
  
  //Opml structure should be updated in client side and save the whole Opml here
  def addOmplOutline(uid: String, ol:OpmlOutline) = database withSession { implicit session =>
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