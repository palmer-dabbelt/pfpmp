// See LICENSE for license details.

import firrtl._
import firrtl.ir._
import firrtl.passes.Pass

class EnumerateModules(enumerate: (Module) => Unit) extends Pass {
  def name = "Enumurate Modules"

  def run(c: Circuit): Circuit = {
    val modulesx = c.modules.map {
      case m: ExtModule => m
      case m: Module => {
        enumerate(m)
        m
      }
    }
    Circuit(c.info, modulesx, c.main)
  }
}

