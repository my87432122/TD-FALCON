/******************************************************************************

                      ====================================================
        Method:       Direct NDP
                      ====================================================

        Author:       Ah-Hwee Tan
        Date:         23.2.06

        Reference:
        
        AUTHOR = {Si, J. and Yang, L. and Liu, D.},
        TITLE = {Direct Neural Dynamic Programming},
        BOOKTITLE = {Handbook of Learning and Approximate Dynamic Programming},
        EDITOR = {Jennie Si and Andrew G. Barto and Warren Buckler Powell and Don Wunsch},
        MONTH = {August},
        YEAR = {2004},
        PUBLISHER = {Wiley-IEEE Press}}
        
 ******************************************************************************/

import java.io.*;
import java.lang.*;
import java.util.*;
import java.text.*;

public class DNDP extends AGENT
{
	final   int   numSpace=4; // 0-State 1-Action 2-Reward 3-New State 
    final   int   numSonarInput=5;
    final   int   numAVSonarInput=0;
    final   int   numBearingInput=8;
    final   int   numRangeInput=0;
    final   int   numAction=5;
    
    private int   N=numSonarInput+numBearingInput+numRangeInput;
    private int   A=numAction;
    private int   H=24, M=1;
    private double Input[];
        
	private final double alpha=0.95; //.95;
	private final boolean NONLINEAR=true;
	private final boolean explore=false;
	
	private final double rs=1.0;
	
	private int    decayIntervalRateCN=500;
	private int    decayIntervalRateAN=1000;
	private double initRateCN=0.2, initRateAN=0.2;
	private double rateCN, rateAN;
	
	private double r, J, prev_J, NextJ;
	private double prevReward = 0;
	
	private int   trial=0;
		
	private double [] p, q, x;
	private double [][][] wCN;
	private double [] errTermCN;
	private double eCN, eAN, ErrorCN, ErrorAN;

	private double [] u, v;
	private double [] g, h, y;
	private double [][][] wAN;    
    private double [][] wANL;

    private double MINWT=-1.0;
    private double MAXWT=1.0;
        
    private double MIN=-1.0;
    private double MAX=1.0;
    
    private boolean Trace=false;
    private boolean end_state;
    private int     agent_num;

    private int max_step;
    private int step;
    private int currentBearing;
    private int [][] path;
    private int [] current;	
    
    double MinTestError;    
    boolean train_flag = true;
    double Mean;

    double TrainError;
    double TrainErrorPredictingMean;
    double TestError;
    double TestErrorPredictingMean;
    
    //For overriding unuse methods in the abstract class
    
    public void decay () {}
    public void prune () {}
    public void purge () {}    
    public void penalize () {}
    public void reinforce () {}
    public double getMaxQValue (int i, boolean f, Maze m) {return (0);}    
    public double doSearchQValue (int i, int j) {return (0);}    
    public int    doSearchAction (int i, int j) {return (0);}
    public int    doSelectValidAction (boolean f, Maze m) {return 0;}
    public int doDirectAccessAction (int agt, boolean train, Maze maze) {return 0;}; 
    public void checkAgent (String outfile) {};
	public void saveAgent  (String outfile) {};
    
    private NumberFormat df = NumberFormat.getInstance();
        
    public void displayVector(String s, double[] x, int n) {
        System.out.print (s+ " : ");
        for (int i=0; i<n-1; i++)
            System.out.print (df.format(x[i])+", ");
        System.out.println (df.format(x[n-1]));
    }
    
    public double sigmoid (double x) {
    	double a=1.0;
    	return ((2.0/(double)(1.0+Math.exp(-x/a))) - 1.0);
//		return ((1.0-Math.exp(-x/a))/(1.0+Math.exp(-x/a)));
	}
	   
	public double random( double Low, double High )
    {
        return( ( double )Math.random() * ( High - Low ) + Low );
    }
    
