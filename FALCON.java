import java.io.*;
import java.lang.*;
import java.text.*;

public class FALCON extends AGENT {
    final   int   numSpace=4; // 0-State 1-Action 2-Reward 3-New State 
    final   int   numSonarInput=10;
    final   int   numAVSonarInput=0;
    final   int   numBearingInput=8;
    final   int   numRangeInput=0;
    final   int   numAction=5;
    final   int   numReward=2;
    final   int   complementCoding=1;
    
    final   int   CURSTATE=0;
    final   int   ACTION  =1;
    final   int   REWARD  =2;
    final   int   NEWSTATE=3;
    
    final static int RFALCON =0;
    final static int TDFALCON=1;
    
    final   int   FUZZYART=0;
    final   int   ART2    =1;
    
    final   int   PERFORM =0;
    final   int   LEARN   =1;
    final   int   INSERT  =2;
    
    private int[]      numInput;
    private int        numCode;
    private double     prevReward=0;
    private double[][] activityF1;
    private double[]   activityF2;
    private double[][][] weight; 
    private int        J;
    private int        KMax=3;
    private boolean[]  newCode;
    private double[]   confidence; 
    
    private double initConfidence=(double)0.5;
    private double reinforce_rate=(double)0.5;
    private double penalize_rate=(double)0.2;
    private double decay_rate=(double)0.0005;
    private double threshold=(double)0.01;
    private int    capacity=9999;
    
    private double beta=(double)1.0;
    private double epilson=(double)0.001;
    private double gamma[]={(double)1.0, (double)1.0,(double)1.0,(double)0.0};    
    
    // Action enumeration
     
    private double alpha[]={(double)0.1,(double)0.1,(double)0.1};
    private double b_rho[]={(double)0.2,(double)0.2,(double)0.5,(double)0.0}; // fuzzy ART baseline vigilances
    private double p_rho[]={(double)0.0,(double)0.0,(double)0.0,(double)0.0}; // fuzzy ART performance vigilances
    
    // Direct Access
	/*
	private double alpha[]={(double)0.001,  (double)0.001, (double)0.001, (double)0.001};
	private double b_rho[]={(double)0.25, (double)0.1, (double)0.5, (double)0.0}; // fuzzy ART baseline vigilances   
	private double p_rho[]={(double)0.25, (double)0.1, (double)0.5, (double)0.0}; // fuzzy ART performance vigilances
	*/
    // ART 2 Parameter Setting
	/*    
    private double beta=(double)0.5; 
    private double b_rho[]={(double)0.5,(double)0.2,(double)0.0,(double)0.0};
	*/

    private boolean end_state;
 
    private int agentID;
    private int max_step;
    private int step;
    private int currentBearing;
    private int targetBearing;
    private int [][] path;
    private int [] current;
    
    public static boolean forgetting =false;    
    public static boolean INTERFLAG  =false;
    public static boolean detect_loop=false;
    public static boolean look_ahead =false;
    public static boolean Trace=true;
       
    private NumberFormat df = NumberFormat.getInstance();   //返回当前默认语言环境的通用数值格式
		
    public FALCON ( int av_num ) {
    	
    	df.setMaximumFractionDigits (1);    //返回数的小数部分所允许的最大位数
    	
        agentID = av_num;
        numInput = new int[numSpace];   //// numSpace:0-State 1-Action 2-Reward 3-New State
        numInput[0] = numSonarInput+numAVSonarInput+numBearingInput+numRangeInput;
        numInput[1] = numAction;
        numInput[2] = numReward;
        numInput[3] = numInput[0];
        
        activityF1 = new double[numSpace][];
        for (int i=0; i<numSpace; i++)
            activityF1[i] = new double[numInput[i]];
            
        numCode = 0;
        newCode = new boolean[numCode+1];
        newCode[0] = true;
        
        confidence = new double[numCode+1];
        confidence[0] = initConfidence;
        
        activityF2 = new double[numCode+1];
        
        weight = new double[numCode+1][][];
        for (int j=0; j<=numCode; j++) {
            weight[j] = new double[numSpace][];
            for (int k=0; k<numSpace; k++) {
                weight[j][k] = new double[numInput[k]];
                for (int i=0; i<numInput[k]; i++)
                    weight[j][k][i] = (double)1.0;
            }
        }
        end_state = false;

        current = new int[2];
    }
    
