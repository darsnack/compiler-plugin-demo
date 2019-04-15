import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins._
import scala.tools.nsc.transform._

//https://stackoverflow.com/questions/55680855/how-does-the-scala-compiler-perform-implicit-conversion

class CompilerPlugin(override val global: Global)
  extends Plugin {
  override val name = "compiler-plugin"
  override val description = "Compiler plugin"
  override val components =
    List(new CompilerPluginComponent(global))
}

class CompilerPluginComponent(val global: Global)
  extends PluginComponent with TypingTransformers {
  import global._
  override val phaseName = "compiler-plugin-phase"
  override val runsAfter = List("parser")
  override def newPhase(prev: Phase) =
    new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {
        unit.body = new MyTypingTransformer(unit).transform(unit.body)
      }
    }

  class MyTypingTransformer(unit: CompilationUnit)
    extends TypingTransformer(unit) {
    override def transform(tree: Tree) = tree match {
      case ClassDef(classmods, classname, classtparams, impl) if classname.toString == "Module" => {
        var implStatements: List[Tree] = List()
        for (node <- impl.body) node match {
          case DefDef(mods, name, tparams, vparamss, tpt, body) if name.toString == "loop" => {
            var statements: List[Tree] = List()
            for (statement <- body.children.dropRight(1)) statement match {
              case Assign(opd, rhs) => {
                val optimizedRHS = optimizeStatement(rhs)
                statements = statements ++ List(Assign(opd, optimizedRHS))
              }
              case ValDef(mods, opd, tpt, rhs) => {
                val optimizedRHS = optimizeStatement(rhs)
                statements = statements ++
                  List(ValDef(mods, opd, tpt, optimizedRHS))
              }
              case Apply(Select(src1, op), List(src2)) if op.toString == "push" => {
                val optimizedSrc2 = optimizeStatement(src2)
                statements = statements ++
                  List(Apply(Select(src1, op), List(optimizedSrc2)))
              }
              case _ => statements = statements ++ List(statement)
            }

            val newBody = Block(statements, body.children.last)
            implStatements = implStatements ++
              List(DefDef(mods, name, tparams, vparamss, tpt, newBody))
          }
          case _ => implStatements = implStatements ++ List(node)
        }
        val newImpl = Template(impl.parents, impl.self, implStatements)
        ClassDef(classmods, classname, classtparams, newImpl)
      }
      case _ => super.transform(tree)
    }
  }

  def optimizeStatement(tree: Tree): Tree = tree

  def newTransformer(unit: CompilationUnit) =
    new MyTypingTransformer(unit)
}