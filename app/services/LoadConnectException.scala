package services

class LoadConnectException(val id: Option[String], val cause: Throwable) extends RuntimeException