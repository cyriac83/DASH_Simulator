
/**
 * DashSimulator Class for running the simulation
 * 
 * @author 	Cyriac James
 * @version	1.0, March 2018
 *
 */

import java.io.*;
import java.util.Scanner;

public class DashSimulator {

	// Simulation attributes
		
	// constants
	private final double totalSimTime = 600; // in seconds
	private final double segmentSize = 2; // in seconds   
	private final double maxBufferSize = 30; // maximum buffer size in seconds
        private final double minBufferSize = 2; // minimum buffer size in seconds - used to decide segment retransmission  	
	private int numQualityLevel = 3; // number of quality levels available
        //private final int mtu = 1000; // MTU of the network in KB; - not used now
	//private final double minQuality = 1000; // Minimum quality level (in Kbps) - not used now
	//private final double maxSegmentSize = 4000; //Max. segment size across all video and all quality levels (in KB)
	private final double timeIncrement = 0.001; // time granularity of the simulator is 1 ms

	// variables
        private double simTime; // current simulation time (in s)
	private int numSegment; // total number of segments in a video
	private int currentSegment; // Number of segments downloaded currently
	Segment[][] videoSegment; // to store info about all segments of a video - numSegment * numQuality - each row start with the highest quality
	private int adaptationType; // run time argument
	private int networkMode; // run time argument; moderate or extreme fluctuation 
	private double[] bandwidth; // bandwidth random variable array - only upto totalSimTime is required
        //double [] rtt; // rtt random variable array - not used currently
	private double muBw; // in Kbps
        private double sigmaBw; // in Kbps
        //private double muRtt; // in ms - not used now
        //private double sigmaRtt; // in ms - not used now
	private int harmonicWindow = 5; // harmonic window size - used by highly conservative algorithm
	private double[] downloadRate; // used by highly conservative algorithm

	// State variables
	//private double currentRTT = 0; //
	private double currentAvgRate = 0; // average rate at which current segment is downloaded (in Kbps)
	private double prevAvgRate = 1000; // initialize average rate to the minimum possible in the simulation i.e 1 Kbps
	private double currentSegmentSizeOriginal = 0; // original size of the curretly downloaded segment (in KB)
	private double currentSegmentSizeActual = 0; // actual size of currebtly downloaded segment - same as original size except for our algorithm (i								n KB)
	private double currentSegmentDownloadTime = 0; // time to download the current segment (in s)
	private double currentSegmentMinimum = 0; // minimum amount of a segment to be downloaded
	private double currentSegmentMinimumQos = 0; // minimum amount of a segment to be downloaded to maintain QoS
	private double bufferLevel = 0; // initialize buffer occupancy (in s)
	private boolean hasStall = false; // stall flag
	private double dataWastage = 0; // during retranmission (in KB) 	
	private double prevQualityLevel = 0; // quality level of previous segment (in Kbps)
	private double currentQualityLevel = 0; // quality level of current segment (in Kbps)
	private double currentPSNR = 0; // PSNR of currently downloaded segment (in dB)
	private double currentSSIM = 0; // SSIM of currently downloaded segment (ratio)
	private int numSwitches = 0; // number of switches
	private boolean reTransmit = false; // segment retransmit flag
	/* constructor to initialize before each simulation round*/

