import sbt.internal.util.complete.Parser
import sbt.internal.util.complete.DefaultParsers._

sealed abstract class Stage(val name: String) {
  val parser: Parser[this.type] = (Space ~> token(name)).map(_ => this)
}

object Stage {
  val parser: Parser[Stage] =
    token(Stage.Sandbox.parser) |
      token(Stage.Admin.parser)

  case object Sandbox extends Stage("Sandbox")
  case object Admin extends Stage("Admin")
}
