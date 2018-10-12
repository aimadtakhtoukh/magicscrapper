package app.gathererscrapping.card

import akka.util.Timeout
import akka.pattern.ask
import app.beans.Legality
import app.gathererscrapping.{Actors, MultiverseIdMessage}

import scala.concurrent.duration._
import scala.concurrent.Await

trait CardScrapping {

  def legalitiesFromMultiverseId(multiverseId : String): Array[Legality] = {
    implicit val timeout: Timeout = Timeout(1 minute)
    val future = Actors.legalityScrapper ? MultiverseIdMessage(multiverseId)
    Await.result(future, timeout.duration).asInstanceOf[Array[Legality]]
  }
}
