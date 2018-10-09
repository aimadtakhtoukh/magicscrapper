package app.gathererscrapping

import akka.actor.{Actor, ActorLogging}
import app.beans.Card
import app.gathererscrapping.GathererScrappingFunctions.getDocument
import com.google.gson.{Gson, GsonBuilder}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

import Constants.multiverseRegex

class SetToCard extends Actor with ActorLogging {
  var setSize = 0
  val gson: Gson = new GsonBuilder().setPrettyPrinting().create()
  var cardList = new ListBuffer[Card]()

  override def receive: Receive = {
    case SetUrlMessage(setUrl) =>
      val cards = cardListFromSet(setUrl)
      setSize = cards.length
      cards
        .map(mid => {log.info(mid); mid})
        .foreach(
          Actors.cardWorkerRouter ! MultiverseIdMessage(_)
        )
    case CardMessage(card) =>
      cardList += card
      log.info(s"Count : ${cardList.size} / $setSize")
      if (cardList.size == setSize) {
        val cards : List[String] = cardList.toList
          .sortBy(_.cardFaces(0).name)
          .zip(Stream from 1)
          .map { case (c, index) =>
            if (c.cardFaces(0).numberInSet.equals("0"))
              c.copy(cardFaces = Array(c.cardFaces(0).copy(numberInSet = s"$index")))
            else
              c
          }
          .sortBy(_.cardFaces(0).numberInSet.replaceAll("[a-zA-Z]", "").toInt)
          .map(gson.toJson)
        Actors.saveInFile ! SaveInFileMessage(card.setName, cards)
      }
  }

  def cardListFromSet(setUrl : String) : List[String] = {
    getDocument(setUrl)
      .select("table tr.cardItem td.name a")
      .asScala
      .map(_.attr("href"))
      .flatMap {
        case multiverseRegex(multiverseId) => Some(multiverseId)
        case _ => None
      }
      .toList
  }
}
