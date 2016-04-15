package mining.io

import java.nio.file.FileSystems
import java.io.File
import mining.util.DirectoryUtil

trait FeedTestPrepare {
    var fmPath: String = null

    def prepareFolders() = {
        //tmp folder under project directory
        val tmpFolderPath = DirectoryUtil.pathFromProject("tmp")
        val tmpFolder = new File(tmpFolderPath)
        if (!tmpFolder.exists()) tmpFolder.mkdir()

        ////A LITTLE DANGEROUS THOUGH
        cleanUpFolder(tmpFolder)

        //feed manager directory
        val fmFolderPath = DirectoryUtil.pathFromProject("tmp", "fm")
        val fmFolder = new File(fmFolderPath)

        //feed ser file directory
        val serFolderPath = DirectoryUtil.pathFromProject("tmp", "ser")
        val serFolder = new File(serFolderPath)

        val userFolderPath = DirectoryUtil.pathFromProject("target", "userompl")
        val userFolder = new File(userFolderPath)

        System.setProperty("mining.feedmgr.path", fmFolderPath)
        System.setProperty("mining.ser.path", serFolderPath)
        fmPath = DirectoryUtil.pathFromPaths(fmFolderPath, "feedmanager.ser")

        if (!fmFolder.exists()) fmFolder.mkdir()
        if (!serFolder.exists()) serFolder.mkdir()
        if (!userFolder.exists()) serFolder.mkdir()

        cleanUpFolder(fmFolder)
        cleanUpFolder(serFolder)
        cleanUpFolder(userFolder)
    }

    def cleanUpFolder(folder: File) = for (subfile <- folder.listFiles()) subfile.delete()

}