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
  extends PluginComponent {
  import global._
  override val phaseName = "compiler-plugin-phase"
  override val runsAfter = List("parser")
  override val runsBefore = List("namer")
  override def newPhase(prev: Phase) =
    new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {
        unit.body = new MyTransformer(unit).transform(unit.body)
      }
    }

  class MyTransformer(unit: CompilationUnit)
    extends Transformer {
    override def transform(tree: Tree) = tree match {
      case ClassDef(classmods, classname, classtparams, impl) if classname.toString == "Module" => {
        // println("Here")
        var implStatements: List[Tree] = List()
        for (node <- impl.body) node match {
          case DefDef(mods, name, tparams, vparamss, tpt, body) if name.toString == "loop" => {
            // println("Here Here")
            var statements: List[Tree] = List()
            for (statement <- body.children.dropRight(1)) statement match {
              case Assign(opd, rhs) => {
                val optimizedRHS = optimizeStatement(rhs)
                statements = statements ++ List(treeCopy.Assign(statement, opd, optimizedRHS))
              }
              case ValDef(mods, opd, tpt, rhs) => {
                // println("Here Here Here")
                val optimizedRHS = optimizeStatement(rhs)
                statements = statements ++
                  List(treeCopy.ValDef(statement, mods, opd, tpt, optimizedRHS))
              }
              case Apply(Select(src1, op), List(src2)) if op.toString == "push" => {
                val optimizedSrc2 = optimizeStatement(src2)
                statements = statements ++
                  List(treeCopy.Apply(statement, Select(src1, op), List(optimizedSrc2)))
              }
              case _ => statements = statements ++ List(statement)
            }

            val newBody = treeCopy.Block(body, statements, body.children.last)
            implStatements = implStatements ++
              List(treeCopy.DefDef(node, mods, name, tparams, vparamss, tpt, newBody))
          }
          case _ => implStatements = implStatements ++ List(node)
        }
        val newImpl = treeCopy.Template(impl, impl.parents, impl.self, implStatements)
        treeCopy.ClassDef(tree, classmods, classname, classtparams, newImpl)
      }
      case _ => super.transform(tree)
    }
  }

  def optimizeStatement(statement: Tree): Tree = {
    if (statement.children.length == 0) statement
    else statement match {
      case Apply(Apply(Select(src1, op), List(src2)), List(src3)) => {
        var optimizedSrc1 = optimizeStatement(src1)
        var optimizedSrc2 = optimizeStatement(src2)
        var newOp = TermName(op.toString)

        var optimizedNode = atPos(statement.pos.focus)(q"$optimizedSrc1.$newOp($optimizedSrc2)(..$src3)")
        optimizeStatementHelper(optimizedNode)
      }
      case Apply(Select(src1, op), List(src2)) => {
        var optimizedSrc1 = optimizeStatement(src1)
        var optimizedSrc2 = optimizeStatement(src2)
        var newOp = TermName(op.toString)

        var optimizedNode = atPos(statement.pos.focus)(q"$optimizedSrc1.$newOp($optimizedSrc2)")
        optimizeStatementHelper(optimizedNode)
      }
      case Select(src1, op) => {
        var optimizedSrc1 = optimizeStatement(src1)
        var newOp = TermName(op.toString)

        var optimizedNode = atPos(statement.pos.focus)(q"$optimizedSrc1.$newOp")
        optimizeStatementHelper(optimizedNode)
      }
      case _ => statement
    }
  }

  def optimizeStatementHelper(tree: Tree): Tree = tree

  def newTransformer(unit: CompilationUnit) =
    new MyTransformer(unit)
}