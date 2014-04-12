package mining.io

import java.sql.Date
import scala.xml._
import mining.util.DirectoryUtil
import scala.collection.mutable.ListBuffer
import java.sql.Blob
import java.io.InputStreamReader
import javax.sql.rowset.serial.SerialBlob



case class OpmlOutline(
    outline:List[OpmlOutline], 
    title:String,
    xmlUrl:String,
    outlineType:String,
    text:String,
    htmlUrl:String
){

   def toXml():Node={
     <outline text={text} title={title} type={outlineType} xmlUrl={xmlUrl} htmlUrl={xmlUrl}>{
	    for { o <- outline  } yield o.toXml
	 }</outline>
   }
}


object OpmlOutline{
  def apply( children:List[OpmlOutline], node:Node)={
    new OpmlOutline(
        children, (node\"@title").toString, (node\"@xmlUrl").toString, 
        (node\"@type").toString, (node\"@text").toString, (node\"@htmlUrl").toString
    )
  }
  
  def apply( title:String, xmlUrl:String, outType:String, text:String, htmlUrl:String )={
    new OpmlOutline(List[OpmlOutline](), title,xmlUrl,outType,text,htmlUrl)
  }
  
  def empty():OpmlOutline={
    new OpmlOutline(List[OpmlOutline](), "","","","","")
  }
}

case class OpmlStorage(
    id:String,
    raw:Blob
){
    def toOpml():Opml={
      Opml( id, XML.load( new InputStreamReader(raw.getBinaryStream,"UTF-8")) )
    }
}

case class Opml(
    id:String,
    outline:List[OpmlOutline]
){
   def toXml():Elem={
      	<opml version="1.0">
		    <head><title>{id}'s subscription</title></head>
		    <body>
		    {
			    for { o <- outline  }
			    	yield o.toXml
		    }
		    </body>
    	</opml>
    }
   
   def toStorage():OpmlStorage = {
     new OpmlStorage(id, new SerialBlob( toXml.toString.getBytes("UTF-8") ) )
   }
   
}

object Opml{
  def apply( id:String, dom:Elem) = {
    val outline1 = dom \ "body" \ "outline"
    val result = outline1.foldLeft[List[OpmlOutline]]( List[OpmlOutline]() )(( acc, node ) =>{
    	val outline2 = node \ "outline"
    	val result2 = outline2.foldLeft[List[OpmlOutline]]( List[OpmlOutline]() )(( acc2, node2 ) =>{
    		val nid2 = OpmlOutline( List[OpmlOutline](), node2 )
    		acc2 :+ nid2
    	})
    	val nid = OpmlOutline(result2, node )
        acc :+ nid
    })
    new Opml( id,  result)
  }	
}



    
    
