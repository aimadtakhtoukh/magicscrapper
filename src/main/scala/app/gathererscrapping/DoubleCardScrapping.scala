package app.gathererscrapping

import akka.actor.{Actor, ActorLogging}
import app.beans.{Card, CardFace, Ruling}
import app.gathererscrapping.GathererScrappingFunctions.extractDetails
import org.jsoup.nodes.Document

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class DoubleCardScrapping extends Actor with ActorLogging {
  override def receive: Receive = {
    case CardDetailsMessage(document, multiverseId) =>
      val (leftSideDetails, rightSideDetails) = detailsFromDocument(document)

      val (leftRulings, rightRulings) = rulingsFromDocument(document)

      val leftSide : CardFace = cardSideFromExtractedDetails(leftSideDetails, leftRulings)
      val rightSide : CardFace = cardSideFromExtractedDetails(rightSideDetails, rightRulings)

      val card = doubleCardFromExtractedDetails(
        multiverseId,
        commonDetails(leftSideDetails, rightSideDetails),
        Array(leftSide, rightSide)
      )
      log.info(card.toString)
      sender ! CardMessage(card)
  }

  def commonDetails(leftSide : Map[String, String], rightSide : Map[String, String]) : Map[String, String] = {
    Map(
      "expansion" -> getValueIfSame(leftSide("expansion"), rightSide("expansion")),
      "rarity" -> getValueIfSame(leftSide("rarity"), rightSide("rarity")),
      "artist" -> getValueIfSame(leftSide("artist"), rightSide("artist"))
    )
  }

  def getValueIfSame(left : String, right : String) : String = if (left equals right) left else s"$left||$right"

  def detailsFromDocument(document: Document): (Map[String, String], Map[String, String]) = {
    document
      .select("td.rightCol")
      .asScala
      .toList
      .take(2)
      .map(element => element
        .select("[id^=ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_]")
        .asScala
        .filter(_.children.asScala.exists(_.hasClass("label")))
        .filter(_.children.asScala.exists(_.hasClass("value")))
        .map(p => (
          p.select(".label").text.dropRight(1),
          p.select(".value"))
        )
        .filterNot(_._1.isEmpty)
        .flatMap(extractDetails)
        .toMap) match {
      case List(a, b) => (a, b)
    }
  }

  def rulingsFromDocument(document: Document): (Array[Ruling], Array[Ruling]) = {
    document
      .select("div.discussion")
      .asScala
      .toList
      .take(2)
      .map(_.select("table tr")
        .asScala
        .map(tr => Ruling(
          tr.select("td[id$=rulingDate]").text(),
          tr.select("td[id$=rulingText]").text()
        ))
        .toArray
      ) match {
      case List(a, b) => (a, b)
      case List(a) => (a, Array.empty[Ruling])
      case List() => (Array.empty[Ruling], Array.empty[Ruling])
    }
  }

  def cardSideFromExtractedDetails(details : Map[String, String], rulings : Array[Ruling]) : CardFace = {
    val (power, toughness) = details.get("pt")
      .map(_.split("/"))
      .filter(_.length == 2)
      .map(split => (split(0), split(1)))
      .getOrElse(("", ""))

    CardFace(
      numberInSet = details.getOrElse("number", "0"),
      name = details.getOrElse("name", ""),
      manaCost = details.getOrElse("manaCost", ""),
      convertedManaCost = details.getOrElse("convertedManaCost", "0"),
      types = details.getOrElse("types", ""),
      cardText = details.getOrElse("cardText", ""),
      flavorText = details.getOrElse("flavorText", ""),
      power = power,
      toughness = toughness,
      loyalty = details.getOrElse("loyalty", ""),
      rulings = rulings
    )
  }

  def doubleCardFromExtractedDetails(multiverseId : String, commonDetails : Map[String, String], cardFaces : Array[CardFace]) : Card = {
    Try(
      Card(
        multiverseId = multiverseId,
        name = cardFaces.map(_.name).mkString("||"),
        setName = commonDetails.getOrElse("expansion", ""),
        rarity = commonDetails.getOrElse("rarity", ""),
        artist = commonDetails.getOrElse("artist", ""),
        cardFaces = cardFaces
      )
    ) match {
      case Success(card) => card
      case Failure(_) =>
        log.error(s"Error with card $multiverseId, cardFaces : $cardFaces")
        Card()
    }
  }
}
