/**
 *  *  * RandomGen Class to generate uniform random variables (drawn ideas from MCRandom.java file)
 *  
 *  @author      Cyriac James
 *  @version   2.0, NOv 7, 2015
 * 
 * */



import java.util.Random;

public class RandomGen 
{

        // the default seed value used for Java random number generator
        private final static int default_seed = 97153;

        //Random object
        private Random random;

	 /**
	 ** Default constructor for RandomGen to initialize random number generator 
	 **
	 **/
        
	public RandomGen() {
                random = new Random(default_seed);
        }

	 /**
	** Constructor for RandomGen to initialize random number generator
	**
	** @param seed	Seed to initialize the random number generator
	**/

	public RandomGen(int seed) {
                random = new Random(seed);
        }


	/**
	* Returns a double array of uniformly distributed random numbers between 0 (inclusive) and 1 (exclusive).
	*
	* @param size      length of the output array
	* @return a double array of uniformly distributed random numbers
	*/
        public double[] getUniRnd(int size) {
                return random.doubles(size).toArray();
        }



	/**
      * Returns an integer array of uniform discrete random numbers between lower (inclusive) and upper (exclusive).
      *
      * @param size      length of the output array
      * @param lower     the lower limit of the random numbers (inclusive)
      * @param upper     the upper limit of the ransom numbers (exclusive)  
      * @return an integer array of uniformly distributed random numbers
      */
        public int[] getUniRnd(int size, int lower, int upper) {
                return random.ints(size, lower, upper).toArray();
        }


}
