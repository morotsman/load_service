package services

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.util.Timeout
import model._

object StatisticsActor {
  def props = Props[StatisticsActor]

  case class FailedRequest(loadSpec: LoadSpec, e: Throwable)
  case class SuccessfulRequest(loadSpec: LoadSpec)
  case class AgggregateStatistcs()

  case class LoadResourceCreated(loadSpec: LoadSpec)
  case class LoadResourceDeleted(loadSpec: LoadSpec)

  case class StatisticsEvent(loadResource: ResourceKey, numberOfRequests: Int, eventType: String)
}

class StatisticsActor extends Actor {
  import StatisticsActor._

  val tmp = (1, 2)

  var successfulRequestsLastSecond: scala.collection.mutable.Map[ResourceKey, Int] = scala.collection.mutable.Map()
  var failedRequestsLastSecond: scala.collection.mutable.Map[ResourceKey, List[String]] = scala.collection.mutable.Map()

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler.scheduleOnce(1000.millis, self, AgggregateStatistcs)
    context.system.eventStream.subscribe(context.self, classOf[FailedRequest])
    context.system.eventStream.subscribe(context.self, classOf[SuccessfulRequest])
    context.system.eventStream.subscribe(context.self, classOf[LoadResourceCreated])
    context.system.eventStream.subscribe(context.self, classOf[LoadResourceDeleted])
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(context.self)
  }

  println("StatisticsActor created")

  def getKey(loadSpec: LoadSpec): ResourceKey = ResourceKey(loadSpec.method, loadSpec.url)

  def aggregateFailure(errors: List[String]): Map[String, Int] =
    errors.groupBy(e => e).map(ge => (ge._1, ge._2.size))

  def receive = {
    case FailedRequest(l, e) =>
      failedRequestsLastSecond(getKey(l)) = e.getClass.toString :: failedRequestsLastSecond(getKey(l))
    case SuccessfulRequest(l) =>
      successfulRequestsLastSecond(getKey(l)) = successfulRequestsLastSecond(getKey(l)) + 1
    case LoadResourceCreated(l) =>
      successfulRequestsLastSecond = successfulRequestsLastSecond + (getKey(l) -> 0)
      failedRequestsLastSecond = failedRequestsLastSecond + (getKey(l) -> Nil)
    case LoadResourceDeleted(l) =>
      successfulRequestsLastSecond = successfulRequestsLastSecond - getKey(l)
      failedRequestsLastSecond = failedRequestsLastSecond - getKey(l)
    case AgggregateStatistcs =>
      context.system.scheduler.scheduleOnce(1000.millis, self, AgggregateStatistcs)
      successfulRequestsLastSecond.foreach(s => context.system.eventStream.publish(StatisticsEvent(s._1, s._2, "successful")))
      failedRequestsLastSecond.foreach(s => {
        context.system.eventStream.publish(StatisticsEvent(s._1, s._2.size, "failed"))
        val aggregatedFailures = aggregateFailure(s._2)
        aggregatedFailures.foreach(e => {
          context.system.eventStream.publish(StatisticsEvent(s._1, e._2, e._1))
        })
      })

      successfulRequestsLastSecond = successfulRequestsLastSecond.map(v => (v._1 -> 0))
      failedRequestsLastSecond = failedRequestsLastSecond.map(v => (v._1, Nil))
    case u =>
      println("StatisticsActor received unknown message: " + u)
  }

}