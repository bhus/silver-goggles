package ai

import upickle.default._
import scala.util.{Try, Success, Failure}

/**
 * The "Secret Sauce" for Safe AI in Scala.
 * This takes an LLM's raw JSON and validates it against a Scala 3 Case Class.
 * If the JSON is "almost" right, it can provide feedback for a "Repair" prompt.
 */
object SchemaValidator {

  /**
   * Attempts to parse JSON into a T.
   * Returns a Right(T) on success, or a Left(ErrorMessage) on failure.
   */
  def validate[T : Reader](json: String): Either[String, T] = {
    // Strip markdown formatting if the LLM wrapped it in ```json ... ```
    val cleanJson = json.stripPrefix("```json").stripPrefix("```").stripSuffix("```").trim
    
    Try(read[T](cleanJson)) match {
      case Success(value) => Right(value)
      case Failure(ex) => 
        Left(s"Validation Failed: ${ex.getMessage}\nRaw JSON was: $cleanJson")
    }
  }

  /**
   * Generates a "Repair Prompt" if validation fails.
   * This is a key feature for an "Agentic" framework.
   */
  def repairPrompt(error: String): String = {
    s"""The JSON you provided was invalid for the following reason:
       |$error
       |Please correct the JSON and ensure it strictly follows the schema.
       |Return ONLY the valid JSON block.""".stripMargin
  }
}
