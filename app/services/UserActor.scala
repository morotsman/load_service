package services

import javax.inject._

import akka.actor._
import akka.event.LoggingReceive
import com.google.inject.assistedinject.Assisted
import play.api.Configuration
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json._
import services.StatisticsActor._

import model.ResourceKey

class UserActor @Inject()(@Assisted out: ActorRef,
                          configuration: Configuration) extends Actor with ActorLogging {


  override def preStart(): Unit = {
    super.preStart()
    context.system.eventStream.subscribe(context.self, classOf[StatisticsEvent])
    context.system.eventStream.subscribe(context.self, classOf[StatisticEvents])
  }
  
  override def postStop(): Unit = {
    super.preStart()
    context.system.eventStream.unsubscribe(context.self) 
  }

  var observedMocks : Set[ResourceKey]= Set() 
   

  override def receive: Receive = LoggingReceive {

    case json: JsValue =>
      val action = (json \ "action").as[String]
      val resource = (json \ "resource").as[ResourceKey]    
      if(action == "watch") {
        println("UserActor: watch");
        observedMocks = observedMocks + resource
        context.system.eventStream.publish(HistoricData(resource,self));
      } else if(action == "unWatch"){
        println("UserActor: unwatch");
        observedMocks = observedMocks - resource
      }      
    case event@StatisticsEvent(resource,numberOfRequests, eventType, t) =>
      if(observedMocks.contains(resource)) {
         val statisticsEvent = Json.obj("type" -> "statisticsEvent", "resource" -> resource, "numberOfRequestsPerSecond" -> numberOfRequests, "eventType" -> eventType, "avargeLatancyInMillis" -> t)
         out ! statisticsEvent
      }
    case StatisticEvents(events) =>
      events.reverse.foreach { e => 
        out ! Json.obj("type" -> "statisticsEvent", "resource" -> e.loadResource, "numberOfRequestsPerSecond" -> e.numberOfRequests, "eventType" -> e.eventType, "avargeLatancyInMillis" -> e.avargeTimeInMillis)
      }
     
    case unknown@_ => 
      println("Unknown message received by UserActor: " + unknown)
  }
}

class UserParentActor @Inject()(childFactory: UserActor.Factory) extends Actor with InjectedActorSupport with ActorLogging {
  import UserParentActor._

  override def receive: Receive = LoggingReceive {
    case Create(id, out) =>
      println("Creating user actor!!!!!");
      val child: ActorRef = injectedChild(childFactory(out), s"userActor-$id")
      sender() ! child
  }
}

object UserParentActor {
  case class Create(id: String, out: ActorRef)
}

object UserActor {
  trait Factory {
    // Corresponds to the @Assisted parameters defined in the constructor
    def apply(out: ActorRef): Actor
  }
}