	public DashSimulator(double mu_bw_mod, double sigma_bw_mod, double mu_bw_ext, double sigma_bw_ext, double mu_rtt, double sigma_rtt, int adaptation_type, int network_mode, Scanner inputstream) {
	
		simTime = 0; // intialize current simulation time
		currentSegment = 0; // number of segments downloaded so far
		numSwitches = 0; // number of quality switches so far
		bufferLevel = maxBufferSize; // start simulation from the time buffer is full

                adaptationType = adaptation_type;
                networkMode = network_mode;
		downloadRate = new double[harmonicWindow];

	
		switch(networkMode)
		{
			case 1: // moderate fluctuation 
				muBw = mu_bw_mod;
				sigmaBw = sigma_bw_mod;
				break;
			case 2: // extreme fluctuation
				muBw = mu_bw_ext;
                                sigmaBw = sigma_bw_ext;
                                break;
		}
	
		 switch(adaptationType)
                {

                        case 1: // BETA
                                numQualityLevel = 3;
                                break;

                        case 2: // Mean
                                numQualityLevel = 5;
                                break;

                        case 3: // Harmonic Mean
                                numQualityLevel = 5;
                                break;


                }

		prevAvgRate = muBw;  // simulation starts with the assumption that player knows average rate before hand

		for(int i = 0; i < harmonicWindow; i++)
                        downloadRate[i] = muBw;
		//muRtt = mu_rtt;
		//sigmaRtt = sigma_rtt;

                /* logic for reading data from file */
		try {	
		numSegment = Integer.parseInt(inputstream.nextLine()); // read first line from the video file - number of segments in the video 
			
		int count = 0;	
		videoSegment = new Segment[numSegment][numQualityLevel];
		double tmp = 0;
		while(count < numSegment) // read all other info from the video file and initalize DS
		{
			//System.out.println("inside loop");
			double sensitivity = 1.0;
			for (int i = 0; i < numQualityLevel; i++)
			{
				videoSegment[count][i] = new Segment();	
				videoSegment[count][i].segmentNumber = count;

				if(i == 0)
				{
					tmp = Double.parseDouble(inputstream.nextLine());
                        		videoSegment[count][i].sensitivity = tmp;
					sensitivity = tmp;
				}
				else
					videoSegment[count][i].sensitivity = sensitivity;

				tmp = Double.parseDouble(inputstream.nextLine());	
				videoSegment[count][i].qualityLevel = tmp;

				//System.out.println("inside loop-1:" + tmp);				

				tmp = Double.parseDouble(inputstream.nextLine());
				videoSegment[count][i].segmentSize = tmp;


				tmp = Double.parseDouble(inputstream.nextLine());
				videoSegment[count][i].nonRefSize = tmp;

				// read psnr values for various percentage of non-reference frames removed
				for(int j = 0; j < Segment.qualityOption ; j++)
				{
					tmp = Double.parseDouble(inputstream.nextLine());
					videoSegment[count][i].psnr[j] = tmp;
			//		System.out.println("inside loop-2:" + tmp);
				}

				
				// read ssim values for various percentage of non-reference frames removed
				for(int j = 0; j < Segment.qualityOption ; j++)
                                {
                                        tmp = Double.parseDouble(inputstream.nextLine());
                                        videoSegment[count][i].ssim[j] = tmp; 
                                }
				
	
			
			}
			
			count++; // move to next segment info
		
		}
		}
		catch(Exception e){
                        System.out.println("Error in reading from the video file: " + e.getMessage());
                        System.exit(0);
                }
	

	
	}	
	
