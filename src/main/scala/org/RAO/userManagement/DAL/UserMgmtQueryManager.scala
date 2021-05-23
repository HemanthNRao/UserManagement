package org.RAO.userManagement.DAL

object UserMgmtQueryManager extends QueryManager
{
  def addUser(id:String, name:String, pass:String, phone: String, email:String)=
  {
    db.queyWithNoResult("INSERT INTO users (id, username, password, phone, email) values(?,?,?,?,?)", Array(id, name, pass, phone, email))
  }

  def checkUserExists(email:String, pass:String)=
  {
    db.queryWithSingleResult[Int]("SELECT count(*) FROM users WHERE username=? and password=?", Array(email, pass))
  }

  def createSession(id:String, aceess:Int, name:String)=
  {
    db.queyWithNoResult("INSERT INTO usersession (sessionId, access , userdata) VALUES (?,?,?)", Array(id, aceess, name))
  }

  def checkSession(id:String) = {
    db.queryWithSingleResult[Int]("select count(*) from usersession where sessionId=?", Array(id))
  }

  def deleteSession(id:String): Unit =
  {
    db.queyWithNoResult("delete from usersession where sessionId=?", Array(id))
  }

}
