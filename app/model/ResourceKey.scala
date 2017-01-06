package model

case class ResourceKey(method: String,url: String, id: String)

import play.api.libs.json._
import play.api.libs.functional.syntax._

object ResourceKey {
  implicit val resourceKeyWrites = new Writes[ResourceKey] {
    def writes(loadSpec: ResourceKey) = Json.obj(
      "method" -> loadSpec.method,
      "url" -> loadSpec.url,
      "id" -> loadSpec.id);

  }

  implicit val resourceKeyReads: Reads[ResourceKey] = (
    (JsPath \ "method").read[String] and
    (JsPath \ "url").read[String] and
    (JsPath \ "id").read[String])(ResourceKey.apply _)

}