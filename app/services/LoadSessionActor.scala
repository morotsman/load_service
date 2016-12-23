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
  def props(name: String, loadSpec: LoadSpec, ws: WSClient) = Props[LoadSessionActor](new LoadSessionActor(name, loadSpec, ws))

  case class StartSession()
  case class EndSession()
  case class SendRequests(numberOfRequests: Int)
}

class LoadSessionActor(val name: String, val loadSpec: LoadSpec, val ws: WSClient) extends Actor {
  import LoadSessionActor._

  val loadActor = context.actorOf(LoadActor.props(ws, loadSpec), "load-actor-" + name)
  var cancellables: List[Cancellable] = Nil
  var index = 0

  def numberOfRequestPerSlot(nrOfSlots: Int, nrOfRequestPerSecond: Int): List[(Int, Int)] = {
    val rests = List.fill(nrOfRequestPerSecond % nrOfSlots)(1)
    val wholes = List.fill(nrOfSlots)(nrOfRequestPerSecond / nrOfSlots)
    wholes.zipAll(rests, 0, 0).map(x => x._1 + x._2).zipWithIndex
  }

  def receive = {
    case StartSession =>
      println("LoadSessionActor: Start Session: " + loadSpec)

      val numberOfSlots: Int = loadSpec.numberOfRequestPerSecond
      
      cancellables = numberOfRequestPerSlot(numberOfSlots, loadSpec.numberOfRequestPerSecond).map(numberOfRequests => {
        val index = numberOfRequests._2
        context.system.scheduler.schedule((index * (1000/numberOfSlots)).millis, 1000.millis, self, SendRequests(numberOfRequests._1))
      })
    case SendRequests(numberOfRequests) =>
      for (
        request <- index to (index + numberOfRequests - 1)
      ) { loadActor ! SendRequest(request) }
      index = index + numberOfRequests - 1
    case EndSession =>
      println("LoadSessionActor: Stop Session")
      cancellables.foreach { x => x.cancel }
      context.stop(loadActor)
      context.stop(self)
  }

}