package org.RAO.userManagement.DAL

import org.RAO.userManagement.ConfigManager

trait QueryManager
{
  val dbType = ConfigManager.get("db")
  val db = dbType match
  {
    case "sqlite3" => SqliteBackend
  }

}
