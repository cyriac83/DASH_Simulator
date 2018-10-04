
public class Segment{

	final static int qualityOption = 6; // 6 quality options for each segment of a particular quality level; 0 - 0% removed, 5 - 100% removed (in s							teps of 20)
	int segmentNumber;
	double qualityLevel;
	double segmentSize;
	double nonRefSize;
	double sensitivity;
	double[] psnr = new double[qualityOption];
	double[] ssim = new double[qualityOption];

		
}	
	
