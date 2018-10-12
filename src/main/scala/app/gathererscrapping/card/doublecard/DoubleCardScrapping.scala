package app.gathererscrapping.card.doublecard

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import app.beans._
import app.gathererscrapping.GathererScrappingFunctions._
import app.gathererscrapping._
import app.gathererscrapping.card.CardScrapping
import org.jsoup.nodes.Document

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class DoubleCardScrapping extends Actor with ActorLogging with CardScrapping with DoubleCardDetailsScrapping {
  override def receive: Receive = {
    case CardDetailsMessage(document, multiverseId) =>
      val (leftSideDetails, rightSideDetails) = detailsFromDocument(document)

      val (leftRulings, rightRulings) = rulingsFromDocument(document)

      val languageUrl = buildLanguageUrl(multiverseId)
      val languagesToMultiverseId = ("English", multiverseId) :: otherLanguagesFromUrl(languageUrl)

      val (leftSideLanguage, rightSideLanguage) = languagesToMultiverseId
        .map(languageInfoToForeignCard)
        .toArray
        .unzip

      val leftSide : CardFace = cardSideFromExtractedDetails(leftSideDetails, leftRulings, leftSideLanguage)
      val rightSide : CardFace = cardSideFromExtractedDetails(rightSideDetails, rightRulings, rightSideLanguage)

      val legalities = legalitiesFromMultiverseId(multiverseId)

      val card = doubleCardFromExtractedDetails(
        multiverseId,
        legalities,
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
  def doubleCardFromExtractedDetails(multiverseId : String,
                                     legalities : Array[Legality],
                                     commonDetails : Map[String, String],
                                     cardFaces : Array[CardFace]) : Card = {
    Try(
      Card(
        multiverseId = multiverseId,
        name = cardFaces.map(_.name).mkString("||"),
        setName = commonDetails.getOrElse("expansion", ""),
        rarity = commonDetails.getOrElse("rarity", ""),
        artist = commonDetails.getOrElse("artist", ""),
        legalities = legalities,
        cardFaces = cardFaces
      )
    ) match {
      case Success(card) => card
      case Failure(_) =>
        log.error(s"Error with card $multiverseId, cardFaces : $cardFaces")
        Card()
    }
  }

  def languageInfoToForeignCard(languageInfo : (String, String)): (Language, Language) = {
    implicit val timeout: Timeout = Timeout(30 seconds)
    val future = Actors.languageDoubleCardScrapping ? LanguageCardMessage(languageInfo._1, languageInfo._2)
    Await.result(future, timeout.duration).asInstanceOf[(Language, Language)]
  }
}
