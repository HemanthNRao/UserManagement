package org.RAO.userManagement.DAL

import org.RAO.userManagement.ConfigManager

import java.sql.DriverManager

object SqliteBackend extends DBBackend
{
  Class.forName(ConfigManager.get("sqlite3.driver"))
  var url=ConfigManager.get("sqlite3.url")
  var user=ConfigManager.get("sqlite3.user")
  var pass=ConfigManager.get("sqlite3.pass")
  private def dbConn=DriverManager.getConnection(url,user,pass)
  override def getConnection = dbConn

  //Execute the block for setting up the tables
  {
    val createUsers ="""CREATE TABLE IF NOT EXISTS users (id varchar(40) PRIMARY KEY, username varchar(70) UNIQUE, password varchar(70) NOT NULL, phone varchar(12), email varchar(40));"""
    val dropFunds = """drop table if exists users"""
    val createUserSessions = """CREATE TABLE IF NOT EXISTS usersession (sessionId varchar(50) PRIMARY KEY, access int(10), userdata text);"""
    val dropTransactions = """drop table if exists usersession"""
    queyWithNoResult(createUsers)
    queyWithNoResult(createUserSessions)
  }
}
