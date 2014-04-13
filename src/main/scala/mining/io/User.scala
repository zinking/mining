package mining.io

class User {

}

//options:{"folderClose":{},"nav":true,"expanded":false,"mode":"all","sort":"newest","hideEmpty":false,"scrollRead":false}
case class Setting(
		userId:String,
		sort:String,
		display:String,
		hideEmtpy:String
    )
{
  }

case class ReadStory(
    userId:String,
    storyId:String,
    star:String,
    read:String
    ){
  //star: "STAR" | ""
  //read: "MARK" | "READ" | "UNREAD"
}