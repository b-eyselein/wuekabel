package model

import better.files._
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy
import org.apache.poi.ss.usermodel.{CellType, Row}
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object Importer {

  private val stdFile: File = file"conf/vokabeln.xlsx"

  private val cardTypeCellIndex: Int = 0
  private val questionCellIndex: Int = 1
  private val meaningCellIndex : Int = 2

  private def cardTypeFromString(cardTypeStr: String): Either[String, CardType] = cardTypeStr match {
    case "Wort" => Right(CardType.Vocable)
    case "Text" => Right(CardType.Text)
    case "SC"   => Right(CardType.SingleChoice)
    case "MC"   => Right(CardType.MultipleChoice)
    case other  => Left(other)
  }

  private def partitionEitherSeq[T, U](a: Seq[Either[T, U]]): (Seq[T], Seq[U]) =
    a.foldLeft[(Seq[T], Seq[U])]((Seq[T](), Seq[U]())) { (b, a) =>
      a match {
        case Left(t)  => (b._1 :+ t, b._2);
        case Right(u) => (b._1, b._2 :+ u)
      }
    }

  def importFlashcards(langId: Int, collId: Int, file: File = stdFile): (Seq[String], Seq[CompleteFlashcard]) = {
    val workbook = new XSSFWorkbook(file.path.toAbsolutePath.toFile)

    val sheet = workbook.getSheetAt(workbook.getActiveSheetIndex)

    val firstRowWithoutHeaderInex = sheet.getFirstRowNum + 1

    // Ignore header row
    val readFlashcards = (firstRowWithoutHeaderInex to sheet.getLastRowNum) flatMap { rowIndex =>
      Option(sheet.getRow(rowIndex)) match {
        case None      => None
        case Some(row) => Some(readRow(row, langId, collId))
      }
    }

    workbook.close()

    partitionEitherSeq(readFlashcards)
  }

  private def readChoiceRow(row: Row, langId: Int, collId: Int, cardType: CardType, question: String): Either[String, CompleteFlashcard] = {

    val cardId = row.getRowNum

    val lastCellNum = row.getLastCellNum
    val maxCellIndex = if (lastCellNum % 2 == 1) lastCellNum + 1 else lastCellNum

    val (failures, answers): (Seq[String], Seq[ChoiceAnswer]) = partitionEitherSeq((meaningCellIndex to maxCellIndex by 2).map { cellIndex =>
      readStringCell(row, cellIndex) map { answer =>
        val id = (cellIndex - meaningCellIndex) / 2

        val correctnessCellStringValue = row.getCell(cellIndex + 1, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue

        val correctness = if (correctnessCellStringValue.nonEmpty) Correctness.Correct else Correctness.Wrong

        ChoiceAnswer(id, cardId, collId, langId, answer, correctness)
      }
    })

    failures.foreach(println)

    Right(CompleteFlashcard(
      Flashcard(cardId, collId, langId, cardType, question, meaning = None),
      choiceAnswers = answers
    ))

  }

  private def readTextRow(row: Row, langId: Int, collId: Int, cardType: CardType, question: String): Either[String, CompleteFlashcard] =
    readStringCell(row, meaningCellIndex) map { meaning =>
      CompleteFlashcard(
        Flashcard(row.getRowNum, collId, langId, cardType, question, Some(meaning)),
        choiceAnswers = Seq[ChoiceAnswer]()
      )
    }

  private def readRow(row: Row, langId: Int, collId: Int): Either[String, CompleteFlashcard] =
    readStringCell(row, cardTypeCellIndex) flatMap { cardTypeString: String =>

      cardTypeFromString(cardTypeString) flatMap { cardType: CardType =>

        readStringCell(row, questionCellIndex) flatMap { question: String =>

          cardType match {
            case CardType.Vocable | CardType.Text                => readTextRow(row, langId, collId, cardType, question)
            case CardType.SingleChoice | CardType.MultipleChoice => readChoiceRow(row, langId, collId, cardType, question)
          }

        }

      }

    }

  private def readStringCell(row: Row, index: Int): Either[String, String] = {
    val cell = row.getCell(index, MissingCellPolicy.CREATE_NULL_AS_BLANK)

    cell.getCellType match {
      case CellType.STRING => Right(cell.getStringCellValue.trim)
      case other           => Left(s"Column $index of row ${row.getRowNum} should be a string but was ${other.name()}")
    }
  }

}
