package model

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class LoadSpec(val method: String, val url: String, val numberOfRequestPerSecond: Int, val maxTimeForRequestInMillis: Int, val body: String, val status: Option[String], val id: Option[String])

object LoadSpec {
  implicit val loadSpecWrites = new Writes[LoadSpec] {
    def writes(loadSpec: LoadSpec) = Json.obj(
      "method" -> loadSpec.method,
      "url" -> loadSpec.url,
      "numberOfRequestPerSecond" -> loadSpec.numberOfRequestPerSecond,
      "maxTimeForRequestInMillis" -> loadSpec.maxTimeForRequestInMillis,
      "body" -> loadSpec.body,
      "status" -> loadSpec.status,
      "id" -> loadSpec.id);

  }

  implicit val loadSpecReads: Reads[LoadSpec] = (
    (JsPath \ "method").read[String] and
    (JsPath \ "url").read[String] and
    (JsPath \ "numberOfRequestPerSecond").read[Int] and
    (JsPath \ "maxTimeForRequestInMillis").read[Int] and
    (JsPath \ "body").read[String] and 
    (JsPath \ "status").readNullable[String] and 
    (JsPath \ "id").readNullable[String])(LoadSpec.apply _)

}