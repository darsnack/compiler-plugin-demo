case class A (id: Int) {
  def +(that: A): A = ???
  def -(that: A): A = ???
  def *(that: A): A = ???

  def +(that: Double): A = ???
  def -(that: Double): A = ???
  def *(that: Double): A = ???
}

object A {
  implicit class Ops(lhs: Double) {
    def +(rhs: A): A = ???
    def -(rhs: A): A = ???
    def *(rhs: A): A = ???
  }
}


class Module {
  def loop() = {
    val y: A = A(0)
    val z: A = A(1)
    val w: A = A(2)
    val a: Double = 1.0
    val x: A = y + a * (w + z)

    x
  }
}
