package model

import model.Consts._
import play.api.data.Form
import play.api.data.Forms._

final case class LoginFormValues(username: String, password: String)

final case class RegisterFormValues(username: String, pw: String, pwRepeat: String)

final case class LtiToolProxyRegistrationRequestFormValues(
  ltiMessageType: String,
  ltiVersion: String,
  regkey: String,
  regPassword: String,
  tcProfileUrl: String,
  launchPresentationReturnUrl: String
)

final case class LtiFormValues(username: String, courseIdentifier: String, courseName: String)

final case class ChangePwFormValues(oldPw: Option[String], firstNewPw: String, secondNewPw: String)

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

  val changePwForm: Form[ChangePwFormValues] = Form(
    mapping(
      oldPwName -> optional(nonEmptyText),
      newPw1Name -> nonEmptyText,
      newPw2Name -> nonEmptyText
    )(ChangePwFormValues.apply)(ChangePwFormValues.unapply)
  )

  val newLanguageValuesForm: Form[String] = Form(single(nameName -> nonEmptyText))

  val newCourseForm: Form[Course] = Form(
    mapping(
      idName -> number,
      shortNameName -> nonEmptyText,
      nameName -> nonEmptyText
    )(Course.apply)(Course.unapply)
  )

  val newCollectionForm: Form[CollectionBasics] = Form(
    mapping(
      idName -> number,
      courseIdName -> number,
      "frontLanguageId" -> number,
      "backLanguageId" -> number,
      nameName -> nonEmptyText
    )(CollectionBasics.apply)(CollectionBasics.unapply)
  )

  val ltiToolProxyRegistrationRequestForm: Form[LtiToolProxyRegistrationRequestFormValues] = Form(
    mapping(
      "lti_message_type" -> nonEmptyText,
      "lti_version" -> nonEmptyText,
      "reg_key" -> nonEmptyText,
      "reg_password" -> nonEmptyText,
      "tc_profile_url" -> nonEmptyText,
      "launch_presentation_return_url" -> nonEmptyText
    )(LtiToolProxyRegistrationRequestFormValues.apply)(LtiToolProxyRegistrationRequestFormValues.unapply)
  )

  val ltiValuesForm: Form[LtiFormValues] = Form(
    mapping(
      "ext_user_username" -> nonEmptyText,
      "context_label" -> nonEmptyText,
      "context_title" -> nonEmptyText
    )(LtiFormValues.apply)(LtiFormValues.unapply)
  )

}
