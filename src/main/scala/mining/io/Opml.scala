package mining.io

import scala.xml.Elem
import scala.xml.Node
import scala.xml.XML
import scala.beans.BeanProperty

case class OpmlOutline(outlines: List[OpmlOutline],
                       title: String,
                       xmlUrl: String,
                       outlineType: String,
                       text: String,
                       htmlUrl: String) {

    def toXml: Node = {
        <outline text={text} title={title} type={outlineType} xmlUrl={xmlUrl} htmlUrl={xmlUrl}>
            {for {o <- outlines} yield o.toXml}
        </outline>
    }

    def containsFeed(fd: Feed): Boolean =
        allFeedsUrl.contains(fd.xmlUrl)

    @BeanProperty
    lazy val allOutlines: List[OpmlOutline] = {
        if (outlines.isEmpty) List(this)
        else outlines.foldLeft(List.empty[OpmlOutline])((acc, node) => acc ++ node.allOutlines)
    }


    @BeanProperty
    lazy val allFeedsUrl: List[String] =
        allOutlines.map(_.xmlUrl)

    def mergeWith(maybeThat: Option[OpmlOutline]):OpmlOutline = {
        //pre-requisite. the two can be combined
        maybeThat match {
            case Some(that) =>
                if (outlines.nonEmpty) {
                    // the case where two folders should be combined
                    val thisMap = OpmlOutline.outlines2Map(outlines)
                    val thatMap = OpmlOutline.outlines2Map(that.outlines)

                    val combined:Map[String,OpmlOutline] = thisMap ++ thatMap.map { case (k, v) =>
                        k -> thisMap.getOrElse(k, v)
                    }
                    val combinedList = combined.values.toList
                    this.copy(outlines=combinedList)
                } else {
                    // the case where one feed gets merged with another
                    this
                }
            case None =>
                this
        }

    }

}

object OpmlOutline {
    def apply(children: List[OpmlOutline], node: Node) = {
        new OpmlOutline(
            children, (node \ "@title").toString(), (node \ "@xmlUrl").toString(),
            (node \ "@type").toString(), (node \ "@text").toString(), (node \ "@htmlUrl").toString()
        )
    }

    def apply(title: String, xmlUrl: String, outType: String, text: String, htmlUrl: String) = {
        new OpmlOutline(List[OpmlOutline](), title, xmlUrl, outType, text, htmlUrl)
    }

    def empty() = {
        new OpmlOutline(List[OpmlOutline](), "", "", "", "", "")
    }

    /**
     * function to convert outlines to map from unique indentifier to the outline
     */
    //val outlines2Map : List[OpmlOutline] => Map[String,OpmlOutline] = (outlines:List[OpmlOutline]) => {
    def outlines2Map(outlines:List[OpmlOutline]):Map[String,OpmlOutline] = {
        outlines map { outline=>
            if (outline.outlines.isEmpty) { //this is a feed
                outline.xmlUrl->outline
            } else {
                outline.title->outline
            }
        } toMap
    }
}

/** OPML XML stored as blob in database */
case class OpmlStorage(id: Long, raw: String) {
    def toOpml: Opml = {
        Opml(id, XML.loadString(raw))
    }
}


case class Opml(id: Long, outlines: List[OpmlOutline]) {
    def toXml: Elem = {
        <opml version="1.0">
            <head>
                <title> {id} 's subscription</title>
            </head>
            <body>
                {for {o <- outlines}
                yield o.toXml}
            </body>
        </opml>
    }

    def toStorage: OpmlStorage = new OpmlStorage(id,toXml.toString())

    def containsFeed(fd: Feed): Boolean = allFeedsUrl.contains(fd.xmlUrl)

    def containsFeedUrl(feedUrl: String): Boolean = allFeedsUrl.contains(feedUrl)

    @BeanProperty
    lazy val allOutlines: List[OpmlOutline] =
        outlines.foldLeft(List.empty[OpmlOutline])((acc, node) => acc ++ node.allOutlines)

    @BeanProperty
    lazy val allFeedsUrl: List[String] =
        allOutlines.map(_.xmlUrl)
        
    def mergeWith(thatOpml: Opml) :Opml = {
        // two cases to merge
        // first: merge folder with folder
        // collapse feed
        val thisMap = OpmlOutline.outlines2Map(outlines)
        val thatMap = OpmlOutline.outlines2Map(thatOpml.outlines)

        val combined = thisMap ++ thatMap.map{ case (k,v) =>
            k -> v.mergeWith(thisMap.get(k))
        } values

        this.copy(outlines=combined.toList)
    }

}

object Opml {
    def apply(id: Long, dom: Elem) = {
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
        new Opml(id, result)
    }
}