    public DNDP (int av_num)
    {
    	agent_num = av_num;
        df.setMaximumFractionDigits (2);
        
        Input = new double[N];
              
		y = new double[N+A];
		p = new double[H];
		q = new double[H];
		
		wCN    = new double[2][][];
		wCN[0] = new double[H][];
		for (int i=0; i<H; i++) {
			wCN[0][i] = new double[N+A];
			for (int j=0; j<N+A; j++)
				wCN[0][i][j] = random (MINWT,MAXWT);	
		}
		wCN[1] = new double[1][];
		wCN[1][0] = new double[H];
		for (int j=0; j<H; j++)
			wCN[1][0][j] = random (MINWT,MAXWT);	
        
		x = new double[N];
		g = new double[H];
		h = new double[H];
		u = new double[A];
		v = new double[A];

		errTermCN = new double[A];
				
		wAN    = new double[2][][];
		
		wAN[0] = new double[H][];
		for (int i=0; i<H; i++) {
			wAN[0][i] = new double[N];
			for (int j=0; j<N; j++)
				wAN[0][i][j] = random (MINWT,MAXWT);
		}
		wAN[1] = new double[A][];
		for (int k=0; k<A; k++) {
			wAN[1][k] = new double[H];
			for (int i=0; i<H; i++)		
				wAN[1][k][i] = random (MINWT,MAXWT);
		}
	
		wANL = new double[A][];
		for (int k=0; k<A; k++) {
			wANL[k] = new double[N];
			for (int i=0; i<N; i++)
			wANL[k][i] = random (MINWT,MAXWT);
		}

		rateAN = initRateAN;
		rateCN = initRateCN;
				
        System.out.println( "ACN created");
        //current = new int[2];
    }
    
    public void setParameters (int AVTYPE, boolean immediateReward) {
		QEpsilonDecay = (double)0.00001;
		QEpsilon      = (double)0.50000;
		
		if (immediateReward)
    		QGamma = (double) 0.1;
    	else
    		QGamma = (double) 0.9;    	
    }
    
    public void init () {

		for (int i=0; i<H; i++) {
			for (int j=0; j<N; j++)
				wAN[0][i][j] = random (MINWT,MAXWT);
		}

		for (int k=0; k<A; k++) {
			for (int i=0; i<H; i++)		
				wAN[1][k][i] = random (MINWT,MAXWT);
		}
	
		for (int k=0; k<A; k++) {
			for (int i=0; i<N; i++)
			wANL[k][i] = random (MINWT,MAXWT);
		}
		
		rateAN = initRateAN;
		rateCN = initRateCN;
    }
    	
	public double errCN () {
		return (J-(r + alpha*NextJ));
//		return ((r + alpha*NextJ)-J);    this is wrong
//		return (r + alpha*J - prev_J);
	}

	public void propagateCN () {
		
		for (int i=0; i<H; i++) {
			q[i] = 0.0;
			for (int j=0; j<N+A; j++)
				q[i] += wCN[0][i][j] * y[j];
		}
		
		for (int i=0; i<H; i++)
			p[i] = sigmoid (q[i]);
			
		J = 0.0;
		for (int i=0; i<H; i++)
			J += wCN[1][0][i] * p[i];
		J = sigmoid (J);
	}

	public void errorBPCN () {
		double alpha=1.0;

		for (int i=0; i<H; i++)  
			wCN[1][0][i] -= rateCN * alpha * eCN * p[i];

		for (int i=0; i<H; i++)       
			for (int j=0; j<N+A; j++) 
				wCN[0][i][j] -= rateCN * alpha * eCN * wCN[1][0][i] * 0.5 * (1-p[i]*p[i]) * y[j];
		}		
	
	public double [] getErrTermCN () {   // gradJu
		
		for (int k=0; k<A; k++) {
			errTermCN[k] = 0.0;
			for (int i=0; i<H; i++)
				errTermCN[k] += wCN[1][0][i] * 0.5 * (1-p[i]*p[i]) * wCN[0][i][N+k];
		}
		return (errTermCN);
	}
	
