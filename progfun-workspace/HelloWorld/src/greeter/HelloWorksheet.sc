package greeter

object Sheet {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
	val x=7;                                  //> x  : Int = 7
	def increase(i: Int) = i+1                //> increase: (i: Int)Int
	increase(x)                               //> res0: Int = 8
}