	public void run(int seed, PrintWriter output) 
	{
		/* initialize */

		//System.out.println("In run"); 
		try
                {
			// Random variable generator
                	RandomVariate randvar = new RandomVariate(seed);

			// returns bandwidth values (normal dist.) in Kbps required for the entire simulation in an array
                        bandwidth = randvar.normRandom((int)totalSimTime*2, muBw, sigmaBw);

			// returns bandwidth values required for the entire simulation in an array - not used currently
                        //rtt = randvar.normRandom((int)((totalSimTime/segmentSize) * (maxSegmentSize/mtu)), muRtt, sigmaRtt);

                }
                catch(Exception e){
                        System.out.println("Error in creating random numbers: " + e.getMessage());
                        System.exit(0);
                }
		
		/* Download segments and write output */
		while(simTime < totalSimTime) // loop until end of simulation time
		{
			//System.out.println("In while");
			if(bufferLevel > maxBufferSize - segmentSize) // if no space for another segment, wait until 1 segment space is available
			{
				simTime = simTime + (bufferLevel - (maxBufferSize - segmentSize));
				bufferLevel = maxBufferSize - segmentSize;
			}


			// intialization before each segment download 
			reTransmit = false; // every segment download starts with the hope that there will not be a retranmission!
        	        hasStall = false; // every segment download starts with the hope that there will not be any stalls!
	                dataWastage = 0; // every segment download starts with the hope that there will not be a retranmission!
	
			// get next segment
			switch(adaptationType)
			{
				case 1: // BETA algorithm
					//System.out.println("Before algo");
					adaptationBeat();
					{
					int i;
                                        for(i = 0; i < (harmonicWindow - 1); i++)
                                                downloadRate[i] = downloadRate[i+1];

                                        downloadRate[i] = currentAvgRate;
					}
					//System.out.println("After algo");
					break;

				case 2: // Less conservative algorithm - mean estimator
					//System.out.println("Before algo");
					adaptationLessConservative(); // select segment quality and download segment
					//System.out.println("After algo");
					break;

				case 3: // Highly conservative - harmonic estimator
					//System.out.println("Before algo");
                                        adaptationHighConservative(); // select segment quality and download segment
					// modify download rate array for new harmonic mean computation
					{
					int i;	
					for(i = 0; i < (harmonicWindow - 1); i++)
						downloadRate[i] = downloadRate[i+1];
		
					downloadRate[i] = currentAvgRate;
					}
                                        //System.out.println("After algo");
                                        break;
				
				default: // default
					System.out.println("No adaptation selected..exiting!");
					System.exit(0);
					break;

									
			}
			
			writeParamters(output); // write paramters to file after each segment download

			// getting reading for the next segment download
			prevAvgRate = currentAvgRate;
			prevQualityLevel = currentQualityLevel;
			currentSegment = currentSegment + 1; 
	                //System.out.println("sim time:" + simTime);

		}
		

				
			
		//System.out.println("outside run");	
	}



	// returns index of quality level of the next segment to be downloaded
	public void adaptationLessConservative()
	{
		int index = 0;

		try{
		
		// loop over once all segments in a video are processed
		while((videoSegment[currentSegment%numSegment][index].qualityLevel  > prevAvgRate) && (index < (numQualityLevel-1)))
			index++;
		
		//System.out.println("here");
		// for conservative algorithm, segment size orginal and actual are the same
		currentSegmentSizeOriginal = videoSegment[currentSegment%numSegment][index].segmentSize;
		currentQualityLevel = videoSegment[currentSegment%numSegment][index].qualityLevel;

		if( ((int)currentQualityLevel != (int)prevQualityLevel) && currentSegment > 0) // note switches - avoid first one which is unavoidable
			numSwitches++;
	
		currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[0];
		currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[0];

                double bytes_downloaded = 0; // initialize segment bytes downloaded so far
                double timeout_max = simTime + (bufferLevel - minBufferSize); // immediately after a segment download, buffer size is at least 2 seconds
                double time_before = simTime; // record the time at the beginning of a download

		while(true)
                {
                        simTime = simTime + timeIncrement;

                        if(bufferLevel >= timeIncrement )
                                 bufferLevel = bufferLevel - timeIncrement;
                        else
                        {
                                 bufferLevel = 0;
                                 hasStall = true;
                        }

                        bytes_downloaded = bytes_downloaded + (bandwidth[(int)simTime] * timeIncrement)/8;
			
			if(bandwidth[(int)simTime] < 0) // some times nornam distribution values may be negative (very rare)
                                bandwidth[(int)simTime] = 0;


                        if(bytes_downloaded >= currentSegmentSizeOriginal) // done with download
                        {
                                currentSegmentSizeActual = currentSegmentSizeOriginal;
                                currentSegmentDownloadTime = simTime -  time_before;
                                break;
                        }


                        if(simTime >= timeout_max && reTransmit == false)
                        {

                                // buffer is too low - switch to panic mode - retranmsit at lowest quality (if any)
                                if(index < (numQualityLevel-1))
                                {
                                        index = numQualityLevel-1;
                                        reTransmit = true;
                                        dataWastage = bytes_downloaded;
                                        bytes_downloaded = 0;
                                        time_before = simTime;
                                        currentSegmentSizeOriginal = videoSegment[currentSegment%numSegment][index].segmentSize;
                                        currentQualityLevel = videoSegment[currentSegment%numSegment][index].qualityLevel;
                                        currentSegmentSizeActual = currentSegmentSizeOriginal;
					currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[0];
                			currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[0];
					hasStall = false;
                                        // we don't increment the number of switches as segment is played out only after download           
                                }
                                else
                                {
                                        // already downloading lowest quality segment
                                }

                        }

                } // while	
		bufferLevel = bufferLevel + 2; // download complete, update buffer level

		} // try
		catch(Exception e)
                {
                        System.out.println("Error in adaptation - conservative: " + e.getMessage());
                        System.exit(0);

                }

		
	}

