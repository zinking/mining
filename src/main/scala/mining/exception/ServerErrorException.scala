package mining.exception

/**
 * requested site encountered some error
 * Created by awang on 26/3/16.
 */
case class ServerErrorException(message: String) extends Exception{

}

case class ServerNotExistException(message: String) extends Exception{

}

case class PageNotChangedException(message: String) extends Exception{

}

case class InvalidFeedException(message: String) extends Exception{

}