    public void setParameters (int AVTYPE, boolean immediateReward) {

		if (AVTYPE==RFALCON) {
    		QEpsilonDecay = (double)0.00000;
    		QEpsilon      = (double)0.00000;
    	}
		else { //  QEpsilonDecay rate for TD-FALCON    
			QEpsilonDecay = (double)0.00050;
			QEpsilon      = (double)0.50000;
    	}
    	
    	if (immediateReward)
    		QGamma = (double) 0.5;
    	else
    		QGamma = (double) 0.9;    	
    }
    
    public void stop()
    {
        end_state = true;
    }

    public void checkAgent (String outfile) {
        PrintWriter pw_agent=null;
        boolean invalid;
        
        try {
            pw_agent = new PrintWriter (new FileOutputStream(outfile),true);
        } catch (IOException ex) {}
        
        pw_agent.println ("Number of Codes : "+numCode);
        for (int j=0; j<numCode; j++) {
            invalid=false;
            for (int i=0; i<numInput[ACTION]; i++)
                if (weight[j][0][i]==1 && weight[j][ACTION][i]==1)
                    invalid=true;

            if (invalid) {
                pw_agent.println ("Code "+j);
                for (int k=0; k<numSpace; k++) {
                    pw_agent.print ("Space "+k+" : ");
                    for (int i=0; i<numInput[k]-1; i++)
                        pw_agent.print (weight[j][k][i]+", ");
                    pw_agent.println (weight[j][k][numInput[k]-1]);
                }
            }
        }
        if (pw_agent!=null)
            pw_agent.close ();
    }
    
    public void clean() {
        int numClean=0;
        for (int j=0; j<numCode; j++)
            for (int i=0; i<numInput[ACTION]; i++)
                if (weight[j][0][i]==1 && weight[j][ACTION][i]==1) {
                    newCode[j]=true;
                    numClean++;
                }
        if (numClean>0)
            System.out.println (numClean+" bad code(s) removed.");      
    }
    
    public void saveAgent (String outfile) {
        PrintWriter pw_agent=null;
        
        try {
            pw_agent = new PrintWriter (new FileOutputStream(outfile),true);
        } catch (IOException ex) {}
        
        pw_agent.println ("Number of Codes : "+numCode);
        for (int j=0; j<=numCode; j++) {
            pw_agent.println ("Code "+j);
            for (int k=0; k<numSpace; k++) {
                pw_agent.print ("Space "+k+" : ");
                for (int i=0; i<numInput[k]-1; i++)
                    pw_agent.print (weight[j][k][i]+", ");
                pw_agent.println (weight[j][k][numInput[k]-1]);
            }
        }
        if (pw_agent!=null)
            pw_agent.close ();
    }
                    
    public int getNumCode()
    {
        return( numCode );
    }
        
    public int getCapacity () {
        return (capacity);
    }
    
    public void setTrace (boolean t) {
        Trace = t;
    }
    
    public void setPrevReward (double r) {
        prevReward = r;
    }
        
    public double getPrevReward () {
        return (prevReward);
    }
        
    public void createNewCode () {
        numCode++;
        
        activityF2 = new double[numCode+1];

        boolean[] new_newCode = new boolean[numCode+1];
        for (int j=0; j<numCode; j++)
            new_newCode[j] = newCode[j];
        new_newCode[numCode] = true;
        newCode = new_newCode;
        
        double[] new_confidence = new double[numCode+1];
        for (int j=0; j<numCode; j++)
            new_confidence[j] = confidence[j];
        new_confidence[numCode] = initConfidence;
        confidence = new_confidence;
        
        double[][][] new_weight = new double[numCode+1][][];
        for (int j=0; j<numCode; j++)
            new_weight[j] = weight[j];
            
        new_weight[numCode] = new double[numSpace][];
        for (int k=0; k<numSpace; k++) {
            new_weight[numCode][k] = new double[numInput[k]];
            for (int i=0; i<numInput[k]; i++)
                new_weight[numCode][k][i] = (double)1.0;
        }
        weight = new_weight;
    }
    
    public void reinforce () {
        confidence[J] += ((double)1.0-confidence[J])*reinforce_rate;
    }
        
    public void penalize () {
        confidence[J] -= confidence[J]*penalize_rate;
    }
    
    public void decay () {
        for (int j=0; j<numCode; j++)
            confidence[j] -= confidence[j]*decay_rate;
    }

    public void prune () {
        for (int j=0; j<numCode; j++)
            if (confidence[j]<threshold)
                newCode[j]=true;
    }
            
