package mining.model

import java.util.Date

/**
 * Created by awang on 5/12/15.
 */
case class AuthUser
(
    userId:Long,
    email:String,
    name:String,
    pass:String,
    apiKey:String,
    lastLoginFrom:String,
    lastLoginTime:Date
)
