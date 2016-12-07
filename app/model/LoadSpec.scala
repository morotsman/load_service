package model

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class LoadSpec(val url: String, val numberOfRequestPerSecond: Int)


object LoadSpec {
  implicit val loadSpecWrites = new Writes[LoadSpec] {
    def writes(loadSpec: LoadSpec) = Json.obj(
      "url" -> loadSpec.url,
      "numberOfRequestPerSecond" -> loadSpec.numberOfRequestPerSecond);

  }

  implicit val loadSpecReads: Reads[LoadSpec] = (
    (JsPath \ "url").read[String] and
    (JsPath \ "numberOfRequestPerSecond").read[Int])(LoadSpec.apply _)

}