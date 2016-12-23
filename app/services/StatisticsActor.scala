package services

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.util.Timeout
import model._

object StatisticsActor {
  def props = Props[StatisticsActor]
  
  case class FailedRequest(loadSpec: LoadSpec, e: Throwable)
  case class SuccessfulRequest(loadSpec:LoadSpec)
  case class AgggregateStatistcs()
  
  case class LoadResourceCreated(loadSpec:LoadSpec)
  case class LoadResourceDeleted(loadSpec:LoadSpec)
}

class StatisticsActor extends Actor{
  import StatisticsActor._
  
  val tmp = (1,2)
  
  type Method = String
  type Url = String
  case class ResourceKey(method: Method,url: Url)
  
  var successfulRequestsLastSecond : scala.collection.mutable.Map[ResourceKey, Int] = scala.collection.mutable.Map()
  var failedRequestsLastSecond : scala.collection.mutable.Map[ResourceKey, Int] = scala.collection.mutable.Map()
  
  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce(1000.millis,self, AgggregateStatistcs) 
    context.system.eventStream.subscribe(context.self, classOf[FailedRequest])
    context.system.eventStream.subscribe(context.self, classOf[SuccessfulRequest])
    context.system.eventStream.subscribe(context.self, classOf[LoadResourceCreated])
    context.system.eventStream.subscribe(context.self, classOf[LoadResourceDeleted])
    super.preStart()
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(context.self)
    super.postStop()
  }
  
  println("StatisticsActor created")
  
  def getKey(loadSpec: LoadSpec): ResourceKey = ResourceKey(loadSpec.method, loadSpec.url)
  
  def receive = { 
    case FailedRequest(l,e) => 
      //println(e)
      failedRequestsLastSecond(getKey(l)) = failedRequestsLastSecond(getKey(l)) + 1
    case SuccessfulRequest(l) =>
      successfulRequestsLastSecond(getKey(l)) = successfulRequestsLastSecond(getKey(l)) + 1
    case LoadResourceCreated(l) => 
      successfulRequestsLastSecond = successfulRequestsLastSecond + (getKey(l) -> 0)
      failedRequestsLastSecond = failedRequestsLastSecond + (getKey(l) -> 0)
    case LoadResourceDeleted(l) => 
      successfulRequestsLastSecond = successfulRequestsLastSecond - getKey(l)
      failedRequestsLastSecond = failedRequestsLastSecond - getKey(l)
    case AgggregateStatistcs => 
      context.system.scheduler.scheduleOnce(1000.millis,self, AgggregateStatistcs)
      successfulRequestsLastSecond.foreach(r => 
        println("Successful: " + r._1.method + " " + r._1.url + ": " + r._2)  
      )
      failedRequestsLastSecond.foreach(r => 
        println("Failed: " + r._1.method + " " + r._1.url + ": " + r._2)  
      )
      
      successfulRequestsLastSecond = successfulRequestsLastSecond.map( v => (v._1 -> 0))
      failedRequestsLastSecond = failedRequestsLastSecond.map( v => (v._1 -> 0))
    case u => 
      println("StatisticsActor received unknown message: " + u)
  }
  
}