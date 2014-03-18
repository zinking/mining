package mining.util

import java.text.SimpleDateFormat
import java.util.Locale

object DateUtil {
  def getParser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
}