    public void purge () {
        int numPurge=0;
        
        for (int j=0; j<numCode; j++)
            if (newCode[j]==true)
                numPurge++;
        
        if (numPurge>0) {
            double[][][] new_weight = new double[numCode-numPurge+1][][];
            boolean[] new_newCode = new boolean[numCode-numPurge+1];
            double[]   new_confidence = new double[numCode-numPurge+1];
        
            System.out.print ("Total of "+numCode+" rule(s) created. ");
            int k=0;
            for (int j=0; j<numCode; j++)
                if (newCode[j]==false) {        
                    new_weight[k] = weight[j];
                    new_newCode[k] = newCode[j];
                    new_confidence[k] = confidence[j];  
                    k++;
                }
            new_weight[numCode-numPurge] = weight[numCode];
            new_newCode[numCode-numPurge] = newCode[numCode];
            new_confidence[numCode-numPurge] = confidence[numCode];
                
            weight = new_weight;
            newCode = new_newCode;
            confidence = confidence;
            
            numCode -= numPurge;
            activityF2 = new double[numCode+1];
            System.out.println (numPurge+" rule(s) purged.");
        }
    }
    
    public void setState( double [] sonar, double [] av_sonar, int bearing, double range ) 
    {
        int index;

        for( int i = 0; i < ( numSonarInput / 2 ); i++ ) 
        {
            activityF1[0][i] = sonar[i];
            activityF1[0][i+numSonarInput/2] = 1 - sonar[i];
        }
        index = numSonarInput;

        for( int i = 0; i < ( numAVSonarInput / 2 ); i++ ) 
        {
            activityF1[0][index+i] = av_sonar[i];
            activityF1[0][index+i+numSonarInput/2] = 1 - av_sonar[i];
        }
        index += numAVSonarInput;

        for( int i = 0; i < numBearingInput; i++ )
            activityF1[0][index+i] = (double)0.0;
        activityF1[0][index+bearing] = (double)1.0;
        index += numBearingInput;

        for( int i = 0; i < ( numRangeInput / 2 ); i++ )
        {
            activityF1[0][index+i] = range;
            activityF1[0][index+i+(numRangeInput/2)] = 1-range;
        }
    }
        
    public void initAction () {
        for (int i=0; i<numInput[ACTION]; i++)
            activityF1[ACTION][i] = 1;
    }

    public void init_path( int maxstep)
    {
        int k;

        max_step = maxstep;
        step = 0;
        currentBearing = 0;
        path = new int[max_step+1][2];
 /*       for( k = 0; k < 2; k++ )
        {
            current[k] = 0;
            path[step][k] = 0;
        }
*/    }

    public void resetAction () {
        for (int i=0; i<numInput[ACTION]; i++)
            activityF1[ACTION][i] = 1-activityF1[ACTION][i];
    }
    
    public void setAction (int action) {
        for (int i=0; i<numInput[ACTION]; i++)
            activityF1[ACTION][i] = 0;
        activityF1[ACTION][action] = (double)1.0;
    }
        
    public void setReward (double r) {
        activityF1[REWARD][0] = r;
        activityF1[REWARD][1] = 1-r;
    }
    
    public void initReward () {
        activityF1[REWARD][0] = 1;
        activityF1[REWARD][1] = 1;
    }
    
    public void setNewState( double [] sonar, double [] av_sonar, int bearing, double range ) 
    {
        int index;

        for( int i = 0; i < numSonarInput/2; i++ ) 
        {
            activityF1[NEWSTATE][i] = sonar[i];
            activityF1[NEWSTATE][i+(numSonarInput/2)] = 1 - sonar[i];
        }
        index = numSonarInput;

        for( int i = 0; i < ( numAVSonarInput / 2 ); i++ ) 
        {
            activityF1[NEWSTATE][i] = av_sonar[i];
            activityF1[NEWSTATE][i+numSonarInput/2] = 1 - av_sonar[i];
        }
        index += numAVSonarInput;

        for( int i = 0; i < numBearingInput; i++ )
            activityF1[NEWSTATE][index+i] = (double)0.0;
        activityF1[NEWSTATE][index+bearing] = (double)1.0;
        index += numBearingInput;

        for( int i = 0; i < ( numRangeInput / 2 ); i++ )
        {
            activityF1[NEWSTATE][index+i] = range;
            activityF1[NEWSTATE][index+i+(numRangeInput/2)] = 1-range;
        }
    }
        
