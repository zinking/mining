package mining.util

import scala.util.Properties
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object EnvUtil {
    val logger = LoggerFactory.getLogger(getClass())

    def runMode = Properties.propOrElse("runMode", "prod")

    def configs = {
        val mode = runMode
        logger.info(s"Loading configuration of $mode environment")
        ConfigFactory.load(runMode)
    }

    def time[R](block: => R): R = {
        val t0 = System.nanoTime()
        val result = block
        val t1 = System.nanoTime()
        logger.info("Elapsed time: " + (t1 - t0) + "ns")
        result
    }
}