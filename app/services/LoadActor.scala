package services

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.util.Timeout
import javax.inject._
import scala.concurrent.{ ExecutionContext, Future, Promise }

import model.LoadSpec

import play.api.libs.ws._
import play.api.http.HttpEntity

object LoadActor {
  def props(ws: WSClient, url: String) = Props[LoadActor](new LoadActor(ws,url))

  case class SendRequest(request: Int)
}

class LoadActor(val ws: WSClient, val url: String) extends Actor {
  import LoadActor._

  def receive = {
    case SendRequest(id) =>
      val request: WSRequest = ws.url(url)
      val complexRequest: WSRequest =
        request.withHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)
          .withQueryString("search" -> "play")
      complexRequest.get()
    case _ =>
      println("Handle error")
  }

}