	public void reduceRateCN () {
		if (rateCN>0.01) rateCN -= 0.01;
	}

	public void reduceRateAN () {
		if (rateAN>0.01) rateAN -= 0.01;
	}
	
	public double errAN () {
		return (J-rs);
//		return (J - (rs/(double)(1.0-alpha)));
	}

	public void setx (double [] Input, int N) {			                // for AN
		for (int i=0; i<N; i++)
			x[i] = (double) Input[i];
	}
	
	public void sety (double [] Input, int N, double [] action) {		// for CN
		for (int i=0; i<N; i++)
			y[i] = (double) Input[i];
		for (int k=0; k<A; k++)	
			y[N+k] = action[k];
	}
	
	public void propagateAN () {
		
		if (NONLINEAR) {

			for (int i=0; i<H; i++) {
				h[i] = 0.0;
				for (int j=0; j<N; j++)
					h[i] += wAN[0][i][j] * x[j];
			}
		
			for (int i=0; i<H; i++) 
				g[i] = sigmoid (h[i]);

			for (int k=0; k<A; k++) {
				v[k] = 0.0;
				for (int i=0; i<H; i++)
					v[k] += wAN[1][k][i] * g[i];
	
				u[k] = sigmoid (v[k]);
			}
		}
		else {   // LINEAR		

			for (int k=0; k<A; k++) {		
				v[k] = 0.0;
				for (int j=0; j<N; j++)
					v[k] += wANL[k][j] * x[j];
					
				u[k] = sigmoid (v[k]);
			}
		}
	}

	public void errorBPAN () {

		double [] errTerm = new double[A];
		for (int k=0; k<A; k++)
			errTerm[k] = eAN * errTermCN[k] * 0.5 * (1-u[k]*u[k]);   // estimated error for each action node
				
		if (NONLINEAR) {
			
			for (int i=0; i<H; i++)     
				for (int j=0; j<N; j++) 
					{
						double eT = 0.0;
						for (int k=0; k<A; k++)
							eT += errTerm[k]*wAN[1][k][i];
						wAN[0][i][j] -= rateAN * eT * 0.5 * (1-g[i]*g[i]) * x[j];  // not sure
					}
		
			for (int k=0; k<A; k++)
				for (int i=0; i<H; i++)
					wAN[1][k][i] -= rateAN * errTerm[k] * g[i];
					
		}
		else { // LINEAR

			for (int j=0; j<N; j++)
				for (int k=0; k<A; k++)
				wANL[k][j] -= rateAN * errTerm[k] * wANL[k][j] * x[j];
		}		
	}		

	public void normalizeAN () {
		double maxW = 0.0;
		
		for (int i=0; i<H; i++)
			for (int j=0; j<N; j++)
				if (maxW < Math.abs(wAN[0][i][j]))
					maxW = Math.abs(wAN[0][i][j]);
					
		if (maxW > 1.5)
		for (int i=0; i<H; i++)
			for (int j=0; j<N; j++)
				wAN[0][i][j] /= maxW;
		
		maxW=0.0;
		for (int k=0; k<A; k++)					 
		for (int i=0; i<H; i++)
			if (maxW < Math.abs(wAN[1][k][i]))
				maxW = Math.abs(wAN[1][k][i]);
					
		if (maxW > 1.5)
		for (int k=0; k<A; k++)
		for (int i=0; i<H; i++)
				wAN[1][k][i] /= maxW;
	}	
	
	public void normalizeCN () {
		double maxW = 0.0;
		
		for (int i=0; i<H; i++)
			for (int j=0; j<N+A; j++)
				if (maxW < Math.abs(wCN[0][i][j]))
					maxW = Math.abs(wCN[0][i][j]);
					
		if (maxW > 1.5)
		for (int i=0; i<H; i++)
			for (int j=0; j<N+A; j++)
				wCN[0][i][j] /= maxW;
		
		maxW=0.0;					 
		for (int i=0; i<H; i++)
			if (maxW < Math.abs(wCN[1][0][i]))
				maxW = Math.abs(wCN[1][0][i]);
					
		if (maxW > 1.5)
		for (int i=0; i<H; i++)
				wCN[1][0][i] /= maxW;
	}
	
