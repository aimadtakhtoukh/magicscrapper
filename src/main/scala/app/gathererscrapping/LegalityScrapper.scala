package app.gathererscrapping

import scala.collection.JavaConverters._
import akka.actor.Actor
import app.beans.Legality
import app.gathererscrapping.GathererScrappingFunctions._
import app.gathererscrapping.Constants._

class LegalityScrapper extends Actor {
  override def receive: Receive = {
    case MultiverseIdMessage(multiverseId) =>
      val url = buildLegalityUrl(multiverseId)
      sender ! legalities(url)
  }

  def buildLegalityUrl(multiverseId : String) =
    s"${gathererPageUrl}Card/Printings.aspx?multiverseid=$multiverseId"

  def legalities(url : String): Array[Legality] = {
    getDocument(url)
      .select("table.cardList")
      .asScala
      .filter(e => e.select("tr.headerRow").text().contains("Legality"))
      .flatMap(e => e.select("tr.cardItem").asScala)
      .map(e =>
        Legality(
          e.select("td.column1").text(),
          e.select("td[style]").text()
        )
      )
      .filterNot(e => e.format.isEmpty && e.legality.isEmpty)
      .toArray
  }
}
