# DASH_Simulator
DASH simulator
This program mimics the basic operation of an HTTP Adaptive Streaming (HAS) client. 
Adaptation algorithms:
- There are two adaptation algorithms, last segment and harmonic mean estimator
Bandwidth:
- there are two bandwidth variation setup, moderate fluctuation (where mean is 4 Mbps and standard deviation is 2 Mbps) and extreme fluctuation (where mean is 4 Mbps and standard deviation is 4 Mbps). In both cases, bandwidth values follow a normal distribution and are generated for every 1 ms
Input
- the only input that is required is the video segment size
- however, there are other inputs which the program expects, which you can comment according to your needs