    public void setState( double [] sonar, double [] av_sonar, int bearing, double range ) 
    {
        int index=0;
        
        for( int i = 0; i < numSonarInput; i++ ) 
            Input[index+i] = sonar[i];
        index += numSonarInput;
        
        for( int i = 0; i < numAVSonarInput; i++ ) 
            Input[index+i] = av_sonar[i];
        index += numAVSonarInput;
        
        for( int i = 0; i < numBearingInput; i++ )
            Input[index+i] = (double)0.0;
        if (bearing!=-1)
	        Input[index+bearing] = (double)1.0;        
        index +=numBearingInput;
        
        if (numRangeInput!=0)
	        Input[index] = (double) range;
    }
        
    public void initAction () {
    }

    public void init_path( int maxstep)
    {
        int k;

        max_step = maxstep;
        step = 0;
        currentBearing = 0;
        path = new int[max_step+1][2];
/*        for( k = 0; k < 2; k++ )
        {
            current[k] = 0;
            path[step][k] = 0;
        }
 */   }

    public void resetAction () {
    }
    
    public void setAction (int action) {
		if (numAction==1) {
		    if (action==0) u[0]=-1.0;
   	        else if (action==1) u[0]=-0.5;
            else if (action==3) u[0]=0.5;
            else if (action==4) u[0]=1.0;
            else u[0] = 0.0;
		}
	    else {
	    	for (int k=0; k<A; k++) 
	    		if (k==action) 
	    			u[k] = MAX; 
        		else u[k] = MIN;
        }
    }
        
    public void setReward (double reward) {
        r = reward-(1.0-rs);
    }
    
    public void initReward () {
        r = 1;
    }
  
    public double getPrevReward () {
    	return (prevReward);
    }
    
    public void setprev_J () {
    	prev_J = J;
    }

    public void setNextJ (double J) {
    	NextJ = J;
    }
        
    public void setPrevReward (double pr) {
    	prevReward = pr;
    }
  
    public void setNewState( double [] sonar, double [] av_sonar, int bearing, double range ) 
    {
    	int index=0;   
    	
        for (int i=0; i<numSonarInput; i++) 
            Input[i] = sonar[i];
        
        index += numSonarInput;
        for( int i = 0; i <numAVSonarInput; i++ ) 
            Input[index+i] = av_sonar[i];
        
        index += numAVSonarInput;
        for( int i = 0; i < numBearingInput; i++ )
            Input[index+i] = (double)0.0;
        Input[index+bearing] = (double)1.0;
        
        index +=numBearingInput;
        if (numRangeInput!=0)
	        Input[index] = (double) range;
    }

	public int findMax (double [] u, int A) {
		
		int id = 0;
		double maxU = u[0];
		
		for (int k=1; k<A; k++)
			if (maxU < u[k]) {
				maxU = u[k];
				id = k;
			}
		return (id);
	}			
	
	public double computeJ (Maze maze) {

        setx (Input, N);
        propagateAN ();
            
/*            int action = findMax (u, A, maze);        	     
	    	for (int k=0; k<A; k++) 
	    		if (k==action) u[k] = MAX;
        		else u[k] = MIN;
        	if (Trace) displayVector ("nu", u, A);
 */       	
        sety (Input, N, u);
    	propagateCN ();    	
    	return (J);
	}	

