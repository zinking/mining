package mining.io

import java.util.Date

import spray.json._


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

/**
 * The design of user read story stats
 * a read story should not be unread
 * a starred story should not be un-starred
 * in case of the mis-operation, leave it alone, it is mis-operation no big deal
 *
 * the `startFrom` concept: from user's point of view, the starting point where he wants to read the feed
 * that means before this `startFrom`, the stories of the feed is either `read` or he doesn't care
 * so when a user mark a feed `all read`, he moved his start cursor of the feed to that time point
 * thus the counters should be set accordingly from that point of view
 * the actions `read` happen before that point is no longer considered
 *
 * for example, assume one feed has 3 stories at 2001,2002,2003
 * by default the `startFrom` pointer is set at 2000, so user has 3 unread story
 * if user read the 2001 story, then he has 2 unread story
 * if user marked the feed all read at time point 2001-10-01, then stories being considered for the user regarding
 * stats only contain story 2002 and 2003, and previous read doesn't count any more
 * so after that action, the user has 2 unread story left
 * the 2001 story is no longer taken into consideration when calculating unreads
 *
 * Test spec refer to <code>SlickUserDAOSpec</code>
 *
 * @param userId id of the user
 * @param feedId if of the feed
 * @param storyId id of the story being stated
 * @param hasRead whether the story has been read
 * @param hasLike whether the story has been starred
 * @param comment comment provided
 */
case class UserStat
(
    userId: Long,
    feedId: Long,
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

case class UserFeedReadStat
(
    userId: Long,
    feedId: Long,
    unreadCount: Long,
    startFrom: Date
)

case class UserFollow
(
    userId: Long,
    following: Long
)


object UserFactory {
    import UserProtocol._
    def defaultPref = UserPref(
        "asc","yes",
        hideEmpty = true,
        folderCose = true,
        nav = true,
        expand = false,
        "tiny",
        scrollRead = true
    )

    def newUser(userId: Long, email: String) = User(userId, email, defaultPref.toJson.compactPrint)
}

case class UserActionStat
(
    timeStamp: Date,
    action: String,
    userId: Long,
    feedId: Long,
    storyId: Long,
    content: String
) {

}