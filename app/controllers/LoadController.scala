package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import akka.actor.ActorSystem
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration._

import services.LoadActor._
import services.LoadActor
import services.LoadManagerActor._
import services.LoadManagerActor
import model.LoadSpec

import akka.util.Timeout
import play.api.libs.json._
import akka.pattern.ask

import play.api.libs.ws._

/*
 * http://localhost:9000/load-resource/test
 * 
 * {"url": "http://localhost:9001/mock-service/test", "numberOfRequestPerSecond":1000}
 */

@Singleton
class LoadController @Inject() (actorSystem: ActorSystem,ws: WSClient)(implicit exec: ExecutionContext) extends Controller {

  val loadManagerActor = actorSystem.actorOf(LoadManagerActor.props(ws), "load-manager-actor")
  implicit val timeout: Timeout = 1.seconds

  def listLoadResources = Action.async {
    (loadManagerActor ? ListLoadResources).mapTo[Set[String]].map { msg => Ok(Json.toJson(msg)) }
  }
  
  def getLoadResource(name: String) = Action.async {
    (loadManagerActor ? GetLoadResource(name)).mapTo[Option[LoadSpec]].map { 
      case Some(msg) => Ok(Json.toJson(msg)) 
      case None => NotFound
    }
  }
  
  def createLoadResource() = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[LoadSpec].map {
      loadSpec => (loadManagerActor ? CreateLoadReource(loadSpec)).mapTo[LoadSpec].map { msg => Ok(Json.toJson(msg)) }
    }.recoverTotal {
      errors => Future.successful(BadRequest("Bad request: " + JsError.toFlatJson(errors)))
    }
  }
  
  
  def updateLoadResource(name: String) = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[LoadSpec].map {
      loadSpec => (loadManagerActor ? UpdateLoadResource(name,loadSpec)).mapTo[LoadSpec].map { msg => Ok(Json.toJson(msg)) }
    }.recoverTotal {
      errors => Future.successful(BadRequest("Bad request: " + JsError.toFlatJson(errors)))
    }
  }

def deleteLoadResource(name: String) = Action.async {
    (loadManagerActor ? DeleteLoadResource(name)).map(msg => Ok)
  }

def listLoadSessions = Action.async {
    (loadManagerActor ? ListLoadSessions).mapTo[Set[String]].map { msg => Ok(Json.toJson(msg)) }
  }

def updateLoadSessions(name: String) = Action.async {
    println("Start load session")
    (loadManagerActor ? StartLoadSession(name)).map { msg => Ok }
  }

def deleteLoadSessions(name: String) = Action.async {
  println("Stop load session")
    (loadManagerActor ? EndLoadSession(name)).map { msg => Ok }
  } 
  

}
