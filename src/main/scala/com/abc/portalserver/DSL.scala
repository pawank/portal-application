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

    val graph =
      new OrientGraphFactory(
        "remote:localhost/portalapp",
        "root",
        "[guessme321]"
      ).getNoTx().asScala()

    //val candidate = graph + Candidate.samples.head
    //candidate.toCC[Candidate]

    //val sa = graph + Employee.superadmin
    //sa.toCC[Employee]

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

    val v = graph + DoneByAt.samples.head
    v.toCC[DoneByAt](marshaller)
    graph + DoneByAt.samples.head.copy(updatedOn = Some(OffsetDateTime.now()))

  }
}
