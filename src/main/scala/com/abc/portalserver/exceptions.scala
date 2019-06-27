package com.abc.portalserver

import java.util.UUID

case class TodoNotFound(id: UUID)
    extends Exception(s"Todo with id '$id' not found")
