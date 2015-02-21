package com.xebialabs.jello.domain

import java.util.UUID

import com.typesafe.config.ConfigFactory
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.{Board, Column, _}
import com.xebialabs.jello.domain.json.TrelloProtocol
import com.xebialabs.jello.http.RequestExecutor
import spray.httpx.SprayJsonSupport._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Trello {

  val conf = ConfigFactory.load()

  val columns: Seq[Int] = conf.getIntList("trello.lists").map(_.toInt)

  case class Column(id: String, name: String)

  case class Board(id: String, shortUrl: String, lists: Seq[Column] = Seq()) extends RequestExecutor with TrelloProtocol {

    def putTickets(tickets: Seq[Ticket]): Future[Seq[NewCardResp]] = {
      Future.successful(
        tickets.map(t => Await.result(
          runRequest[NewCardResp](NewCard(name = s"${t.id} ${t.title}", idList = lists.head.id)),
          1 second
        ))
      )
    }

  }

}

class Trello extends RequestExecutor with TrelloProtocol {

  def createBoard(): Future[Board] = {

    def createColumn(board: String, column: Int): Future[Column] = {
      runRequest[NewColumnResp](
        (board,ColumnReq(column.toString, columns.indexOf(column) * 10 + 10))
      )
    }

    val boardFuture: Future[Board] = runRequest[BoardResp](NewBoardReq(UUID.randomUUID().toString))

    boardFuture.map {
      case b: Board =>
        val createdColumns = columns.map(c => {
          // Blocking because of bug in Trello :-(
          Await.result(createColumn(b.id, c), 10 second)
        })
        b.copy(lists = createdColumns)
    }


  }

  def archiveBoard(id: String) = runRequest[BoardResp](id, CloseBoardReq())



  def getTickets(boardId: String): Future[Seq[Ticket]] = {
    runRequest[Seq[ListsItemResp]](ListsReq(boardId)).map(_.flatten)
  }

}
