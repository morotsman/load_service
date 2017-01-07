package services

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.util.Timeout
import model._

object StatisticsActor {
  def props = Props[StatisticsActor]

  case class FailedRequest(loadSpec: ResourceKey, e: Throwable, timeInMillis: Long)
  case class SuccessfulRequest(loadSpec: ResourceKey,timeInMillis: Long)
  case class AgggregateStatistcs()

  case class LoadResourceCreated(loadSpec: ResourceKey)
  case class LoadResourceDeleted(loadSpec: ResourceKey)
  case class HistoricData(resource: ResourceKey, actor: ActorRef)
  

  case class StatisticsEvent(loadResource: ResourceKey, numberOfRequests: Int, eventType: String, avargeTimeInMillis: Long)
  case class StatisticEvents(events: List[StatisticsEvent])
}

class StatisticsActor extends Actor {  
  import StatisticsActor._

  var successfulRequestsLastSecond: scala.collection.mutable.Map[ResourceKey, List[Long]] = scala.collection.mutable.Map()
  var failedRequestsLastSecond: scala.collection.mutable.Map[ResourceKey, List[String]] = scala.collection.mutable.Map()
  
  var historicData : Map[(ResourceKey,String), List[StatisticsEvent]] = Map() 

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler.scheduleOnce(1000.millis, self, AgggregateStatistcs)
    context.system.eventStream.subscribe(context.self, classOf[FailedRequest])
    context.system.eventStream.subscribe(context.self, classOf[SuccessfulRequest])
    context.system.eventStream.subscribe(context.self, classOf[LoadResourceCreated])
    context.system.eventStream.subscribe(context.self, classOf[LoadResourceDeleted])
    context.system.eventStream.subscribe(context.self, classOf[HistoricData])
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(context.self)
  }

  println("StatisticsActor created")
  
  val maxNumberOfEvents = 300;
  
  def addHistoricData(r: ResourceKey, event: StatisticsEvent): Unit = {
    val historicEvents = historicData.getOrElse((r,event.eventType), Nil)
    val events = if(historicEvents.size > maxNumberOfEvents) {
      event::historicEvents.dropRight(100)
    } else {
      event::historicEvents
    }
    historicData = historicData + ((r,event.eventType) -> events) 
  }


  def aggregateFailure(errors: List[String]): Map[String, Int] =
    errors.groupBy(e => e).map(ge => (ge._1, ge._2.size))
    
  def avarege(l : List[Long]): Option[Long] = 
    if(l.size == 0) None
    else Some(l.sum/l.size)

  def receive = {
    case FailedRequest(r, e, t) => 
      failedRequestsLastSecond.get(r) foreach { f => 
        failedRequestsLastSecond(r) = e.getClass.toString :: f
      }  
    case SuccessfulRequest(r, t) =>
      successfulRequestsLastSecond.get(r) foreach { s => 
        successfulRequestsLastSecond(r) = t::s 
      }
    case LoadResourceCreated(r) =>
      successfulRequestsLastSecond = successfulRequestsLastSecond + (r -> Nil)
      failedRequestsLastSecond = failedRequestsLastSecond + (r -> Nil)
    case LoadResourceDeleted(r) =>
      successfulRequestsLastSecond = successfulRequestsLastSecond - r
      failedRequestsLastSecond = failedRequestsLastSecond - r
    case AgggregateStatistcs =>
      context.system.scheduler.scheduleOnce(1000.millis, self, AgggregateStatistcs)
      successfulRequestsLastSecond.foreach(s => {
        val event = StatisticsEvent(s._1, s._2.size, "successful", avarege(s._2).getOrElse(0))
        addHistoricData(s._1, event)
        context.system.eventStream.publish(event)
      })
      failedRequestsLastSecond.foreach(s => {  
        val totalFailures = StatisticsEvent(s._1, s._2.size, "failed", 0)
        context.system.eventStream.publish(totalFailures)
        addHistoricData(s._1, totalFailures)
        val aggregatedFailures = aggregateFailure(s._2)
        aggregatedFailures.foreach(e => {
          val event = StatisticsEvent(s._1, e._2, e._1, 0)
          context.system.eventStream.publish(event)
        })
      })

      successfulRequestsLastSecond = successfulRequestsLastSecond.map(v => (v._1 -> Nil))
      failedRequestsLastSecond = failedRequestsLastSecond.map(v => (v._1, Nil))
    case HistoricData(resource,a) =>
      historicData.filter(r => r._1._1 == resource).foreach(kv =>
        a ! StatisticEvents(kv._2) 
      )
    case u =>
      println("StatisticsActor received unknown message: " + u)
  }

}