	public int findMax (double [] u, int A, Maze maze) {
		
		int id, maxid=0;
		int [] MMax = new int [A+1];
		
		double maxU = -999.0;
		MMax[0] = -1;
		
		for (int k=0; k<A; k++)
			if (maze.withinField (agent_num, k-2)) {
				if (maxU < u[k]) {
					maxU = u[k];
					maxid = 0;
					MMax[maxid] = k;
				}
				else if (maxU==u[k]) {
					maxid++;
					MMax[maxid] = k;
				}
			}
				
        if( maxid > 0 ) {
	        int randomIndex = (int) ( Math.random() * ( maxid + 1 ) );
			id = MMax[randomIndex];
        }
        else id = MMax[0];
        
/*        if (id==-1 && Trace) {
        	displayVector ("No Valid Action u", u, A); 
			for (int k=0; k<A; k++)
				if (maze.withinField (agent_num, k-2)) System.out.print ("true ");
				else System.out.print ("false ");
				System.out.println ("");
		}*/
		return (id);
	}			
	
	public int doSelectAction (boolean train, Maze maze) {
		int action=2;
		
		int [] validActions = new int[numAction];
        int maxVA = 0;
        
        for( int i = 0; i < numAction; i++ )
        	if (maze.withinField (agent_num, i-2)) {
    	       	validActions[maxVA] = i;
    	     maxVA++;
    	}
        
        if (maxVA==0) return (-1);
 	               
        setx (Input, N);
        propagateAN ();

		if (numAction==1) {		
   			if (u[0]<=-0.75)                    action = 0;
       		else if (u[0]>-0.75 && u[0]<=-0.25) action = 1;
       		else if (u[0]>-0.25 && u[0]<0.25)   action = 2;
       		else if (u[0]>=0.25 && u[0]<0.75)   action = 3;
			else if (u[0]>=0.75)                action = 4;
			
			if (maze.withinField (agent_num, action-2)) {
				if (Trace) System.out.println ("Chosen Action= " + action + " u=" + u[0]);
			}
			else {       // if chosen action is not valid, select a random action
	           	int randomIndex = (int) (Math.random() * maxVA);
            	action = validActions[randomIndex];
            	
             	if (action==0) u[0]=-1.0;
   	        	else if (action==1) u[0]=-0.5;
            	else if (action==3) u[0]=0.5;
            	else if (action==4) u[0]=1.0;
            	else u[0] = 0.0;
            	if (Trace) System.out.println ("Random Action=" + action + " u=" + u[0]);
            }                        
		}
		else {
			if (Trace) displayVector ("u", u, A);
			               
        	action = findMax (u, A, maze);
        	     
	    	if (!maze.withinField (agent_num, action-2)) {       // if chosen action is not valid, select a random action
		        int randomIndex = (int) (Math.random() * maxVA);
    	        action = validActions[randomIndex];
	    	}
	    	for (int k=0; k<A; k++) 
	    		if (k==action) u[k] = MAX;
        		else u[k] = MIN;
        	if (Trace) displayVector ("nu", u, A);
		}
			                  
		if (explore) {   
 	               
        	if( Math.random() < QEpsilon || !maze.withinField (agent_num, action-2)) {
        	// Select random action if exploring or invalid action.
            	if(Trace) 
            		System.out.println("random action selected!");
            	int randomIndex = (int) (Math.random() * maxVA);
            	action = validActions[randomIndex];
            
            	if (action!=-1) {
	            	if (numAction==1) {
		            	if (action==0) u[0]=-1.0;
	    	        	else if (action==1) u[0]=-0.5;
        		    	else if (action==2) u[0]=0.0;
            			else if (action==3) u[0]=0.5;
            			else if (action==4) u[0]=1.0;
            			else u[0] = 0.0;
            		}
            		else {
            			for (int k=0; k<numAction; k++)
            				u[k] = MIN;
            			u[action] = MAX;
            		}
            	}            
        	}
		}
		
		sety (Input, N, u);
    	propagateCN ();   	
		      
       	if (Trace)
	        System.out.println ("SelectActionACN: J=" + df.format(J) + " action=" + action);
		
        return action;
    }
    
