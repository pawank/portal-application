package com.abc.portalserver

import java.util.UUID

import io.scalaland.chimney.dsl._
import java.time.OffsetDateTime

case class ID(value: String) extends AnyVal

sealed trait GraphID {
  def id: Option[ID]
  def label: String
}

case class Employee(
    id: Option[ID],
    label: String = "Employee",
    name: String,
    email: String,
    mobile: Option[String],
    skills: List[String]
) extends GraphID

object Employee {
  val superadmin =
    Employee(
      id = None,
      name = "Super Admin",
      email = "superadmin@gmail.com",
      mobile = None,
      skills = List("Scala", "Python")
    )
}

case class DoneByAt(
    createdOn: OffsetDateTime = OffsetDateTime.now(),
    updatedOn: Option[OffsetDateTime] = None,
    deletedOn: Option[OffsetDateTime] = None,
    createdBy: String,
    updatedBy: Option[String] = None,
    deletedBy: Option[String] = None
)

object DoneByAt {
  val samples = List(
    DoneByAt(createdOn = OffsetDateTime.now, createdBy = "superadmin@gmail.com")
  )
}

case class TodoItem(id: UUID, title: String, completed: Boolean, order: Int)

case class Region(
    name: String,
    latitude: Option[Double],
    longitude: Option[Double],
    isoCode: Option[String]
)

object Region {
  val samples = {
    val r = Region(
      name = "Bengaluru North",
      latitude = None,
      longitude = None,
      isoCode = None
    )
    List(r, r.copy(name = "Jakkur"), r.copy(name = "Koramangla"))
  }
}

case class City(
    name: String,
    latitude: Option[Double],
    longitude: Option[Double],
    isoCode: Option[String]
)

object City {
  val samples = {
    Region.samples.map(r => {
      r.transformInto[City]
    })
  }
}

case class Country(
    name: String,
    isoCode: Option[String]
)

case class Candidate(
    id: Option[ID],
    label: String = "Candidate",
    name: String,
    email: String,
    mobile: String,
    dob: java.util.Date,
    location: String,
    city: String,
    country: String
) extends GraphID

object Candidate {
  val samples = List(
    Candidate(
      id = None,
      name = "P K",
      email = "pk@pk.com",
      mobile = "98908900922",
      dob = new java.util.Date(),
      location = "Bengaluru",
      city = "Bengaluru",
      country = "India"
    )
  )
}

case class Company(
    id: Option[ID],
    label: String = "Company",
    name: String,
    industry: String,
    noOfEmployees: Int,
    companyType: String,
    location: String,
    city: String,
    country: String
) extends GraphID

case class Resume(title: String, cvType: String)

case class JD(
    title: String,
    cvType: String,
    position: String,
    role: String,
    location: Option[String]
)

case class Job(
    id: Option[ID],
    label: String = "Job",
    title: String,
    applyLink: String,
    description: String,
    skills: List[String],
    goodToHave: List[String]
) extends GraphID
