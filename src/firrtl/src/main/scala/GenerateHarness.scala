// See LICENSE for license details.

import firrtl._
import firrtl.ir._
import firrtl.Annotations._
import firrtl.passes.Pass
import firrtl.Annotations.AnnotationMap

class EnumerateTopModules(topName: String) extends PLSIPassManager {
  override def operateHigh() = Seq(
    new ReParentCircuit(topName)
  )

  override def operateLow() = Seq(
    new RemoveUnusedModules,
    new EnumerateModules( { m => if (m.name != topName) { AllModules.add(m.name) } } )
  )
}

class EmitHarnessVerilog(topName: String) extends PLSIPassManager {
  override def operateLow() = Seq(
      new ConvertToExtMod((m) => m.name == topName),
      new RemoveUnusedModules,
      new RenameModulesAndInstances((m) => AllModules.rename(m))
    )
}

object AllModules {
  private var modules = Set[String]()
  def add(module: String) = {
    modules = modules | Set(module)
  }
  def rename(module: String) = {
    var new_name = module
    while (modules.contains(new_name))
      new_name = new_name + "_inTestHarness"
    new_name
  }
}

object GenerateHarness extends App {
  var input: Option[String] = None
  var output: Option[String] = None
  var synTop: Option[String] = None
  var harnessTop: Option[String] = None

  var usedOptions = Set.empty[Integer]
  args.zipWithIndex.foreach{ case (arg, i) =>
    arg match {
      case "-i" => {
        input = Some(args(i+1))
        usedOptions = usedOptions | Set(i+1)
      }
      case "-o" => {
        output = Some(args(i+1))
        usedOptions = usedOptions | Set(i+1)
      }
      case "--syn-top" => {
        synTop = Some(args(i+1))
        usedOptions = usedOptions | Set(i+1)
      }
      case "--harness-top" => {
        harnessTop = Some(args(i+1))
        usedOptions = usedOptions | Set(i+1)
      }
      case _ => {
        if (! (usedOptions contains i)) {
          error("Unknown option " + arg)
        }
      }
    }
  }

  firrtl.Driver.compile(
    input.get,
    output.get,
    new EnumerateTopModules(synTop.get),
    Parser.UseInfo
  )

  firrtl.Driver.compile(
    input.get,
    output.get,
    new EmitHarnessVerilog(synTop.get),
    Parser.UseInfo
  )
}
