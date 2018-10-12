package app.gathererscrapping.card.singlecard

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import app.beans._
import app.gathererscrapping.GathererScrappingFunctions._
import app.gathererscrapping._
import app.gathererscrapping.card.CardScrapping
import com.google.gson.Gson

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class SingleCardScrapping extends Actor with ActorLogging with CardScrapping with SingleCardDetailsScrapping {
  override def receive: Receive = {
    case CardDetailsMessage(document, multiverseId) =>
      var result = Map[String, String]()
      result += ("multiverseId" -> multiverseId)

      val languageUrl = buildLanguageUrl(multiverseId)

      val languagesToMultiverseId = (("English", multiverseId) :: otherLanguagesFromUrl(languageUrl))

      val languages = languagesToMultiverseId
        .map(languageInfoToForeignCard)
        .toArray

      singleCardDetailsFromDocument(document, multiverseId) match {
        case Success(detail) => result ++= detail
        case Failure(exception) => log.error(exception.getMessage)
      }

      val legalities = legalitiesFromMultiverseId(multiverseId)

      val card = singleCardFromExtractedDetails(result, languages, legalities)
      log.info(card.toString)
      sender ! CardMessage(card)
  }

  def singleCardFromExtractedDetails(details : Map[String, String],
                                     languages : Array[Language],
                                     legalities : Array[Legality]) : Card = {
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
        legalities = legalities,
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
            rulings = details.get("rulings").map(r => new Gson().fromJson(r, classOf[Array[Ruling]])).map(_.toArray).getOrElse(Array.empty),
            languages = languages
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

  def languageInfoToForeignCard(languageInfo : (String, String)): Language = {
    implicit val timeout: Timeout = Timeout(30 seconds)
    val future = Actors.languageSingleCardScrapping ? LanguageCardMessage(languageInfo._1, languageInfo._2)
    Await.result(future, timeout.duration).asInstanceOf[Language]
  }
}
