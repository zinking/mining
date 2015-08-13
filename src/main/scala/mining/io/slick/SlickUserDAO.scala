package mining.io.slick

import java.sql.Blob
import _root_.slick.backend.DatabasePublisher
import slick.driver.H2Driver.api._
import slick.dbio._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import mining.io._
import org.h2.tools.Server
import mining.util.DirectoryUtil
import java.io.File
import java.io.PrintWriter
import scala.io.Source
import scala.xml.XML


object SlickUserDAO {
  def apply(db:Database) = new SlickUserDAO(db)
}

class SlickUserDAO(db: Database) extends SlickUserFeedDDL(db) {
  val opmlFileFolderPath = DirectoryUtil.pathFromProject("target","useropml")

  def saveUser(user: User) : Unit = {
    Await.result(
        db.run( userInfo += user ),
        Duration.Inf
    )
  } 
  
  def updateUser(user: User) : Unit = {
    Await.result(
        db.run(
            userInfo.filter(_.userId === user.userId).update(user)
        ), 
        Duration.Inf
    )
  }

  def getUserById(userId: Long): Option[User] = {
    Await.result(
        db.run(userInfo.filter(_.userId === userId).result.headOption), 
        Duration.Inf
    )
  }
  
  def setUserStarStory(userId:Long, storyId: Long, starred: Boolean): Unit = {
    //implication is that User cannot star a story before reading it
    Await.result(
        db.run(
            userReadStories.filter(s => (s.userId === userId && s.storyId === storyId)).result.headOption map { r=>
              r match{
                case Some(us) =>userReadStories.update(ReadStory(us.userId, us.storyId, "", starred, us.read))  
                case _        => 
              } 
            }
        ), 
        Duration.Inf
    )
  }
  
  def setUserStarStoryWithLink(userId:Long, link: String, starred: Boolean): Unit = {
    //implication is that User cannot star a story before reading it
    Await.result(
        db.run(
            userReadStories.filter(s => (s.userId === userId && s.storyLink === link)).result.headOption map { r=>
              r match{
                case Some(us) =>userReadStories.update(ReadStory(us.userId, 0, link , starred, us.read))   
                case _        => 
              } 
            }
        ), 
        Duration.Inf
    )       
  }
  
  def getUserStarStories(userId: Long  , pagesz:Int = 10, pageno:Int = 0): Seq[Story] = {
    val query = for {
      user <- userInfo
      userStory <- userReadStories if (user.userId === userStory.userId && userStory.star === true)
      story <- stories if userStory.storyId === story.id
    } yield (story) 
    
    Await.result(
        db.run(query.result).map{ list =>
           list.drop( pageno* pagesz ).take(pagesz)
        }
    , Duration.Inf)
  }
  
  def saveUserReadStory(userId: Long, storyId: Long, read: String):Unit = {
    Await.result(
        db.run(
            userReadStories.filter(s => (s.userId === userId && s.storyId === storyId)).result.headOption map { uo=>
              uo match{
               case Some(uoo) => userReadStories.update(ReadStory(uoo.userId, uoo.storyId, "", uoo.star, read))   
               case None      => userReadStories.update(ReadStory(userId, storyId, "",  false, read))  
              } 
            }
        ).map(_ => ()), 
        Duration.Inf
    )    
  }
  
  def saveUserReadStoryWithLink(userId: Long, link: String, read: String):Unit = {
    Await.result(
        db.run(
            userReadStories.filter(s => (s.userId === userId && s.storyLink === link)).result.headOption map { uo=>
              uo match{
                case Some(uoo) => userReadStories.update(ReadStory(uoo.userId, uoo.storyId, uoo.storyLink, uoo.star, read))   
                case None      => userReadStories.update(ReadStory(userId, 0, link, false, read))  
              } 
            }
        ).map(_ => ()), 
        Duration.Inf
    )
  }

  def saveOpml(uo: Opml):Unit = {
    //saveOpmlStorage( uo.toStorage )
     val rawXml = uo.toXml.toString()
     saveOpml(uo.id,rawXml)
  }
  
  def saveOpml(uid:Long,xmlContent:String):Unit = {
     val userOpmlFilePath = s"${opmlFileFolderPath}/${uid}.xml"
     val userOpmlFile = new File(userOpmlFilePath)
     val pw = new PrintWriter(userOpmlFile)
     pw.write(xmlContent)
     pw.close
  }
      
  @deprecated
  def saveOpmlStorage(opmlStorage: OpmlStorage) = {
    Await.result(
        db.run(
            opmls.insertOrUpdate(opmlStorage)
        ).map(_ => ()), 
        Duration.Inf
    )

  }
  
  def getOpmlById(userId: Long): Option[Opml] = {
    val userOpmlFilePath = s"${opmlFileFolderPath}/${userId}.xml"
    val userOpmlFile = new File(userOpmlFilePath)
    if( userOpmlFile.exists() ){
      val fileContents = Source.fromFile(userOpmlFile).getLines.mkString
      val xmlContent = XML.loadString(fileContents)
      Some(Opml(userId,xmlContent))
    }
    else{
      None
    }
  }
 
  
  def getOpmlByIdStreamed(userId: Long) = {
    val opmlQuery = for (o <- opmls if o.userId === userId ) yield o.raw
    val opmlQueryResult = opmlQuery.result
    val blobSource: DatabasePublisher[Blob] = db.stream(opmlQueryResult)
    val byteSource: DatabasePublisher[Array[Byte]] = blobSource.mapResult { b =>
      b.getBytes(0, b.length().toInt)
    }
    
  }
  
  //Opml structure should be updated in client side and save the whole Opml here
  def addOmplOutline(uid: Long, ol:OpmlOutline) = {
    Await.result(
        db.run(
            opmls.filter( _.userId === uid ).result.headOption map { r1=>
              r1 match{
                 case Some(uoo) => {
                   val curopml = Opml( uid, uoo.toOpml.outline :+ ol )//TODO: whatif this feed is already subscribed
                   opmls.update(curopml.toStorage)
                   saveOpml(curopml)
                 }
                 case None =>{
                   val newopml = Opml( uid, List( ol ) )
                   opmls+=(newopml.toStorage)
                   saveOpml(newopml)
                 }
              } 
            }
        ), 
        Duration.Inf
    )   
  }
}