	public void adaptationHighConservative()
        {
                int index = 0;

                try{

                // loop over once all segments in a video are processed
		double harmonic_mean = harmonicMean(downloadRate);
                while((videoSegment[currentSegment%numSegment][index].qualityLevel  > harmonic_mean) && (index < (numQualityLevel-1)))
                        index++;

                //System.out.println("here");
                // for conservative algorithm, segment size orginal and actual are the same
                currentSegmentSizeOriginal = videoSegment[currentSegment%numSegment][index].segmentSize;
                currentQualityLevel = videoSegment[currentSegment%numSegment][index].qualityLevel;

                if( ((int)currentQualityLevel != (int)prevQualityLevel) && currentSegment > 0) // note switches - avoid first one which is unavoidable
                        numSwitches++;

                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[0];
                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[0];

                double bytes_downloaded = 0; // initialize segment bytes downloaded so far
		double timeout_max = simTime + (bufferLevel - minBufferSize); // immediately after a segment download, buffer size is at least 2 seconds
                double time_before = simTime; // record the time at the beginning of a download


                while(true)
                {
			simTime = simTime + timeIncrement;
                        
			if(bufferLevel >= timeIncrement )
                                 bufferLevel = bufferLevel - timeIncrement;
                        else
                        {
                                 bufferLevel = 0;
                                 hasStall = true;
                        }
			
			if(bandwidth[(int)simTime] < 0) // some times nornam distribution values may be negative (very rare)
                                bandwidth[(int)simTime] = 0;


                        bytes_downloaded = bytes_downloaded + (bandwidth[(int)simTime] * timeIncrement)/8;

                        if(bytes_downloaded >= currentSegmentSizeOriginal) // done with download
                        {
                                currentSegmentSizeActual = currentSegmentSizeOriginal;
                                currentSegmentDownloadTime = simTime -  time_before;
                                break;
                        }


			if(simTime >= timeout_max && reTransmit == false)

                        {

                        	// buffer is too low - switch to panic mode - retranmsit at lowest quality (if any)
                                if(index < (numQualityLevel-1))
                                {
                                	index = numQualityLevel-1;
                                        reTransmit = true;
                                        dataWastage = bytes_downloaded;
                                        bytes_downloaded = 0;
                                        time_before = simTime;
                                        currentSegmentSizeOriginal = videoSegment[currentSegment%numSegment][index].segmentSize;
                                        currentQualityLevel = videoSegment[currentSegment%numSegment][index].qualityLevel;
                                        currentSegmentSizeActual = currentSegmentSizeOriginal;
					currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[0];
                                        currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[0];
                                        hasStall = false;
                                        // we don't increment the number of switches as segment is played out only after download           
                                }
				else
				{
					// already downloading lowest quality segment
				}

			}

		} // while
                bufferLevel = bufferLevel + 2; // download complete, update buffer level

                } // try
                catch(Exception e)
                {
                        System.out.println("Error in adaptation - conservative: " + e.getMessage());
                        System.exit(0);

                }


        }

