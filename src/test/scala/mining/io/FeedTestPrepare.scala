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
    //TODO: we need to delete all the files produced instead of using this
    cleanUpFolder(tmpFolder) 

    //feed manager directory
    val fmFolderPath = DirectoryUtil.pathFromProject("tmp", "fm")
    val fmFolder = new File(fmFolderPath)
    
    //feed ser file directory
    val serFolderPath = DirectoryUtil.pathFromProject("tmp", "ser")
    val serFolder = new File(serFolderPath)

    System.setProperty("mining.feedmgr.path", fmFolderPath)
    System.setProperty("mining.ser.path", serFolderPath)
    fmPath = DirectoryUtil.pathFromPaths(fmFolderPath, "feedmanager.ser")
    
    if (!fmFolder.exists()) fmFolder.mkdir()
    if (!serFolder.exists()) serFolder.mkdir()
    
    cleanUpFolder(fmFolder)
    cleanUpFolder(serFolder)
  }
  
  def cleanUpFolder(folder: File) = for (subfile <- folder.listFiles()) subfile.delete()
  
}