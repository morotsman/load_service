package services

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.util.Timeout
import javax.inject._
import scala.concurrent.{ ExecutionContext, Future, Promise }
import java.net.ConnectException

import model._

import play.api.libs.ws._
import play.api.http.HttpEntity

import services.StatisticsActor._

object LoadActor {
  def props(ws: WSClient, loadSpec: LoadSpec) = Props[LoadActor](new LoadActor(ws, loadSpec))

  case class SendRequest(request: Int)
  case class Failure(t: Throwable)
}

class LoadActor(val ws: WSClient, val loadSpec: LoadSpec) extends Actor {
  import LoadActor._

  val eventBus = context.system.eventStream
  
  
  override def postRestart(reason: Throwable) {
    super.postRestart(reason)
    println(s"Restarted because of ${reason.getMessage}")
  }
  
  def receive = {
    case SendRequest(requestNumber) =>
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
        case e : ConnectException => 
          println("ConnectException")
          self ! Failure(e)
        case e =>
          loadSpec.id foreach { id => 
            eventBus.publish(FailedRequest(ResourceKey(loadSpec.method, loadSpec.url, id), e, System.currentTimeMillis - startTime))
          }
          
      })
      
      loadSpec.id foreach { id => 
        futureResult.map(_ => SuccessfulRequest(ResourceKey(loadSpec.method, loadSpec.url, id), System.currentTimeMillis - startTime)).foreach(eventBus.publish)
      }

    case Failure(e) =>
      throw new LoadConnectException(loadSpec.id, e)
    case _ =>
      println("Handle error")
  }

}