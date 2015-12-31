package mining.util

import java.io.File
import java.nio.file.FileSystems

object DirectoryUtil {
    val seperator = FileSystems.getDefault.getSeparator

    val projectPath = new File(".").getCanonicalPath

    def pathFromProject(paths: String*) = pathFrom(projectPath, paths)

    def pathFromPaths(base: String, paths: String*) = pathFrom(base, paths)

    private def pathFrom(base: String, paths: Seq[String]) = Seq(base) ++ paths mkString seperator
}