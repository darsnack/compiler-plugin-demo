object App {
  class A {
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

  val y: A = ???
  val z: A = ???
  val a: Double = ???
  val x: A = y + a * z
}
