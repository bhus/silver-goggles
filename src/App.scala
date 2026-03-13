import scalatags.Text.all._
import scalatags.Text.tags2.title
import scalikejdbc._

object App extends cask.MainRoutes {
  // Database setup
  Class.forName("org.sqlite.JDBC")
  ConnectionPool.singleton("jdbc:sqlite:messages.db", "", "")

  // Initialize table
  DB.autoCommit { implicit session =>
    sql"CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)".execute.apply()
  }

  @cask.get("/")
  def hello() = {
    // Using autoCommit for reads to avoid the setReadOnly(true) call which SQLite JDBC doesn't like
    val messages = DB.autoCommit { implicit session =>
      sql"SELECT content, timestamp FROM messages ORDER BY timestamp DESC"
        .map(rs => (rs.string("content"), rs.string("timestamp")))
        .list
        .apply()
    }
    
    cask.Response(
      html(
        head(
          title("Li Haoyi Stack with DB")
        ),
        body(
          h1("Message Board"),
          form(action := "/message", method := "post")(
            input(`type` := "text", name := "content", placeholder := "Type a message..."),
            button(`type` := "submit")("Post")
          ),
          hr(),
          h2("Messages:"),
          ul(
            messages.map { case (content, timestamp) =>
              li(
                b(timestamp), ": ", content
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
      DB.autoCommit { implicit session =>
        sql"INSERT INTO messages (content) VALUES ($content)".update.apply()
      }
    }
    cask.Redirect("/")
  }

  initialize()
}