	public void doLearnACN ()
    {        
    	double tCN=0.05, tAN=0.005;
    	int    maxNC=10, maxNA=20;
	
        sety (Input, N, u);
		propagateCN ();   // calculate new J
		    	
       	eCN = errCN ();
        ErrorCN = 0.5*eCN*eCN;
        
        int i = 0; 	       	        
  	    if (Trace) displayCNStatus (i);
  	    
        while (ErrorCN>tCN && i<maxNC) {
        	
         	errorBPCN ();        	
	       	propagateCN ();   // recalculate J
 		
       		eCN = errCN ();
       		ErrorCN = 0.5*eCN*eCN;
       		      
         	i++;
         	if (Trace && i%10==0) displayCNStatus (i); 
      	}
       	if (Trace && i%10!=0) displayCNStatus (i);
       	
//        normalizeCN ();
		
		i = 0;
		
		setx (Input, N);
        propagateAN ();
        sety (Input, N, u);
		propagateCN ();   // calculate new J
		
        eAN = errAN ();
        ErrorAN = 0.5*eAN*eAN;

        if (Trace) displayANStatus (i);
        
        while (ErrorAN>tAN && i<maxNA) {
        
            errTermCN = getErrTermCN();		
            errorBPAN ();        	        	

        	propagateAN ();   // recalculate u   
        	sety (Input, N, u);
        	propagateCN ();   // recalculate J
        
        	eAN = errAN ();
         	ErrorAN = 0.5*eAN*eAN;
         	 
         	i++;
         	if (Trace && i%10==0) displayANStatus (i);
      	}
		if (Trace && i%10!=0) displayANStatus (i);

		trial++;
		if (trial%decayIntervalRateCN==0) reduceRateCN ();
		if (trial%decayIntervalRateAN==0) reduceRateAN ();
    }
        
	public void displayANStatus (int i) {   	
	    System.out.println ("LoopAN "+ i + ": ErrorAN=" + df.format(ErrorAN) + 
	    								     " eAN=" + df.format (eAN) +
	    			                         " J=" + df.format(J) + 
	    			                         " NextJ=" + df.format(NextJ) +
	    			                         " r=" + df.format(r));
	    			                    			                        		
//		displayVector ("errTermCN", errTermCN, A);
	}
	
	public void displayCNStatus (int i) {      	
	    System.out.println ("LoopCN " + i + ": ErrorCN=" + df.format(ErrorCN) + 
	    								     " eCN=" + df.format (eCN) +
	    			                         " J=" + df.format(J) + 
	    			                         " NextJ=" + df.format(NextJ) +
	 				                         " r=" + df.format(r));
	}      	    

    public void setTrace (boolean t) {
        Trace = t;
    }
                          
    public int getNumCode() {
        return( H );
    }

    public int getCapacity () {
        return (H);
    }        
    
    // useless codes added by students
    
    public void turn (int d)
    {
        currentBearing = ( currentBearing + d + 8 ) % 8;
        if (d==4)
        	System.out.println ("agent cur = " + current[0] + ", " + current[1]);        	
	}

    public void move (int a, boolean succ) 
    {
        int k;

        currentBearing = ( currentBearing + a + 8 ) % 8;
        
        ++step;
        if( !succ )
        {
            path[step][0] = current[0];
            path[step][1] = current[1];
            return;
        }
                
        switch (currentBearing) {
             case 0:
                                current[1]--;
                                break;
                        case 1:
                                current[0]++;
                                current[1]--;
                                break;
                        case 2:
                                current[0]++;
                                break;
                        case 3:
                                current[0]++;
                                current[1]++;
                                break;
                        case 4:
                                current[1]++;
                                break;
                        case 5:
                                current[0]--;
                                current[1]++;
                                break;
                        case 6:
                                current[0]--;
                                break;
                        case 7:
                                current[0]--;
                                current[1]--;
                                break;
                        default: 
                break;
                }
        
        path[step][0] = current[0];
        path[step][1] = current[1];
        
      	if (Trace) System.out.println ("move cur = " + current[0] + ", " + current[1]);        	
            return;
    }
    
}
			