import scalatags.Text.all._
import scalatags.Text.tags2.title

object App extends cask.MainRoutes {

  @cask.get("/")
  def hello() = {
    cask.Response(
      html(
        head(
          title("Li Haoyi Stack 2026")
        ),
        body(
          h1("Hello from Cask via Mill YAML!"),
          p("This is a modern Scala web project using the Li Haoyi stack: Mill (YAML), Cask, and Scalatags.")
        )
      ).render,
      headers = Seq("Content-Type" -> "text/html")
    )
  }

  initialize()
}