	public void adaptationBeat()
        {
                int index = 0;
                try{

                // loop over once all segments in a video are processed
	
		// enable for me estimator comparison
		while((videoSegment[currentSegment%numSegment][index].qualityLevel  > prevAvgRate) && (index < (numQualityLevel-1)))
                        index++;

		// enable for harmonic mean comparison
                //double harmonic_mean = harmonicMean(downloadRate); // enable for harmonic mean
		//while((videoSegment[currentSegment%numSegment][index].qualityLevel  > harmonic_mean) && (index < (numQualityLevel-1)))
                  //      index++;

                //System.out.println("here");
                // for conservative algorithm, segment size orginal and actual are the same
                currentSegmentSizeOriginal = videoSegment[currentSegment%numSegment][index].segmentSize;
                currentSegmentMinimum = videoSegment[currentSegment%numSegment][index].segmentSize - videoSegment[currentSegment%numSegment][index].nonRefSize;
                currentQualityLevel = videoSegment[currentSegment%numSegment][index].qualityLevel;

                if( ((int)currentQualityLevel != (int)prevQualityLevel) && currentSegment > 0) // note switches - avoid first one which is unavoidable
                        numSwitches++;

                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[0];
                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[0];

                double bytes_downloaded = 0; // initialize segment bytes downloaded so far
		double timeout = simTime + currentSegmentSizeOriginal/prevAvgRate;
		double sensitivity =  videoSegment[currentSegment%numSegment][index].sensitivity; 		
		currentSegmentMinimumQos = sensitivity * currentSegmentSizeOriginal;
		double timeout_max = simTime + (bufferLevel - minBufferSize); // immediately after a segment download, buffer size is at least 2 seconds
		double time_before = simTime; // record the time at the beginning of a download
	
                while(true)
                {
			
                        //System.out.println("segment size:" + currentSegmentSizeOriginal);
                        //System.out.println("bytes downloaded###:" + bytes_downloaded);
                        //System.out.println("bwd##:" + bandwidth[(int)simTime]);
                        simTime = simTime + timeIncrement;
			if(bufferLevel >= timeIncrement )
                                 bufferLevel = bufferLevel - timeIncrement;
                        else
                        {
                                 bufferLevel = 0;
                                 hasStall = true;
                        }

			if(bandwidth[(int)simTime] < 0) // some times nornam distribution values may be negative (very rare)
				bandwidth[(int)simTime] = 0;


			bytes_downloaded = bytes_downloaded + (bandwidth[(int)simTime] * timeIncrement)/8;

			if(bytes_downloaded >= currentSegmentSizeOriginal) // done with download
			{
				currentSegmentSizeActual = currentSegmentSizeOriginal;
				currentSegmentDownloadTime = simTime -  time_before;
				break;
			}
				

			if(simTime >= timeout && reTransmit == false)
			{
				if(bytes_downloaded < currentSegmentMinimum)
                                {
					// buffer is too low - switch to panic mode - retranmsit at lowest quality (if any)
                                	if(bufferLevel <= minBufferSize && index < (numQualityLevel-1))
                                        {
                                        	index = numQualityLevel-1;
                                                reTransmit = true;
                                                dataWastage = bytes_downloaded;
                                                bytes_downloaded = 0;
						time_before = simTime;
                                                currentSegmentSizeOriginal = videoSegment[currentSegment%numSegment][index].segmentSize;
                                                currentQualityLevel = videoSegment[currentSegment%numSegment][index].qualityLevel;
                                		currentSegmentSizeActual = currentSegmentSizeOriginal;    
						currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[0];
                                        	currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[0];
                                        	hasStall = false;
						// we don't increment the number of switches as segment is played out only after download           
				        }
					else
					{
							// wait until max time out
					}
				}
				else if(bytes_downloaded >= currentSegmentMinimum && bytes_downloaded < currentSegmentMinimumQos)
				{
					if(bufferLevel <= minBufferSize) // move on with the next segment -- buffer level too low
					{
						currentSegmentSizeActual = bytes_downloaded;
		                                currentSegmentDownloadTime = simTime -  time_before;
						if(bytes_downloaded >= currentSegmentSizeOriginal * 0.8) // 20 % removed
						{
							currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[1];
                					currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[1];
						}
						else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.6 && bytes_downloaded < currentSegmentSizeOriginal * 0.8) // 40% removed
                                                {
                                                        currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[2];
                                                        currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[2];
                                                }
						else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.4 && bytes_downloaded < currentSegmentSizeOriginal * 0.6) // 60% removed
                                                {
                                                        currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[3];
                                                        currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[3];
                                                }
						else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.2 && bytes_downloaded < currentSegmentSizeOriginal * 0.4) // 80% removed
                                                {
                                                        currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[4];
                                                        currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[4];
                                                }
						else // 100% removed                                                 
						{
                                                        currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[5];
                                                        currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[5];
                                                }
                		                break;
                        		}
					else
                                        {
                                                        // wait until max time out
                                        }
				}
				else if(bytes_downloaded >= currentSegmentMinimumQos) // stop download
				{
					currentSegmentSizeActual = bytes_downloaded;
                                        currentSegmentDownloadTime = simTime -  time_before;
                                        if(bytes_downloaded >= currentSegmentSizeOriginal * 0.8) // 20 % removed
                                        {
                                	        currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[1];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[1];
                                        }
                                        else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.6 && bytes_downloaded < currentSegmentSizeOriginal * 0.8) // 40 % removed
                                        {
                                                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[2];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[2];
                                        }
                                        else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.4 && bytes_downloaded < currentSegmentSizeOriginal * 0.6) // 60% removed
                                        {
                                                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[3];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[3];
                                        }
                                        else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.2 && bytes_downloaded < currentSegmentSizeOriginal * 0.4) // 80% removed
                                        {
                                                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[4];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[4];
                                        }
                                        else // 100% removed                                                 
                                        {
                                        	currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[5];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[5];
                                        }
                                         
					break;
				}
			}

			if(simTime >= timeout_max && reTransmit == false)
                        {
				
				if(bytes_downloaded < currentSegmentMinimum)
                                {
                                        // buffer is too low - switch to panic mode - retranmsit at lowest quality (if any)
                                        if(index < (numQualityLevel-1))
                                        {
                                                index = numQualityLevel-1;
                                                reTransmit = true;
                                                dataWastage = bytes_downloaded;
                                                bytes_downloaded = 0;
                                                time_before = simTime;
                                                currentSegmentSizeOriginal = videoSegment[currentSegment%numSegment][index].segmentSize;
                                                currentQualityLevel = videoSegment[currentSegment%numSegment][index].qualityLevel;
                                                currentSegmentSizeActual = currentSegmentSizeOriginal;
						currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[0];
                                        	currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[0];
                                        	hasStall = false;
                                                // we don't increment the number of switches as segment is played out only after download           
                                        }
                                        else
                                        {
                                                        // already downloading lowest quality segment wait until segment download
                                        }
                                }
				else
				{
					currentSegmentSizeActual = bytes_downloaded;
                                        currentSegmentDownloadTime = simTime -  time_before;
                                        if(bytes_downloaded >= currentSegmentSizeOriginal * 0.8) // 20 % removed
                                        {
                                                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[1];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[1];
                                        }
                                        else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.6 && bytes_downloaded < currentSegmentSizeOriginal * 0.8) // 40 % removed
                                        {
                                                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[2];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[2];
                                        }
                                        else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.4 && bytes_downloaded < currentSegmentSizeOriginal * 0.6) // 60% removed
                                        {
                                                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[3];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[3];
                                        }
                                        else if(bytes_downloaded >= currentSegmentSizeOriginal * 0.2 && bytes_downloaded < currentSegmentSizeOriginal * 0.4) // 80% removed
                                        {
                                                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[4];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[4];
                                        }
                                        else // 100% removed                                                 
                                        {
                                                currentPSNR = videoSegment[currentSegment%numSegment][index].psnr[5];
                                                currentSSIM = videoSegment[currentSegment%numSegment][index].ssim[5];
                                        }

                                        break;
                                }
			}
					

		} // while				
                bufferLevel = bufferLevel + 2; // download complete, update buffer level

		} // try
                catch(Exception e)
                {
                        System.out.println("Error in adaptation - conservative: " + e.getMessage());
                        System.exit(0);

                }


        }


	// compute harmonic mean - for highly conservative algorithm
	private double harmonicMean(double[] rate) 
	{


    		int count = 0;
    		int num = 0;
    		double sum = 0;

    		while(count < harmonicWindow)
    		{

        		if(rate[count] != 0)
        		{
                		sum = sum + 1/rate[count];
                		num++;
        		}
        	
			count++;
    		}

    		return  num/sum;
	}
	

	/* write output */
	public void writeParamters(PrintWriter output)
	{
		// write to an output file the following
		try{
		//System.out.println("Inside write method");
		currentAvgRate = currentSegmentSizeActual/currentSegmentDownloadTime * 8; // in Kbps
		output.print(currentSegment + "\t" + currentSegmentSizeOriginal + "\t" + currentSegmentSizeActual + "\t" + currentSegmentDownloadTime + "\t" + currentAvgRate + "\t" +  currentQualityLevel + "\t" + currentPSNR + "\t" + currentSSIM + "\t" + hasStall + "\t" + numSwitches + "\t" + reTransmit + "\t" + dataWastage + "\t" + bufferLevel + "\n");
		output.flush();
		}
		catch(Exception e)
		{
			System.out.println("Error in write: " + e.getMessage());
                        System.exit(0);
		}
	}
	


	 public static void main(String[] args) {
                
		// moderate bw settings
                double mu_bw_mod = 4000; // in Kbps
                double sigma_bw_mod = 2000; // in Kbps
		
		// extreme bw settings
		double mu_bw_ext = 4000; // in Kbps
	        double sigma_bw_ext = 4000; // in Kbps
                
		// rtt setting - not used currently
		double mu_rtt = 100; // in ms
                double sigma_rtt = 20; // in ms

		if (args.length != 3) 
		{
			System.out.println("Usage: java DashSimulator <adaptation_type, 1 - Beta, 2 - mean, 3 - harmonic> <network_mode, 1 - moderate, 2 - extreme> <in_file>");
                        System.exit(0);
                }

		int adaptation_type = Integer.parseInt(args[0]); // 1 - proposed, 2 - less conservative (mean), 3 - more conservative (harmonic)
		int network_mode = Integer.parseInt(args[1]); // 1 - moderate variation, 2 - extreme variation


		String network_scenario = "";
		String adaptation_alg = "";

		switch(network_mode)
		{

			case 1: // moderate
				network_scenario = "moderate";
				break;
			
			case 2: // extreme
				network_scenario = "extreme";
                                break;

		}


		switch(adaptation_type)
                {

                        case 1: // BETA
                                adaptation_alg = "beta";
                                break;

                        case 2: // Mean
                                adaptation_alg = "mean";
                                break;
			
			case 3: // Harmonic Mean
                                adaptation_alg = "harmonic";
                                break;


                }



                // seeds for 5 runs
                int[] seed = {12345, 34567, 45679, 12357, 98765};

		String infilename = args[2];
                // start and run the simulation
                try{
               
                int count = 1;
                for (int s : seed) {
			File infile = new File(infilename + ".txt");  // video file
                	Scanner inputstream = new Scanner(infile);
                        DashSimulator sim = new DashSimulator(mu_bw_mod, sigma_bw_mod, mu_bw_ext, sigma_bw_ext, mu_rtt, sigma_rtt, adaptation_type, network_mode, inputstream);

                        
                        String outfilename = "output_" + infilename + "_" + adaptation_alg + "_" + network_scenario + "-" + Integer.toString(count);
			File outfile = new File(outfilename);  // output file
		        PrintWriter outputstream = new PrintWriter(outfile);
			sim.run(s, outputstream);
	               	inputstream.close();
			outputstream.close();
			count++;
                }
		}
                catch(Exception e)
                {
                        System.out.println("Error in main: " + e.getMessage());
                        System.exit(0);

		}
	}
}