    public void computeChoice (int type, int numSpace) {
        double top, bottom;
        //Predicting
        if (type==FUZZYART) {
            for (int j=0; j<=numCode; j++) {
                activityF2[j] = (double)0.0;
                for (int k=0; k<numSpace; k++)  //Code activation
        //        	if (gamma[k]>0.0)
                {
                    top = 0;
                    bottom = (double)alpha[k];
                    for (int i=0; i<numInput[k]; i++) {
                        top += Math.min (activityF1[k][i],weight[j][k][i]); //fuzzy AND operation
                        bottom += weight[j][k][i];
                    }
//                    activityF2[j] *= (double)(top/bottom);  // product rule, does not work
                    activityF2[j] += gamma[k]*(double)(top/bottom); //Code activation
                }
//              System.out.println( "F["+j+"] = " + activityF2[j] );
            }
        } 
        else if (type==ART2) {
            for (int j=0; j<=numCode; j++) {
                activityF2[j] = (double)0.0;
                for (int k=0; k<numSpace; k++) {
                    top = 0;
                    for (int i=0; i<numInput[k]; i++)
                        top += activityF1[k][i]*weight[j][k][i];
                    top /= numInput[k];
                    activityF2[j] += top;
                }
            }
        }
    }    

    public int doChoice () {
        double max_act=(double)-1.0;
        int   c=-1;
        
        for (int j=0; j<=numCode; j++)
            if (activityF2[j]>max_act) {
                max_act = activityF2[j];
                c = j;
            }
        return (c);
    }
    
    public boolean isNull (double[] x, int n) {
        for (int i=0; i<n; i++) 
            if (x[i]!=0) return (false);
        return (true);
    }
    
    public double doMatch (int k, int j) {  //Learning：Template matching

        double m=(double)0.0;
        double denominator = (double)0.0; //分母
        
        if (isNull(activityF1[k],numInput[k]))
            return (1);
            
        for (int i=0; i<numInput[k]; i++) {
            m += Math.min (activityF1[k][i], weight[j][k][i]); //fuzzy AND operation
            denominator += activityF1[k][i];
        }
//      System.out.println ("Code "+j+ " match "+m/denominator);
        if (denominator==0)
            return (1);
        return (m/denominator);
    }
    
    public void doComplete (int j, int k) {
        for (int i=0; i<numInput[k]; i++)
            activityF1[k][i] = weight[j][k][i];
    }

    public void doInhibit (int j, int k) {
        for (int i=0; i<numInput[k]; i++)
            if (weight[j][k][i]==1)
                activityF1[k][i] = 0;
    }
        
    public int doSelect (int k) {
        int   winner=0;
        double max_act=0;
                    
        for (int i=0; i<numInput[k]; i++) {
            if (activityF1[k][i]>max_act) {
                max_act = activityF1[k][i];
                winner = i;
            }
        }

        for (int i=0; i<numInput[k]; i++)
            activityF1[k][i] = 0;
        activityF1[k][winner] = 1;
        return(winner);
    }
            
    public void doLearn(int J, int type) {
        double rate;
        
        if (!newCode[J] || numCode<capacity) {

        	if (newCode[J]) rate=1;
        	else rate=beta; //*Math.abs(r-reward);
            
	        for (int k=0; k<numSpace; k++) {
            for (int i=0; i<numInput[k]; i++) {
                if (type==FUZZYART)
                    weight[J][k][i] = (1-rate)*weight[J][k][i] +
                        rate*Math.min(weight[J][k][i],activityF1[k][i]);
                else if (type==ART2)
                    weight[J][k][i] = (1-rate)*weight[J][k][i] +
                        rate*activityF1[k][i];
    	        }
        	}
        
        	if (newCode[J]) {
            	newCode[J]=false;
            	createNewCode ();
        	}
        }
    }

    public void doOverwrite(int J) {

        for (int k=0; k<numSpace; k++)
        {
            for (int i=0; i<numInput[k]; i++)
                weight[J][k][i] = activityF1[k][i];
        }
    }

    public void displayActivity(int k) {
        System.out.print ("Space "+k+" : ");
        for (int i=0; i<numInput[k]-1; i++)
            System.out.print (df.format(activityF1[k][i])+", ");
        System.out.println (df.format(activityF1[k][numInput[k]-1]));      
    }

    public void displayActivity2( PrintWriter pw, int k ) 
    {
        pw.print ( "AV" + agentID + " Space "+k+" : " );
        for (int i=0; i<numInput[k]-1; i++)
            pw.print (df.format(activityF1[k][i])+", ");
        pw.println (df.format(activityF1[k][numInput[k]-1]));      
    }

