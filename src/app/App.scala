package app

import scalatags.Text.all._
import scalatags.Text.tags2.title
import scalasql._
import scalasql.SqliteDialect._
import model.{Message, Id}
import ai.{BoardSummary, MockAIService}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object App extends cask.MainRoutes {
  // Database setup
  val dataSource = new org.sqlite.SQLiteDataSource()
  dataSource.setUrl("jdbc:sqlite:messages.db")

  val dbClient = new scalasql.DbClient.DataSource(
    dataSource,
    config = new scalasql.Config {
      override def nameMapper(v: String) = v.toLowerCase()
    }
  )

  // AI Service setup (Multi-provider)
  val aiService: ai.AIService = {
    sys.env.get("GROQ_API_KEY") match {
      case Some(key) => 
        println("INFO: Using GroqAIService (Lightning Fast)")
        new ai.GroqAIService(key)
      case None => 
        sys.env.get("GEMINI_API_KEY") match {
          case Some(key) => 
            println("INFO: Using GeminiAIService")
            new ai.GeminiAIService(key)
          case None => 
            println("WARNING: No AI keys found. Using MockAIService.")
            ai.MockAIService
        }
    }
  }

  // Initialize table
  dbClient.transaction { db =>
    db.updateRaw("CREATE TABLE IF NOT EXISTS message (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT, timestamp TEXT)")
  }

  @cask.get("/")
  def hello() = {
    val messages: Seq[Message[Id]] = dbClient.transaction { db =>
      db.run(Message.select.sortBy(_.timestamp).desc)
    }
    
    val aiResult = aiService.summarize(messages)
    
    val aiSummaryHtml = aiResult match {
      case Right(summary) => 
        div(style := "background: #ffffcc; padding: 15px; border: 2px solid green; border-radius: 10px; margin-bottom: 20px")(
          h3("✨ AI Insights (Safe AI)"),
          p(b(summary.summary)),
          p(i(s"Detected Topics: ${summary.topics.mkString(", ")}"))
        )
      case Left(error) if error.contains("429") || error.contains("Quota") =>
        div(style := "background: #ffe6e6; padding: 15px; border: 2px solid red; border-radius: 10px; margin-bottom: 20px")(
          h3("🛑 AI Quota Exceeded"),
          p("Your Gemini API key has no remaining quota or is restricted."),
          p(code(error))
        )
      case Left(error) => 
        div(style := "background: #f4f4f4; padding: 10px; border: 1px solid #ccc; margin-bottom: 20px")(
          p(i(s"AI Summary Unavailable: $error"))
        )
    }

    cask.Response(
      html(
        head(
          meta(charset := "utf-8"),
          title("Li Haoyi Stack: Safe AI Edition")
        ),
        body(
          h1("Message Board (ScalaSql Edition)"),
          aiSummaryHtml,
          form(action := "/message", method := "post")(
            input(`type` := "text", name := "content", placeholder := "Type a message..."),
            button(`type` := "submit")("Post")
          ),
          hr(),
          h2("Messages:"),
          ul(
            messages.map { msg =>
              li(
                b(msg.timestamp), ": ", msg.content
              )
            }
          )
        )
      ).render,
      headers = Seq("Content-Type" -> "text/html")
    )
  }

  @cask.postForm("/message")
  def postMessage(content: String) = {
    if (content.nonEmpty) {
      val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      dbClient.transaction { db =>
        db.run(
          Message.insert.columns(
            _.content := content,
            _.timestamp := time
          )
        )
      }
    }
    cask.Redirect("/")
  }

  initialize()
}

