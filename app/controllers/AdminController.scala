package controllers

import javax.inject.{Inject, Singleton}
import model._
import play.api.data.Form
import play.api.libs.Files.TemporaryFile
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with ControllerHelpers with play.api.i18n.I18nSupport {

  def index: EssentialAction = futureWithUser(adminRightsRequired = true) { admin =>
    implicit request =>
      tableDefs.futureAllLanguages map {
        allLanguages => Ok(views.html.admin.adminIndex(admin, allLanguages, FormMappings.newLanguageValuesForm))
      }
  }

  def languageAdmin(langId: Int): EssentialAction = futureWithUserAndLanguage(adminRightsRequired = true, langId) { (admin, language) =>
    implicit request =>
      tableDefs.futureCollectionsForLanguage(language) map {
        collections => Ok(views.html.admin.languageAdmin(admin, language, collections, FormMappings.newCollectionValuesForm))
      }
  }

  def collectionAdmin(langId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(adminRightsRequired = true, langId, collId) { (admin, language, collection) =>
    implicit request =>
      tableDefs.futureFlashcardsForCollection(collection) map {
        flashcards => Ok(views.html.admin.collectionAdmin(admin, language, collection, flashcards))
      }
  }

  def newLanguage: EssentialAction = futureWithUser(adminRightsRequired = true) { admin =>
    implicit request =>
      def onError: Form[String] => Future[Result] = { formWithErrors =>
        tableDefs.futureAllLanguages map {
          allLanguages => BadRequest(views.html.admin.adminIndex(admin, allLanguages, formWithErrors))
        }
      }

      def onRead: String => Future[Result] = { newLanguageName =>
        val newLanguage = Language(-1, newLanguageName)

        tableDefs.futureInsertLanguage(newLanguage) map {
          newLangId => Redirect(routes.AdminController.languageAdmin(newLangId))
        }
      }

      FormMappings.newLanguageValuesForm.bindFromRequest.fold(onError, onRead)
  }

  def newCollection(langId: Int): EssentialAction = futureWithUserAndLanguage(adminRightsRequired = true, langId) { (admin, language) =>
    implicit request =>
      def onError: Form[String] => Future[Result] = { formWithErrors =>
        tableDefs.futureCollectionsForLanguage(language) map {
          collections => BadRequest(views.html.admin.languageAdmin(admin, language, collections, formWithErrors))
        }
      }


      def onRead: String => Future[Result] = { newCollectionName =>
        val newCollection = Collection(-1, langId, newCollectionName)

        tableDefs.futureInsertCollection(newCollection) map {
          newCollId => Redirect(routes.AdminController.collectionAdmin(langId, newCollId))
        }
      }

      FormMappings.newCollectionValuesForm.bindFromRequest.fold(onError, onRead)
  }

  def uploadCardsFile(langId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(adminRightsRequired = true, langId, collId) { (admin, language, collection) =>
    implicit request =>

      request.body.asMultipartFormData flatMap (_.file(Consts.excelFileName)) match {
        case None => Future(Redirect(routes.AdminController.collectionAdmin(langId, collId)))

        case Some(filePart: MultipartFormData.FilePart[TemporaryFile]) =>
          val (failureStrings, importedFlashcards) = Importer.importFlashcards(langId, collId, filePart.ref.path)

          val futureImportedFlashcardsSaved = Future.sequence(importedFlashcards.map(
            tableDefs.futureInsertCompleteFlashcard
          ))

          futureImportedFlashcardsSaved map { importedFlashcardsSaved =>
            Ok(views.html.cardPreview(admin, language, collection, importedFlashcardsSaved, failureStrings))
          }
      }

  }

}
