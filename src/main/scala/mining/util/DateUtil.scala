package mining.util

import java.text.SimpleDateFormat
import java.util.Locale

object DateUtil {
    def getParser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)

    def getSqlDate(date: java.util.Date) = {
        if (date != null) new java.sql.Date(date.getTime())
        else null
    }
}