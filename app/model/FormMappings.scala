package model

import play.api.data.Form
import play.api.data.Forms._
import Consts._

final case class LoginFormValues(username: String, password: String)

final case class RegisterFormValues(username: String, pw: String, pwRepeat: String)

final case class NewCollectionFormValues()

final case class LtiFormValues(username: String, courseIdentifier: String, courseName: String)

object FormMappings {

  val loginValuesForm: Form[LoginFormValues] = Form(
    mapping(
      usernameName -> nonEmptyText,
      pwName -> nonEmptyText
    )(LoginFormValues.apply)(LoginFormValues.unapply)
  )

  val registerValuesForm: Form[RegisterFormValues] = Form(
    mapping(
      usernameName -> nonEmptyText,
      pwName -> nonEmptyText,
      repeatPwName -> nonEmptyText
    )(RegisterFormValues.apply)(RegisterFormValues.unapply)
  )

  val newLanguageValuesForm: Form[String] = Form(single(nameName -> nonEmptyText))

  val newCollectionValuesForm: Form[String] = Form(single(nameName -> nonEmptyText))

  val ltiValuesForm: Form[LtiFormValues] = Form(
    mapping(
      "ext_user_username" -> nonEmptyText,
      "context_label" -> nonEmptyText,
      "context_title" -> nonEmptyText
    )(LtiFormValues.apply)(LtiFormValues.unapply)
  )

}
