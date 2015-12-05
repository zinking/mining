package mining.io

import spray.json._
import DefaultJsonProtocol._


object UserProtocol extends DefaultJsonProtocol {
    implicit val userPrefFormat = jsonFormat8(UserPref.apply)
}

case class UserPref
(
    sort:String,
    display:String,
    hideEmpty:Boolean,
    folderCose:Boolean,
    nav:Boolean,
    expand:Boolean,
    mode:String,
    scrollRead:Boolean
)
//options:{"folderClose":{},"nav":true,"expanded":false,"mode":"all","sort":"newest","hideEmpty":false,"scrollRead":false}
case class User
(
    userId: Long,
    email: String,
    prefData: String
)
{
    def pref:UserPref={
        import UserProtocol._
        prefData.parseJson.convertTo[UserPref]
    }

}

case class UserStory
(
    userId: Long,
    storyId: Long,
    hasRead: Int,
    hasLike: Int,
    comment: String 
) {
    //star: "STAR" | ""
    //read: "MARK" | "READ" | "UNREAD"
    //if an entry is not in ReadStory then it's *definitely* unread
    //if an entry is marked as unread, it is different from unread mentioned above
}


object UserFactory {
    import UserProtocol._
    def defaultPref = UserPref("asc","yes",true,true,true,false,"tiny",true)
    def newUser(userId: Long, email: String) = User(userId, email, defaultPref.toJson.compactPrint)
}