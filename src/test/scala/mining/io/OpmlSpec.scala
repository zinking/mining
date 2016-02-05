package mining.io

import scala.xml._

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.ShouldMatchers


@RunWith(classOf[JUnitRunner])
class OpmlSpec extends FunSuite
with ShouldMatchers
with BeforeAndAfterAll
with FeedTestPrepare {


    val dom: Elem =
        <opml version="1.0">
            <head>
                <title>Sample</title>
            </head>
            <body>
                <outline text="We need more..." title="We need more..." type="rss"
                         xmlUrl="http://blog.csdn.net/zhuliting/rss/list" htmlUrl="http://blog.csdn.net/zhuliting"/>
                <outline title="FlexBlogs" text="FlexBlogs">
                    <outline text="AdobeAll-Bee" title="AdobeAll-Bee" type="rss"
                             xmlUrl="http://www.beedigital.net/blog/?feed=rss2" htmlUrl="http://www.beedigital.net/blog"/>
                </outline>
            </body>
        </opml>
    var opml1: Opml = Opml(1L, dom)

    test("Opml should be able to load from XML and convert back to XML") {
        opml1.id should be(1L)
        opml1.outlines.size should be(2)
        opml1.outlines(1).outlines.size should be(1)
        opml1.outlines(1).outlines.head.xmlUrl should be("http://www.beedigital.net/blog/?feed=rss2")

        val dom1 = opml1.toXml
        //(dom1\"head"\"title").text should be ("sample's subscription")
        (dom1 \ "body" \ "outline").length should be(2)
        val dom2: Node = (dom1 \ "body" \ "outline").drop(1).headOption.get
        (dom2 \ "@title").toString should be("FlexBlogs")
        (dom2 \ "outline").length should be(1)
        (dom2 \ "outline" \ "@title").toString should be("AdobeAll-Bee")
    }

    test("Opml allFeedsUrl should return all the feeds in it") {
        opml1.allFeedsUrl.foreach(url => println(s"Opml contains feed url: $url"))
        opml1.allFeedsUrl.size should be(2)
    }

    test("Opml should be merged correctly") {
        val merged = opml1.mergeWith(opml1)
        merged.id should be(1L)
        merged.outlines.size should be(2)
        merged.outlines(1).outlines.size should be(1)
        merged.outlines(1).outlines.head.xmlUrl should be("http://www.beedigital.net/blog/?feed=rss2")

        val dom1 = opml1.toXml
        //(dom1\"head"\"title").text should be ("sample's subscription")
        (dom1 \ "body" \ "outline").length should be(2)
        val dom2: Node = (dom1 \ "body" \ "outline").drop(1).headOption.get
        (dom2 \ "@title").toString should be("FlexBlogs")
        (dom2 \ "outline").length should be(1)
        (dom2 \ "outline" \ "@title").toString should be("AdobeAll-Bee")

    }
}