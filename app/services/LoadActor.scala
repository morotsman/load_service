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

import services.StatisticsActor._

object LoadActor {
  def props(ws: WSClient, loadSpec: LoadSpec) = Props[LoadActor](new LoadActor(ws, loadSpec))

  case class SendRequest(request: Int)
}

class LoadActor(val ws: WSClient, val loadSpec: LoadSpec) extends Actor {
  import LoadActor._

  var actorStoped = false;
  
  override def postStop: Unit = {
    println("LoadActor stopped")
    actorStoped = true
  }
  
  def receive = {
    case SendRequest(id) =>
      val startTime = System.currentTimeMillis
      val request: WSRequest = ws.url(loadSpec.url)
      val complexRequest: WSRequest =
        request.withHeaders("Accept" -> "application/json")
          .withRequestTimeout(loadSpec.maxTimeForRequestInMillis.millis)
          .withQueryString("search" -> "play")
      val futureResult: Future[WSResponse] = if (loadSpec.method == "GET") {
        complexRequest.get()
      } else if (loadSpec.method == "PUT") {
        complexRequest.put(loadSpec.body)
      } else if (loadSpec.method == "POST") {
        complexRequest.post(loadSpec.body)
      } else {
        complexRequest.delete
      }

      futureResult.recover({
        case e =>
          context.system.eventStream.publish(FailedRequest(loadSpec, e, System.currentTimeMillis - startTime))
      })

      
      futureResult.map(
        x => SuccessfulRequest(loadSpec, System.currentTimeMillis - startTime)).foreach(r => {
          if(!actorStoped) {//TODO better way to do this? Get a nullpointer if i try to publish when the actor has terminated
            context.system.eventStream.publish(r)
          }
        })

    case _ =>
      println("Handle error")
  }

}