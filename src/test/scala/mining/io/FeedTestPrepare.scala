package mining.io

import java.nio.file.FileSystems
import java.io.File

trait FeedTestPrepare {
  var fmPath: String = null

  def prepareFolders() = {
    val sep = FileSystems.getDefault().getSeparator() 
    
    //tmp folder under project directory
    val tmpFolderPath = List(new File(".").getCanonicalPath(), "tmp") mkString(sep)
    val tmpFolder = new File(tmpFolderPath)
    if (!tmpFolder.exists()) tmpFolder.mkdir()

    ////A LITTLE DANGEROUS THOUGH
    //TODO: we need to delete all the files produced instead of using this
    cleanUpFolder(tmpFolder) 

    //feed manager directory
    val fmFolderPath = List(new File(".").getCanonicalPath(), "tmp", "fm") mkString(sep)
    val fmFolder = new File(fmFolderPath)
    
    //feed ser file directory
    val serFolderPath = List(new File(".").getCanonicalPath(), "tmp", "ser") mkString(sep)
    val serFolder = new File(serFolderPath)

    System.setProperty("mining.feedmgr.path", fmFolderPath)
    System.setProperty("mining.ser.path", serFolderPath)
    fmPath = fmFolderPath + sep + "feedmanager.ser"
    
    if (!fmFolder.exists()) fmFolder.mkdir()
    if (!serFolder.exists()) serFolder.mkdir()
    
    cleanUpFolder(fmFolder)
    cleanUpFolder(serFolder)
  }
  
  def cleanUpFolder(folder: File) = for (subfile <- folder.listFiles()) subfile.delete()
  
}