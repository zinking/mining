package mining.util

import scala.util.Properties
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object EnvUtil {
  val logger = LoggerFactory.getLogger(getClass())

  def runMode = Properties.envOrElse("runMode", "prod")
  
  def configs = {
    val mode = runMode
    logger.info(s"Loading configuration of $mode environment")
    ConfigFactory.load(runMode)
  }
}