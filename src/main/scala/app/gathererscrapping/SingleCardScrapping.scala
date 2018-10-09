package app.gathererscrapping

import akka.actor.{Actor, ActorLogging}
import app.beans.{Card, CardFace, Ruling}
import app.gathererscrapping.GathererScrappingFunctions._
import com.google.gson.Gson
import org.jsoup.nodes.Document

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class SingleCardScrapping extends Actor with ActorLogging {
  override def receive: Receive = {
    case CardDetailsMessage(document, multiverseId) =>
      var result = Map[String, String]()
      result += ("multiverseId" -> multiverseId)
      result ++= singleCardDetailsFromDocument(document, multiverseId)
      val card = singleCardFromExtractedDetails(result)
      log.info(card.toString)
      sender ! CardMessage(card)
  }

  def singleCardDetailsFromDocument(document : Document, multiverseId: String) : Map[String, String] = {
    var result = Map[String, String]()
    val singleCardInfo = extractSingleCardInformationFromDocument(document)
    if (singleCardInfo.isEmpty) {
      log.error(s"Error getting card $multiverseId")
    }
    result ++= singleCardInfo
    result += extractSingleRulingsFromDocument(document)
    result
  }

  def extractSingleCardInformationFromDocument(document: Document) : Map[String, String] = {
    Try(
      document
        .select("td#ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_rightCol " +
          "div[id^=ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_]")
        .asScala
        .filter(_.children.asScala.exists(_.hasClass("label")))
        .filter(_.children.asScala.exists(_.hasClass("value")))
        .map(p => (
          p.select(".label").text.dropRight(1),
          p.select(".value"))
        )
        .filterNot(_._1.isEmpty)
        .flatMap(extractDetails)
        .toMap
    ) match {
      case Success(details) => details
      case Failure(_) => Map.empty
    }
  }

  def extractSingleRulingsFromDocument(document : Document) : (String, String) = {
    val gson = new Gson()
    Try(
      document
        .select("table.rulingsTable tr")
        .asScala
        .map(tr => Ruling(
          tr.select("td[id$=rulingDate]").text(),
          tr.select("td[id$=rulingText]").text()
        ))
        .toArray
    ) match {
      case Success(rulings) => "rulings" -> gson.toJson(rulings)
      case Failure(_) => "rulings" -> ""
    }
  }

  def singleCardFromExtractedDetails(details : Map[String, String]) : Card = {
    val (power, toughness) = details.get("pt")
      .map(_.split("/"))
      .filter(_.length == 2)
      .map(split => (split(0), split(1)))
      .getOrElse(("", ""))

    Try(
      Card(
        multiverseId = details.getOrElse("multiverseId", ""),
        name = details.getOrElse("name", ""),
        setName = details.getOrElse("expansion", ""),
        rarity = details.getOrElse("rarity", ""),
        artist = details.getOrElse("artist", ""),
        cardFaces = Array(
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
            rulings = details.get("rulings").map(r => new Gson().fromJson(r, classOf[Array[Ruling]])).map(_.toArray).getOrElse(Array.empty)
          )
        )
      )
    ) match {
      case Success(card) => card
      case Failure(_) =>
        log.error(s"Error with card ${details("multiverseId")}, data = $details")
        Card()
    }
  }
}
