package com.abc.portalserver

import java.util.UUID

case class TodoItem(id: UUID, title: String, completed: Boolean, order: Int)