    public void displayVector(String s, double[] x, int n) {
        System.out.print (s+ " : ");
        for (int i=0; i<n-1; i++)
            System.out.print (df.format(x[i])+", ");
        System.out.println (df.format(x[n-1]));
    }

    public void displayState (String s, double[] x, int n) {
        System.out.print (s+ "   Sonar: [");
        int index=0;
        for (int i=0; i<numSonarInput; i++)
            System.out.print (df.format(x[index+i])+", ");
        System.out.print (df.format(x[index+numSonarInput-1]));
        
        System.out.println ("]");
        System.out.print ("TargetBearing: [");
        index=numSonarInput;
        for (int i=0; i<numBearingInput; i++)
            System.out.print (df.format(x[index+i])+", ");
        System.out.println (df.format(x[index+numBearingInput-1]) + "]");
                
    }                    
    public void displayVector2( PrintWriter pw, String s, double[] x, int n ) 
    {
        pw.print( "AV" + agentID + " " + s + " : " );
        for (int i=0; i<n-1; i++)
            pw.print (df.format(x[i])+", ");
        pw.println (df.format(x[n-1]));
    }
                    
    public double doSearchQValue(int mode, int type) {
        boolean reset=true, perfectMismatch=false;
        double     QValue=(double)0.0;
        double[] rho = new double[4];
        double[] match = new double[4];
        
        if (mode==INSERT)
            for (int k=0; k<numSpace; k++)
                rho[k] = 1;
        else if (mode==LEARN)
            for (int k=0; k<numSpace; k++)
                rho[k] = b_rho[k];
        else if (mode==PERFORM)
            for (int k=0; k<numSpace; k++)
                rho[k] = p_rho[k];

//        System.out.println ("Running searchQValue:");
        computeChoice(type,2); //map from state action to reward
    
        while (reset && !perfectMismatch) {
            reset = false;
            J = doChoice (); //Code competition
            for (int k = 0; k < numSpace; k++ )
                match[k] = doMatch(k,J);    //Learning：Template matching
            if (match[CURSTATE]<rho[CURSTATE]||match[ACTION]<rho[ACTION]||match[REWARD]<rho[REWARD]) {
                if (match[CURSTATE]==1) {
                    perfectMismatch=true;
                    if (Trace) System.out.println ("Perfect mismatch. Overwrite code "+J);
                }
                else {
                    activityF2[J] = (double)-1.0;
                    reset = true;
                
                    for (int k=0; k<1; k++) // raise vigilance of State
                        if (match[k]>rho[k])
                            rho[k] = Math.min (match[k]+epilson,1);
                }
            }   
        }
        if (mode==PERFORM) {
            doComplete (J,REWARD);
            if(activityF1[REWARD][0]==activityF1[REWARD][1] && activityF1[REWARD][0]==1){ //initialize Q value
                if (INTERFLAG)      QValue= (double)initialQ;
                else                QValue= (double)initialQ;
            }
            else
                QValue = activityF1[REWARD][0];
        }   
        else if (mode==LEARN) {
            if (!perfectMismatch) doLearn(J,type);
            else doOverwrite (J);
        }
        return (QValue);
    }
    
    public double getMaxQValue( int method, boolean train, Maze maze )
    {
    	int QLEARNING=0;
     	int SARSA=1;
     	double Q=(double)0.0;
        
        if( maze.isHitMine( agentID ) )   //case hit mine
            Q=0.0;
        else if( maze.isHitTarget( agentID ) )
            Q=1.0; //case reach target
		else {            
        	if(method==QLEARNING){                   //q learning
            	for(int i=0;i<numAction;i++){
                	setAction(i);
                	double tmp_Q=doSearchQValue(PERFORM,FUZZYART);              
                	if(tmp_Q>Q) Q=tmp_Q;
            	}
            } 
            else {                               //sarsa
            	int next_a = doSelectAction( train, maze );
            	setAction(next_a);  // set action
	            Q=doSearchQValue(PERFORM,FUZZYART);
	        }
        }       

        return Q;
    }
    
