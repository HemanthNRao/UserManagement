package org.RAO.userManagement.DAL

import resource.managed

import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait DBBackend
{
  def getConnection:Connection

  def queryWithSingleResult[T](query:String,params:Array[Any]=Array()):Option[T]=
  {
    val mresultset=for
    {
      conn<-managed(getConnection)
      pst<-managed(
        {
          val myst=conn.prepareStatement(query)
          params.zipWithIndex.foreach{ case(value,i)=>addParam(myst,i+1,value)}
          myst
        }
      );
      rs<-managed(pst.executeQuery())
    } yield rs
    val result=mresultset.acquireAndGet
    {
      rs=>if(rs.next()) Some(rs.getObject(1).asInstanceOf[T]) else None
    }
    result
  }

  def queryWithResult(query:String,params:Array[Any]=Array()):Seq[mutable.LinkedHashMap[String,Any]]={
    def getTypedValues(rs:ResultSet,index:Int)=
    {
      val stringTypes=Array("CHAR","TEXT","CLOB","EUM","SET","JSON","DATE","TIME")
      val byteArrayType=Array("BLOB","BINARY")
      val intType=Array("INT")
      val floatType=Array("FLOAT","DOUBLE")

      rs.getMetaData.getColumnTypeName(index) match {
        case x if (stringTypes.find(x.contains(_)).isDefined) => rs.getString(index)
        case x if (byteArrayType.find(x.contains(_)).isDefined) => rs.getByte(index)
        case x if (intType.find(x.contains(_)).isDefined) => rs.getInt(index)
        case x if (floatType.find(x.contains(_)).isDefined) => rs.getDouble(index)
        case x => throw new Exception(s"Not Implemented $x")
      }
    }
    val mresultset=for
    {
      conn<-managed(getConnection)
      pst<-managed(
        {
          val myst=conn.prepareStatement(query)
          params.zipWithIndex.foreach{ case(value,i)=>addParam(myst,i+1,value)}
          println(myst)
          myst
        }
      );
      rs<-managed(pst.executeQuery())
    } yield rs
    val result= mresultset.acquireAndGet
    {
      rs =>
      {
        val resultAccum = ArrayBuffer[mutable.LinkedHashMap[String,Any]]()
        val colCount=rs.getMetaData.getColumnCount
        while(rs.next())
        {
          val row = mutable.LinkedHashMap[String,Any]()
          for(i<-1 to colCount) row += (rs.getMetaData.getColumnName(i).toUpperCase -> getTypedValues(rs, i))
          resultAccum+=row
        }
        resultAccum
      }
    }
    result
  }


  def queyWithNoResult(query:String,params:Array[Any]=Array())=
  {
    for(
      conn <- managed(getConnection);
      pst <- managed(
        {
          val myst = conn.prepareStatement(query)
          params.zipWithIndex.foreach{case (value,i) => addParam(myst,i+1,value)}
          myst
        })

    )
    {
      pst.executeUpdate();
    }
  }

  def addParam(st: PreparedStatement, index: Int, param: Any)=
  {
    param match
    {
      case v:String => st.setString(index,v)
      case v:Int => st.setInt(index,v)
      case v:Float => st.setFloat(index,v)
      case v:Double => st.setDouble(index,v)
      case v:Boolean => st.setBoolean(index,v)
      case v:Array[Byte] => st.setBytes(index,v)
      case _ => throw new NotImplementedError(s"Parameter type for value $param is not supported")
    }
  }
}
