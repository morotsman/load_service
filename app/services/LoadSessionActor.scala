package services

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.util.Timeout

import model.LoadSpec

import LoadActor._
import akka.actor.Cancellable
import play.api.libs.ws._

object LoadSessionActor {
  def props(name:String, loadSpec: LoadSpec, ws: WSClient) = Props[LoadSessionActor](new LoadSessionActor(name,loadSpec,ws))
  
  case class StartSession()
  case class EndSession()
  case class SendRequests()
}




class LoadSessionActor(val name: String, val loadSpec: LoadSpec,val ws:WSClient) extends Actor {
  import LoadSessionActor._
  
  val loadActor = context.actorOf(LoadActor.props(ws,loadSpec.url), "load-actor-" + name)
  var cancellable: Cancellable = null
  var index = 0
  
  def receive = {
    case StartSession =>
      println("LoadSessionActor: Start Session: " + loadSpec)
      cancellable = context.system.scheduler.schedule(0.millis, 1000.millis,self, SendRequests)
    case SendRequests => 
      for(
        request <- index to (index + loadSpec.numberOfRequestPerSecond - 1)    
      ) {loadActor ! SendRequest(request)}
      index = index + loadSpec.numberOfRequestPerSecond - 1
    case EndSession => 
      println("LoadSessionActor: Stop Session")
      cancellable.cancel
      context.stop(loadActor)
      context.stop(self)
  }
 
}