	public int doSearchAction(int mode, int type) {
		boolean reset=true, perfectMismatch=false;
		int     action=0;
		double[] rho   = new double[4];
		double[] match = new double[4];
		
		if (mode==INSERT)
			for (int k=0; k<numSpace; k++)
				rho[k] = 1;
		else if (mode==LEARN)
			for (int k=0; k<numSpace; k++)
				rho[k] = b_rho[k];
		else if (mode==PERFORM)
			for (int k=0; k<numSpace; k++)
				rho[k] = p_rho[k];

//        System.out.println ("Running searchAction");
		if (Trace) {
			System.out.println ("\nInput activities:");
			displayState("STATE", activityF1[CURSTATE], numInput[CURSTATE]);
			displayActivity(ACTION);
			displayActivity(REWARD);
		}
		
/*		if (mode==LEARN) {			
	        if (Trace) System.out.println ("reward="+r+ " prev="+reward);
			if (r==0 || r<=reward) {
  				if (Trace) System.out.println ("reset action");
  				for (int i=0; i<numInput[ACTION]; i++)
					activityF1[ACTION][i] = 1-activityF1[ACTION][i];
  			}
  			reward = r;			
  		}
*/  		
  		computeChoice(type,1);  /* 1 - choice function is computed based on state only */
								/* 3 - choice function is computed based on state, action, and value */
								
		while (reset && !perfectMismatch) {
			reset = false;
			J = doChoice ();
			for (int k=0; k<numSpace; k++)
				match[k] = doMatch(k,J);
			if (Trace) {
				System.out.println ("winner = "+J);
				displayState ("weight[J][STATE] ",weight[J][CURSTATE],numInput[CURSTATE]);
				displayVector ("weight[J][ACTION]",weight[J][ACTION],numInput[ACTION]);
				displayVector ("weight[J][REWARD]",weight[J][REWARD],numInput[REWARD]);				
				System.out.println ("Winner "+J+" act " + df.format(activityF2[J]) + 
									" match[State] = "  + df.format(match[CURSTATE]) +
									" match[action] = " + df.format(match[ACTION]) +
									" match[reward] = " + df.format(match[REWARD]));
			}
			
			// Checking match in all channels		
			if (match[CURSTATE]<rho[CURSTATE]||match[ACTION]<rho[ACTION]||match[REWARD]<rho[REWARD]) {
				if (match[CURSTATE]==1) {
					perfectMismatch=true;
					if (Trace) System.out.println ("Perfect mismatch. Overwrite code "+J);
				}
				else {
					activityF2[J] = (float)-1.0;
					if (Trace) System.out.println ("Reset Winner "+J+" rho[State] "+rho[CURSTATE]
											+" rho[Action] "+rho[ACTION]+" rho[Reward] "+rho[REWARD]);				
					reset = true;
				
					for (int k=0; k<1; k++) // raise vigilance of State only
						if (match[k]>rho[k])
							rho[k] = Math.min (match[k]+epilson,1);
				}
			}	
		}
		
		if (mode==PERFORM) {
			if (newCode[J]) action= -1;
			else {
				doComplete (J,ACTION);
				action = doSelect (ACTION);
			}
		}	
		else if (mode==LEARN) {
			if (!perfectMismatch) doLearn(J,type);
			else doOverwrite (J);
		}
		
		return (action);
	}

    private int loop_path()
    {
        int k;
        
        for( k = ( step - 1 ); k >= 0; k-- )
            if( ( current[0] == path[k][0] ) && ( current[1] == path[k][1] ) )
                return( k );
        return( -1 );
    }

    private int get_except_action()
    {
        int rep_step;
        int a;
        int [] new_pos;

        new_pos = new int[2];
        rep_step = loop_path();
        if( rep_step < 0 )
            return( -1 );
        for( a = 0; a < numAction; a++ )
        {
            virtual_move( a - 2, new_pos );
            if( ( new_pos[0] == path[rep_step+1][0] ) && ( new_pos[1] == path[rep_step+1][1] ) )
                return( a );
        }
        return( -1 );
    }

	public int doSelectAction (boolean train, Maze maze){
		
		double[] qValues=new double[numAction];
		int selectedAction = -1;
		
			
		//get qValues for all available actions
		for(int i=0;i<numAction;i++){
			setAction(i);
			qValues[i]=doSearchQValue(PERFORM,FUZZYART);
		}
				    
	    double maxQ = -Double.MAX_VALUE;
	    int[] doubleValues = new int[qValues.length];
	    int maxDV = 0;
	    
	    //Explore
		if ( Math.random() < QEpsilon && train==true ) {
			selectedAction = -1;
	    }
	    else {
		    
			for( int action = 0 ; action < qValues.length ; action++ ) {
			  /*  if(maze.nextReward(action-2)>0.5){ //add in rules
			    	selectedAction = action;
			    	maxDV=0;
			    	break;
			    } */
			    if( qValues[action] > maxQ ) {
					selectedAction = action;
					maxQ = qValues[action];
					maxDV = 0;
					doubleValues[maxDV] = selectedAction;
			    }
			    else if( qValues[action] == maxQ ) {
					maxDV++;
					doubleValues[maxDV] = action; 
			    }
			}
			
			if( maxDV > 0 ) {
			    int randomIndex = (int) ( Math.random() * ( maxDV + 1 ) );
			    selectedAction = doubleValues[ randomIndex ];
			}
	    }
	    // Select random action if all qValues == 0 or exploring.
	    if ( selectedAction == -1 ) {
	    	if(Trace)
	    		System.out.println("random action selected!");
	
			selectedAction = (int) (Math.random() * qValues.length);
	    }
		return selectedAction;
	}    
	
