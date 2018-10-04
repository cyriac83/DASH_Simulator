/**
 *  * RandomVariate Class to generate exponential random variables
 *   * 
 *    * @author      Cyriac James
 *     * @version     2.0, Nov 7, 2015
 *      *
 *       */


import java.util.Random;

public class RandomVariate
{
	// Object of RandomGen
	RandomGen randgen = null;	

 /**
 *     * Constructor for RandomVariate to initialize random number generator
 *     @param seed   Seed to initialize the random number generator
 *     */

	public RandomVariate(int seed)
	{
		randgen = new RandomGen(seed);
	}


   	/**
 	*Creates a double array of exponential random variates with mean 1/param
	 *       
	 *@param size      length of the output array
	 *@param param     Paramter for exponentaion distribution
	 *@return double array containig exponentially distributed random variates
	 **/

	public double[] expRandom(int size, double param)
	{
		double [] unirand = randgen.getUniRnd(size);
		double [] exprand = new double[size];

		// Generate exponential random variates from uniform random numbers using inverse transfom method
		for(int i=0; i<exprand.length; i++)			
			exprand[i] = -(1/param) * Math.log(unirand[i]);

		return exprand;		

	}


	 /**
         *Creates an int array of uniform random variates
         *@param size      length of the output array
         *@param maxcustomer_perbatch     Max. value taken by the uniform random variate
         *@return int array containig uniformly distributed random variates between 1 and maxcustomer_perbatch
          **/

	public int[] uniRandom(int size, int maxcustomer_perbatch)
	{
		int [] unirand;
		unirand = randgen.getUniRnd(size, 1, maxcustomer_perbatch + 1);

		return unirand;
	}	


	/**
         *Creates a double array of normal random variates using Box-Muller Method
         *@param size      length of the output array
         *@param param_mu  Mean of the distribution
	 *@param param_sigma Standard deviation of the distribution
         *@return double array containig normally distributed random variates with mean 'param_mu' and standar deviation 'param_sigma'
          **/

	public double[] normRandom(int size, double param_mu, double param_sigma)
	{
		double [] unirand = randgen.getUniRnd(size);
                double [] normrand = new double[size];

                // Generate normal random variates from uniform random numbers using Box-Muller transfom method
                for(int i=0; i<normrand.length; i=i+2)
		{
                	normrand[i] = param_mu + param_sigma * Math.sqrt(-2 * Math.log(unirand[i])) * Math.cos(2 * Math.PI * unirand[i+1]);
                        normrand[i+1] = param_mu + param_sigma * Math.sqrt(-2 * Math.log(unirand[i])) * Math.sin(2 * Math.PI * unirand[i+1]);
 		}
                
		return normrand;
                
	}
	

	
}
