package org.RAO.userManagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import org.RAO.userManagement.routes.RESTAPIs
import org.slf4j.LoggerFactory

object APIServer extends App with RESTAPIs
{
  //  Set up logger
  val log = LoggerFactory.getLogger(this.getClass)
  if(args.length == 0)
  {
    println("Usage: API [local OR server] [/path/to/config/file]")
    println("Defaults:")
    println(" 1st arg: local")
    println(" 2nd arg: local-config.conf")
  }

  //  The following variables will be accessable from other classes
  val execType = if(args.length > 0) args(0) else "local"
  val configFile = if(args.length > 1) args(1) else s"$execType-config.conf"
  log.info(s"Using execType: $execType, config file: $configFile ... ")

  //  Set the global config object
  ConfigManager.setConfig(s"config/$configFile")

  //  Add configs from local config file
  val sb = new StringBuilder()
  if(ConfigManager.exists("akka.loglevel")) sb.append(s"akka.loglevel = ${ConfigManager.get("akka.loglevel")}\n")
  if(ConfigManager.exists("akka.log-config-on-start")) sb.append(s"akka.log-config-on-start = ${ConfigManager.get("akka.log-config-on-start")}\n")
  val extraConfig = ConfigFactory.parseString(sb.toString)

  implicit val system = ActorSystem("FundMgrAPI", ConfigFactory.load(extraConfig))
  implicit val executor = system.dispatcher
  val route=
  {
    userRoute
  }
  val bindingFuture = Http().bindAndHandle(route, ConfigManager.get("http.interface"), ConfigManager.get("http.port").toInt)
  log.info(s"Metadata server online at http://${ConfigManager.get("http.interface")}:${ConfigManager.get("http.port")}/")
}