	public int doSelectValidAction( boolean train, Maze maze ) //选择有效操作
    {
        double d;
        double[] qValues=new double[numAction];
        int selectedAction = -1;
        int except_action  = -1;
        int k;
        
//        if (detect_loop)           
//        except_action = get_except_action();

        //get qValues for all available actions

        int [] validActions = new int[qValues.length];
        int maxVA = 0;
        
        for( int i = 0; i < numAction; i++ ) {
        	if (maze.withinField (agentID, i-2)==false) //如果agent跑到边界外面
        	{
        		qValues[i] = -1.0;
//      			System.out.println ( "action " + i + " invalid");    
        	}
        	else {  //agent没有到边界外
	            setAction( i );     //设置动作
    	        qValues[i] = doSearchQValue( PERFORM, FUZZYART );   //计算q值
    	        validActions[maxVA] = i;
    	        maxVA++;
    	    }
        }
        
        if (maxVA==0)
        {
// 	        System.out.println ( "current = ("+current[0]+","+current[1]+")  Bearing = " + currentBearing);
// 	        System.out.println ( "*** No valid action *** ");
 	       
 	    	return (-1);
 	    }
 	               
            // Explore
        if( Math.random() < QEpsilon && train==true) {
        // Select random action if all qValues == 0 or exploring.
            if(Trace) 
            	System.out.println("random action selected!");
            int randomIndex = (int) (Math.random() * maxVA);
            selectedAction = validActions[randomIndex];;
        }
        else {
        	double maxQ = -Double.MAX_VALUE;
        	int [] doubleValues = new int[qValues.length];
        	int maxDV = 0;
        
            for( int vAction = 0 ; vAction < maxVA ; vAction++ ) 
            {
            	int action = validActions[vAction];
//            	System.out.print ( "action[" + action + "] = " + qValues[action]);
/*				if (detect_loop)
                	if( except_action == action )
                    	continue;

//           	System.out.println ( "   nextReward[" + action + "] = " + maze.nextReward (agentID, action-2));
           	    if (look_ahead)
	               	if( maze.nextReward (agentID, action-2) > 0.5) { //add in rules                        
                    	selectedAction = action;
                        maxDV=0;
                        break;
                	}
*/            
                if( qValues[action] > maxQ ) {
                    selectedAction = action;
                    maxQ = qValues[action];
                    doubleValues[maxDV] = selectedAction;
                    maxDV = 1;
                }
                else if( qValues[action] == maxQ ) {
                    doubleValues[maxDV] = action; 
                    maxDV++;
                }
            }

            if( maxDV > 1 ) {   // more than 1 with max value
                int randomIndex = (int) ( Math.random() * maxDV);
                selectedAction = doubleValues[ randomIndex ];
            }
//        	System.out.println ( "Best valid action is " + selectedAction + " with maxQ =" + maxQ);
        }
        
        if (selectedAction==-1)
   	        System.out.println ( "No action selected");

        return selectedAction;
    }

	// Direct Access
	public int doDirectAccessAction (int agt, boolean train, Maze maze)
    {   
        int selectedAction; // from 0 to 4
        
        if (agt!=agentID)
    	    System.out.println ( "ID not consistent");   	
    	    	
        // first try to select an action
        //setState (maze.getSonar(), (maze.getTargetBearing()-maze.getCurrentBearing()+10)%8);
		initAction();   // initialize action to all 1's
		setReward (1);  // search for actions with good reward


        if (Math.random() < QEpsilon ||
        	(selectedAction=doSearchAction (PERFORM, FUZZYART))==-1 || // no close match
        	maze.withinField (agt, selectedAction-2)==false) { // not valid action
 				
            if (Trace) 
            	System.out.println("random action selected!");
            	
        	int [] validActions = new int[numAction];
        	int maxVA = 0;
        
//            System.out.print ("Valid actions :");
            for( int i = 0; i < numAction; i++ )
        		if (maze.withinField (agt, i-2)) {   // valid action
//					System.out.print (" " + i);
	    	       	validActions[maxVA] = i;
    	        	maxVA++;
	        	}
//            System.out.println (" ");
                
			if (maxVA>0) {
				int randomIndex = (int) (Math.random() * maxVA);
	            selectedAction = validActions[randomIndex];
            } 
            else
            	selectedAction = -1;
        }
  //     	else
  // 	        System.out.println ( "Chosen valid action is " + selectedAction);            
        		

//        if (selectedAction==-1)
//   	        System.out.println ( "No action selected");   	    
//   	    if (maze.withinField (agt, selectedAction-2)==false)
//  	        System.out.println("WARNING: selectedaction " + selectedAction + " out of field");
   	                
        return selectedAction;
    }

