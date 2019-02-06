package controllers

import javax.inject.{Inject, Singleton}
import model._
import model.persistence.TableDefs
import play.api.data.Form
import play.api.libs.Files.TemporaryFile
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with ControllerHelpers with play.api.i18n.I18nSupport {

  def index: EssentialAction = futureWithUser(adminRightsRequired = true) { admin =>
    implicit request =>
      tableDefs.futureAllCourses map {
        allCourses => Ok(views.html.admin.adminIndex(admin, allCourses /*, FormMappings.newLanguageValuesForm*/))
      }
  }

  def courseAdmin(courseId: String): EssentialAction = futureWithUserAndCourse(adminRightsRequired = true, courseId) { (admin, course) =>
    implicit request =>
      tableDefs.futureCollectionsForCourse(course) map {
        collections => Ok(views.html.admin.courseAdmin(admin, course, collections))
      }
  }

  def allocateCollectionsToCourseForm(courseId: String): EssentialAction = futureWithUserAndCourse(adminRightsRequired = true, courseId) { (admin, course) =>
    implicit request =>
      tableDefs.futureCollectionsAndCourseImportState(course) map {
        collectionsAndCourseImportState => Ok(views.html.admin.allocateCollectionsToCourseForm(admin, course, collectionsAndCourseImportState))
      }
  }

  def allocateCollectionToCourse(courseId: String, collId: Int): EssentialAction = futureWithUserAndCourse(adminRightsRequired = true, courseId) { (admin, course) =>
    implicit request =>
      tableDefs.futureCollectionById(collId) flatMap {
        case None             => ???
        case Some(collection) =>
          tableDefs.allocateCollectionToCourse(course, collection) map {
            case true  => Redirect(routes.AdminController.allocateCollectionsToCourseForm(course.id))
            case false => ???
          }
      }
  }

  def deallocateCollectionFromCourse(courseId: String, collId: Int) : EssentialAction = futureWithUserAndCourse(adminRightsRequired =  true, courseId) {(admin, course) =>
    implicit  request =>
    tableDefs.futureCollectionById(collId) flatMap {
      case None => ???
      case Some(collection) =>
        tableDefs.deallocateCollectionFromCourse(course, collection) map {
          case true => Redirect(routes.AdminController.allocateCollectionsToCourseForm(courseId))
          case false => ???
        }
    }
  }

  def languageAdmin(langId: Int): EssentialAction = futureWithUserAndLanguage(adminRightsRequired = true, langId) { (admin, language) =>
    implicit request =>
      ???
    //      tableDefs.futureCollectionsForLanguage(language) map {
    //        collections => Ok(views.html.admin.languageAdmin(admin, language, collections, FormMappings.newCollectionValuesForm))
    //      }
  }

  def collectionAdmin(collId: Int): EssentialAction = futureWithUserAndCollection(adminRightsRequired = true, collId) { (admin, collection) =>
    implicit request =>
      tableDefs.futureFlashcardsForCollection(collection) map {
        flashcards => Ok(views.html.admin.collectionAdmin(admin, collection, flashcards))
      }
  }

  def newLanguage: EssentialAction = futureWithUser(adminRightsRequired = true) { admin =>
    implicit request =>
      def onError: Form[String] => Future[Result] = { formWithErrors =>
        tableDefs.futureAllCourses map {
          allCourses => BadRequest(views.html.admin.adminIndex(admin, allCourses /*, formWithErrors*/))
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

  def newCollection: EssentialAction = futureWithUser(adminRightsRequired = true) { admin =>
    implicit request =>
      def onError: Form[String] => Future[Result] = { formWithErrors =>
        //        tableDefs.futureCollectionsForLanguage(language) map {
        //          collections => BadRequest(views.html.admin.languageAdmin(admin, language, collections, formWithErrors))
        //        }
        ???
      }


      def onRead: String => Future[Result] = { newCollectionName =>
        val newCollection = Collection(-1, newCollectionName)

        tableDefs.futureInsertCollection(newCollection) map {
          newCollId => Redirect(routes.AdminController.collectionAdmin(newCollId))
        }
      }

      FormMappings.newCollectionValuesForm.bindFromRequest.fold(onError, onRead)
  }

  def uploadCardsFile(collId: Int): EssentialAction = futureWithUserAndCollection(adminRightsRequired = true, collId) { (admin, collection) =>
    implicit request =>

      request.body.asMultipartFormData flatMap (_.file(Consts.excelFileName)) match {
        case None => Future(Redirect(routes.AdminController.collectionAdmin(collId)))

        case Some(filePart: MultipartFormData.FilePart[TemporaryFile]) =>
          val (failureStrings, importedFlashcards) = Importer.importFlashcards(collId, filePart.ref.path)

          failureStrings.foreach(println)

          val futureImportedFlashcardsSaved = Future.sequence(importedFlashcards.map(
            tableDefs.futureInsertCompleteFlashcard
          ))

          futureImportedFlashcardsSaved map { importedFlashcardsSaved =>
            Ok(views.html.cardPreview(admin, collection, importedFlashcards, failureStrings))
          }
      }

  }

}
