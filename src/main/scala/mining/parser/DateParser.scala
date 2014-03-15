package mining.parser

import java.text.SimpleDateFormat
import java.util.Locale

object DateParser {
	def parseRSSDate(dateStr: String) = {
	  //Try English locale date format first
	  val df1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
	  df1.parse(dateStr)
	}
}