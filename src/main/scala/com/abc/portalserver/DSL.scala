package com.abc.portalserver.dsl

import com.abc.portalserver._
import gremlin.scala._
import java.time.OffsetDateTime

object DSL {
  def testing = {
    import com.orientechnologies.orient.core.metadata.schema.OType
    import com.orientechnologies.orient.core.sql.executor.OResult
    import com.orientechnologies.orient.core.sql.executor.OResultSet
    import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
    import gremlin.scala._
    import java.util.{ArrayList => JArrayList}
    import org.apache.commons.configuration.BaseConfiguration
    import org.apache.tinkerpop.gremlin.orientdb._
    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._

    implicit val graph =
      new OrientGraphFactory(
        "remote:localhost/portalapp",
        "root",
        "[guessme321]"
      ).getNoTx().asScala()

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

    //val candidate = graph + Candidate.samples.head
    //candidate.toCC[Candidate]

    val sa: Vertex = graph + Employee.superadmin
    val e1 = sa.toCC[Employee]
    println(e1)

    val e2: Vertex = graph + Employee.superadmin
      .copy(name = "Pawan", email = "pawan@gmail.com")
    println(e2)

    val done = DoneByAt.samples.head
    /*
    val e = sa.addEdge(
      "At",
      e2,
      done.getClass.getDeclaredFields
        .map(_.getName)
        .zip(done.productIterator.to)
        .toMap
    )
     */
    val Name = Key[String]("name")
    //https://medium.com/rahasak/scala-case-class-to-map-32c8ec6de28a
    //http://blog.echo.sh/2013/11/04/exploring-scala-macros-map-to-case-class-conversion.html
    val doneMap = done.getClass.getDeclaredFields
      .map(_.getName)
      .zip(done.productIterator.to)
      .toMap
    println(doneMap)
    val doneMapWithoutNone =
      for ((k, Some(v)) <- done.getClass.getDeclaredFields
             .map(_.getName)
             .zip(done.productIterator.to)
             .toMap) yield k -> v.asInstanceOf[String]
    println(doneMapWithoutNone)
    val e = sa --- ("At", marshaller.fromCC(done).properties.toMap) --> e2
    //val e = sa --- ("At", Name -> "b") --> e2
    println(e)
    //sa.addEdge("ByAt", w2, DoneByAt.samples.head)
    //graph + DoneByAt.samples.head.copy(updatedOn = Some(OffsetDateTime.now()))

  }
}
