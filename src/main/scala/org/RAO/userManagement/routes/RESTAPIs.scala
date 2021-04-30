package org.RAO.userManagement.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{Multipart, StatusCodes}
import akka.http.scaladsl.model.Multipart.BodyPart
import akka.http.scaladsl.model.headers.{HttpCookie, RawHeader}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.RAO.userManagement.DAL.UserMgmtQueryManager
import org.RAO.userManagement.exceptions.MissingParams
import org.RAO.userManagement.utils.{Json, Utils}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

trait RESTAPIs extends APIRoutes
{
  implicit val system:ActorSystem
  val userRoute = pathPrefix("user") {
    (path("register") & post & entity(as[Multipart.FormData])) {
      formData => {
        val inputMapF = getFormDataToMap(formData)
        onSuccess(inputMapF) {
          inputMap => {
            val missingParam = Utils.required(inputMap, List("username", "password"))
            if (missingParam.isEmpty) {
              //              TODO: add exception for unique username registration
              val id = Utils.constructRandomKey(20)
              val name = inputMap("username").toString
              val pass = inputMap("password").toString
              val phone = inputMap("phone").toString
              val email = inputMap("email").toString
              //              TODO: insert password in encrypted form
              UserMgmtQueryManager.addUser(id, name, pass, phone, email)
              complete(s"User registered successfully with id $id")
            }
            else {
              throw new MissingParams(missingParam)
            }
          }
        }
      }
    } ~
    (path("login") & post & entity(as[Multipart.FormData])) {
      formData => {
        val inputMapF = getFormDataToMap(formData)
        onSuccess(inputMapF) {
          inputMap => {
            val missingParam = Utils.required(inputMap, List("username", "password"))
            if (missingParam.isEmpty) {
              val name = inputMap("username").toString
              val pass = inputMap("password").toString
              //                TODO: encrypt the password before sending to DB
              val res = UserMgmtQueryManager.checkUserExists(name, pass).getOrElse(0)
              if (res == 0)
                complete(StatusCodes.BadRequest, "Invalid username or password")
              else
              {
                val sessionId = Utils.constructRandomKey(20)
                saveSession(sessionId, name)
                setCookie(HttpCookie("session", value = sessionId, path = Option("/login"), httpOnly = true))
                {
                  val headers = List(RawHeader("session", sessionId))
                  respondWithHeaders(headers)
                  {
                    complete("login successfull")
                    //                    redirect("http://localhost:8080/user/register", StatusCodes.TemporaryRedirect)
                  }
                }
              }
            }
            else
              throw new MissingParams(missingParam)
          }
        }
      }
    } ~
    (path("getLogin" / Segment) & get)
    {
      sessionId =>
        val res =UserMgmtQueryManager.checkSession(sessionId).getOrElse(0)
        println("sessionId",sessionId)
        if(res ==1)
          complete(Json.Value(Map("sessionId"->sessionId)).write)
        else
          complete(Json.Value(Map("sessionId"->"")).write)
    }
  }
  private def saveSession(id:String, name: String)=
  {
    println(System.currentTimeMillis.toInt)
    UserMgmtQueryManager.createSession(id, System.currentTimeMillis.toInt, name)
  }
  private def getFormDataToMap(formData: Multipart.FormData): Future[Map[String, Any]] =
  {
    // Method to extract byte array from multipart formdata.
    def getBytesFromFilePart(dataBytes: Source[ByteString, Any]) =
    {
      dataBytes.runFold(ArrayBuffer[Byte]())
      { case (accum, value) => accum ++= value.toArray }
    }
    // Process each form data and store it in a Map[String,Array[Byte]]
    formData.parts.mapAsync(1)
    {
      // Case to extract file or schema input
      // Looks like there might be issues in using the method below resulting in buffer overflow
      // See this thread: https://github.com/akka/akka-http/issues/285
      case b: BodyPart if b.name == "file" || b.name == "schema" => getBytesFromFilePart(b.entity.dataBytes).map(bytes => b.name -> bytes.toArray)
      // Case to extract the rest of the POST parameters
      // Not sure why we are using toStrict() below.
      // Reference: https://github.com/knoldus/akka-http-multipart-form-data.g8/blob/master/src/main/g8/src/main/scala/com/knoldus/MultipartFormDataHandler.scala
      case b: BodyPart => b.toStrict(2.seconds).map(strict => b.name -> strict.entity.data.utf8String.getBytes())
    }
      .runFold(mutable.HashMap[String, Any]())
      { case (map, (keyName, keyVal)) =>
        val value = if (keyName != "file") new String(keyVal)
        else keyVal
        map += (keyName -> value)
      }
      .map(_.toMap)
  }
}
