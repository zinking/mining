package mining.io

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
  //if an entry is not in ReadStory then it's *definitely* unread
  //if an entry is marked as unread, it is different from unread mentioned above
}