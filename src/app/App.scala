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

  // Initialize table
  dbClient.transaction { db =>
    db.updateRaw("CREATE TABLE IF NOT EXISTS message (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT, timestamp TEXT)")
  }

  @cask.get("/")
  def hello() = {
    val messages: Seq[Message[Id]] = dbClient.transaction { db =>
      db.run(Message.select.sortBy(_.timestamp).desc)
    }
    
    // Demonstrate "Safe AI" by validating LLM response
    val aiSummary: Option[BoardSummary] = MockAIService.summarize(messages).toOption

    cask.Response(
      html(
        head(
          title("Li Haoyi Stack: Safe AI Edition")
        ),
        body(
          h1("Message Board (ScalaSql Edition)"),
          aiSummary.map { summary =>
            div(style := "background: #f4f4f4; padding: 10px; border-radius: 5px; margin-bottom: 20px")(
              h3("AI Summary"),
              p(summary.summary),
              p(i(s"Topics: ${summary.topics.mkString(", ")}"))
            )
          }.getOrElse(div()),
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

