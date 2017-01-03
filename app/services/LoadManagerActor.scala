package services

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.util.Timeout

import model.LoadSpec
import model.LoadSession
import LoadSessionActor._
import play.api.libs.ws._

import services.StatisticsActor._

object LoadManagerActor {
  def props(ws: WSClient) = Props[LoadManagerActor](new LoadManagerActor(ws))

  case class ListLoadResources()
  case class GetLoadResource(index: String)
  case class CreateLoadReource(loadSpec: LoadSpec)
  case class UpdateLoadResource(index: String, loadSpec: LoadSpec)
  case class DeleteLoadResource(index: String)

  case class ListLoadSessions()
  case class StartLoadSession(name: String)
  case class EndLoadSession(name: String)
}

class LoadManagerActor(val ws: WSClient) extends Actor {
  import LoadManagerActor._

  var loadResources: Map[String, LoadSpec] = Map()

  var index: Integer = 0

  def receive = {
    case StartLoadSession(id) =>
      val resource = loadResources.get(id) 

      resource foreach { r =>
          if (r.status.getOrElse("Inactive") == "Inactive") {
            val session = context.actorOf(LoadSessionActor.props(id, r, ws), "load-session-" + id)
            loadResources = loadResources + (id -> r.copy(status = Some("Active")))
            session ! StartSession
          }
      }
      
      val session = resource map { r => LoadSession(id) }  
      sender ! session
    case EndLoadSession(id) =>
      val rs = for (
        r <- loadResources.get(id);
        s <- context.child("load-session-" + id)
      ) yield (r, s)

      rs foreach { case(r,s) =>
          s ! EndSession
          loadResources = loadResources + (id -> r.copy(status = Some("Inactive")))
      }
      
      val session = (rs map { _ => LoadSession})
      sender ! session
    case ListLoadResources =>
      sender ! loadResources.keys
    case CreateLoadReource(loadSpec) =>
      loadResources = loadResources + ((index + "") -> loadSpec.copy(status = Some("Inactive"), id = Some(index + "")))
      index = index + 1
      context.system.eventStream.publish(LoadResourceCreated(loadSpec))
      sender ! loadSpec
    case UpdateLoadResource(id, loadSpec) =>
      val resource = loadResources.get(id) map { r =>
        loadSpec.copy(status = r.status)
      }
      
      resource foreach { r => 
        loadResources = loadResources + (id -> r)
      }
      
      sender ! resource
    case DeleteLoadResource(id) =>
      val resource = loadResources.get(id)

      resource.foreach { r =>
        loadResources = loadResources - id
        context.system.eventStream.publish(LoadResourceDeleted(r))
        context.child("load-session-" + id) foreach { a =>
          a ! EndSession
        }
      }

      sender ! resource
    case GetLoadResource(name) =>
      sender ! loadResources.get(name)

  }

}