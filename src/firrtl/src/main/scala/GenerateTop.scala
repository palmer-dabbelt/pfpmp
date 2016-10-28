// See LICENSE for license details.

import firrtl._
import firrtl.ir._
import firrtl.Annotations._
import firrtl.passes.Pass
import firrtl.Annotations.AnnotationMap

object FirrtlVerilogCompiler {
  val infer_read_write_id = TransID(-1)
  val repl_seq_mem_id     = TransID(-2)
  val clock_list_id       = TransID(-3)
}

class EmitTopVerilog(topName: String) extends PLSIPassManager {
  override def operateHigh() = Seq(
    new ReParentCircuit(topName)
  )

  override def operateMiddle() = Seq(
      new passes.InferReadWrite(FirrtlVerilogCompiler.infer_read_write_id),
      new passes.memlib.ReplSeqMem(FirrtlVerilogCompiler.repl_seq_mem_id)
    )

  override def operateLow() = Seq(
      new RemoveUnusedModules,
      new passes.clocklist.ClockListTransform(FirrtlVerilogCompiler.clock_list_id)
    )
}

object GenerateTop extends App {
  var input: Option[String] = None
  var output: Option[String] = None
  var synTop: Option[String] = None
  var harnessTop: Option[String] = None
  var seqMemFlags: Option[String] = Some("-o:unused.confg")
  var listClocks: Option[String] = None

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
      case "--seq-mem-flags" => {
        seqMemFlags = Some(args(i+1))
        usedOptions = usedOptions | Set(i+1)
      }
      case "--list-clocks" => {
        listClocks = Some(args(i+1))
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
    new EmitTopVerilog(synTop.get),
    Parser.UseInfo,
    AnnotationMap(Seq(
      passes.InferReadWriteAnnotation(
        s"${synTop.get}",
        FirrtlVerilogCompiler.infer_read_write_id
      ),
      passes.clocklist.ClockListAnnotation(
        s"${listClocks.get}",
        FirrtlVerilogCompiler.clock_list_id
      ),
      passes.memlib.ReplSeqMemAnnotation(
        s"-c:${synTop.get}:${seqMemFlags.get}",
        FirrtlVerilogCompiler.repl_seq_mem_id
      )
    ))
  )
}
