package mining.io

import java.sql.Date

import mining.util.DirectoryUtil
import mining.util.UrlUtil

/**
 * It's allowed to use URL only as constructor, in major to facilitate testing. 
 * But in real world the feed descriptor should be retrieved from FeedManager, to keep it in sync.
 */
case class Feed(val url: String,
                var feedId: Long = 0L,
                var lastEtag: String = "",
                var checked: Date = new Date(0), 
                var lastUrl: String = "", 
                var encoding: String = "UTF-8") {
  
  def uid = UrlUtil.urlToUid(url)

  def filePath =  DirectoryUtil.pathFromPaths(System.getProperty("mining.ser.path"), uid + ".ser")
  
  override def toString = s"FeedDescriptor[$url]"
}