    public int[] findKMax (double[] v, int n, int K) {
        int   temp;
        double tempf;
        int[] maxIndex = new int[K];
        int[] index = new int[n]; 

        for (int i=0; i<n; i++)
            index[i] = i;

        for (int k=0; k<K; k++) {
            for (int i=n-1; i>k; i--)
                if (v[i-1]<v[i]) {
                    tempf=v[i]; v[i]=v[i-1]; v[i-1]=tempf;
                    temp=index[i]; index[i]=index[i-1]; index[i-1]=temp;
                }
            maxIndex[k]=index[k];
        }
        return(maxIndex);
    }
    
    public boolean doCompleteKMax (int[] k_max) {
        int actualK, j;
        boolean predict=false;
        
        if (numCode<KMax)
            actualK=numCode;
        else
            actualK=KMax;
            
        for (int i=0; i<numInput[ACTION]; i++) {
            activityF1[ACTION][i] = 0;
            for (int k=0; k<actualK; k++) {
                j = k_max[k];
                if (activityF2[j]>0.9) {     // threshold of activity for predicting
                    activityF1[ACTION][i] += activityF2[j] * 
                    (weight[j][REWARD][0]*weight[j][ACTION][i]                  //good move
                               -(1-weight[j][REWARD][0])*weight[j][ACTION][i]); //bad move
                    predict=true;
                }
            }
        }
        return( predict );
    }
            
    public int doSelectDualAction(int type) {
        boolean reset=true;
        int     action=0;
        double[] rho = new double[4];
        double[] match = new double[4];
        int[]   k_max;
        
        for (int k=0; k<numSpace; k++)
            rho[k] = p_rho[k];
        
        if (Trace) {
            System.out.println ("Input activities");
            displayActivity(CURSTATE);
            displayActivity(ACTION);
            displayActivity(REWARD);
        }

        computeChoice(type,1);
//      for (int j=0; j<numCode; j++)
//          System.out.println ("F2["+j+"]= "+activityF2[j]);

        if (numCode>KMax) {
            k_max = findKMax (activityF2, numCode, KMax);
//          for (int j=0; j<K; j++)
//              System.out.println ("k_max["+j+"]= "+k_max[j]);
            if (!doCompleteKMax (k_max))
                J = doChoice();
        }
        else
            J = doChoice();
        
        action = doSelect (ACTION);
        return (action);
    }

    public void virtual_move( int a, int [] res ) {
                
    int k;
    int bearing = ( currentBearing + a + 8 ) % 8;

    res[0] = current[0];
    res[1] = current[1];

    switch( bearing )
                {
                        case 0:
                                res[1]--;
                                break;
                        case 1:
                                res[0]++;
                                res[1]--;
                                break;
                        case 2:
                                res[0]++;
                                break;
                        case 3:
                                res[0]++;
                                res[1]++;
                                break;
                        case 4:
                                res[1]++;
                                break;
                        case 5:
                                res[0]--;
                                res[1]++;
                                break;
                        case 6:
                                res[0]--;
                                break;
                        case 7:
                                res[0]--;
                                res[1]--;
                                break;
                        default:
                                break;
                }
                return;
        }

    public void turn( int d )
    {
        currentBearing = ( currentBearing + d + 8 ) % 8;
	}

    public void move( int a, boolean succ ) 
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

        switch( currentBearing ) {
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
                return;
        }

    int get_J()
    {
        return( J );
    }   
    
     // dummy methods required by abstract AGENT class
    
    public void doLearnACN () {};
	public void setprev_J () {};    
	public double computeJ (Maze maze) {return 0.0;}
	public void setNextJ (double J) {};    
} 
                                