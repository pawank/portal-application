package com.abc.portalserver.dsl

import com.abc.portalserver._
import gremlin.scala._
import java.time.OffsetDateTime
import com.orientechnologies.orient.core.id.ORecordId

case class StorageCredential(url: String, username: String, password: String)

trait VertexManager[T] {
  def add(vertex: T)(implicit graph: ScalaGraph): (gremlin.scala.Vertex, T)
}

class EmployeeVertexManager extends VertexManager[Employee] {
  def add(
      vertex: Employee
  )(implicit graph: ScalaGraph): (gremlin.scala.Vertex, Employee) = {
    val sa: Vertex = graph + vertex
    val e = sa
      .toCC[Employee]
      .copy(id = Some(ID(sa.id.asInstanceOf[ORecordId].toString())))
    println(e)
    (sa, e)
  }
}

trait IGraph[T <: GraphID] {
  import com.orientechnologies.orient.core.metadata.schema.OType
  import com.orientechnologies.orient.core.sql.executor.OResult
  import com.orientechnologies.orient.core.sql.executor.OResultSet
  import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
  import gremlin.scala._
  import java.util.{ArrayList => JArrayList}
  import java.util.{
    Comparator,
    List => JList,
    Map => JMap,
    Collection => JCollection,
    Iterator => JIterator,
    Set => JSet
  }
  import org.apache.commons.configuration.BaseConfiguration
  import org.apache.tinkerpop.gremlin.orientdb._
  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  val graphAsJava =
    new OrientGraphFactory(
      "remote:localhost/portalapp",
      "root",
      "[guessme321]"
    ).getNoTx()

  implicit val graph = graphAsJava.asScala()

  implicit val marshaller = new Marshallable[DoneByAt] {
    import java.time.format.DateTimeFormatter
    import gremlin.scala.PropertyOps
    def fromCC(cc: DoneByAt) =
      FromCC(
        None,
        "DoneByAt",
        List(
          "createdOn" -> cc.createdOn
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
          "updatedOn" -> cc.updatedOn
            .map(x => x.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .getOrElse(""),
          "deletedOn" -> cc.deletedOn
            .map(x => x.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .getOrElse(""),
          "createdBy" -> cc.createdBy,
          "updatedBy" -> cc.updatedBy.getOrElse(""),
          "deletedBy" -> cc.deletedBy.getOrElse("")
        )
      )

    def toCC(element: Element): DoneByAt =
      DoneByAt(
        createdOn = OffsetDateTime.parse(element.value[String]("createdOn")),
        updatedOn = element.value[String]("updatedOn") match {
          case a if !a.isEmpty => Some(OffsetDateTime.parse(a))
          case _               => None
        },
        deletedOn = element.value[String]("deletedOn") match {
          case a if !a.isEmpty => Some(OffsetDateTime.parse(a))
          case _               => None
        },
        createdBy = element.value[String]("createdBy"),
        updatedBy = element.value[String]("updatedBy") match {
          case a if !a.isEmpty() => Some(a)
          case _                 => None
        },
        deletedBy = element.value[String]("deletedBy") match {
          case a if !a.isEmpty() => Some(a)
          case _                 => None
        }
      )
  }

  def createUniqueIndex(
      key: String,
      label: String,
      keyType: Option[OType] = None
  ): Either[String, Boolean] = {
    import org.apache.commons.configuration.BaseConfiguration
    import org.apache.tinkerpop.gremlin.orientdb._
    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._
    try {

      val keyEmployeeIndex = Key[String](key)
      val config = new BaseConfiguration()
      config.setProperty("type", "UNIQUE")
      config.setProperty("keytype", keyType.getOrElse(OType.STRING))
      val indexes = graphAsJava.getIndexedKeys(keyEmployeeIndex.name)
      if (indexes.isEmpty()) {
        graphAsJava.createVertexIndex(
          keyEmployeeIndex.name,
          label,
          config
        )
        Right(true)
      } else {
        println(s"Indexes found = ${indexes} for key = ${keyEmployeeIndex}")
        Right(true)
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        Left(e.getStackTraceString)
    }
  }

  def add[T](
      vertex: T
  )(implicit vertexManager: VertexManager[T]): (gremlin.scala.Vertex, T) = {
    vertexManager.add(vertex)
  }

}

object EmployeeSDL extends IGraph[Employee] {
  def testing = {
    import com.orientechnologies.orient.core.metadata.schema.OType
    import com.orientechnologies.orient.core.sql.executor.OResult
    import com.orientechnologies.orient.core.sql.executor.OResultSet
    import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
    import gremlin.scala._
    import java.util.{ArrayList => JArrayList}
    import java.util.{
      Comparator,
      List => JList,
      Map => JMap,
      Collection => JCollection,
      Iterator => JIterator,
      Set => JSet
    }

    //createUniqueIndex( "email", Employee.getClass().getSimpleName().reverse.tail.reverse)
    val sa = Employee.superadmin
    createUniqueIndex("email", sa.label)

    implicit val employeeVertex = new EmployeeVertexManager

    val done = DoneByAt.samples.head

    val (v1, e1) = add(sa)
    println(e1)
    val (v2, e2) = add(
      Employee.superadmin.copy(name = "Pawan", email = "pawan@gmail.com")
    )
    val e = v1 --- ("At", marshaller.fromCC(done).properties.toMap) --> v2
    //val e = sa --- ("At", Name -> "b") --> e2
    println(e)
    println(e.id().asInstanceOf[ORecordId])
  }
}
