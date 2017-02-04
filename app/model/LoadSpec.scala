package model

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class NameValue(val name: String, val value: String)

object NameValue {
  
  implicit val itemReads = Json.reads[NameValue]
  implicit val itemWrites = Json.writes[NameValue]  
}


case class LoadSpec(val method: String, val url: String, val numberOfRequestPerSecond: Int, val maxTimeForRequestInMillis: Int, 
    val body: String, val status: Option[String], val id: Option[String], val expectedBody: Option[String], 
    val expectedResponseCode: Option[String], val headers: Seq[NameValue], val requestParameters: Seq[NameValue], 
    val numberOfSendingSlots: Option[Int], val rampUpTimeInSeconds: Option[Int], val fromNumberOfRequestPerSecond: Option[Int])

object LoadSpec {
  
  implicit val itemReads = Json.reads[LoadSpec]
  implicit val itemWrites = Json.writes[LoadSpec]
 
}