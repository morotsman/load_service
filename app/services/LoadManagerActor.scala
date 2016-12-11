package services

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.util.Timeout

import model.LoadSpec
import LoadSessionActor._
import play.api.libs.ws._

object LoadManagerActor {
  def props(ws:WSClient) = Props[LoadManagerActor](new LoadManagerActor(ws))

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

  var ongoingSessions: Map[String, ActorRef] = Map()

  var loadResources: Map[String, LoadSpec] = Map()
  
  var index:Integer = 0
  
  def hasSession(name: String): Boolean = 
    ongoingSessions.get(name).map(s => true).getOrElse(false)
  

  def receive = {
    case StartLoadSession(name) => 
      if(hasSession(name)) {
        sender ! 403
      } else {
       val result = loadResources.get(name) match {
        case None => 404
        case Some(s) =>
          val session = context.actorOf(LoadSessionActor.props(name,s,ws), "load-session-" + name)
          ongoingSessions += (name -> session)
          session ! StartSession
          201
        }
       sender ! result
      }
    case EndLoadSession(name) => 
      if(hasSession(name)) {
        
        ongoingSessions(name) ! EndSession
        ongoingSessions -= name
        sender ! 200
      } else {
        sender ! 404
      }
    case ListLoadResources =>
      sender ! loadResources.keys
    case CreateLoadReource(loadSpec) =>
      loadResources = loadResources + ((index+"") -> loadSpec)
      index = index + 1
      sender ! loadSpec
    case UpdateLoadResource(name, loadSpec) =>
      loadResources = loadResources + (name -> loadSpec)
      sender ! loadSpec
    case DeleteLoadResource(name) =>
      loadResources = loadResources - name
      sender ! "Ok"
    case GetLoadResource(name) =>
      sender ! loadResources.get(name)

  }

}