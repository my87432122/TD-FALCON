import java.io.*;
import java.lang.*;
import java.text.*;

public class FALCON extends AGENT {
    final   int   numSpace=4; // 0-State 1-Action 2-Reward 3-New State 
    final   int   numSonarInput=10;
    final   int   numAVSonarInput=0;//就一个agent所以没有AVSonarInput
    final   int   numBearingInput=8;
    final   int   numRangeInput=0;//暂时不知道这个numRangeInput是什么意思，难道是不同的agent之间的距离?
    final   int   numAction=5;
    final   int   numReward=2;
    final   int   complementCoding=1;
    
    final   int   CURSTATE=0;
    final   int   ACTION  =1;
    final   int   REWARD  =2;
    final   int   NEWSTATE=3;
    
    final static int RFALCON =0;
    final static int TDFALCON=1;
    
    final   int   FUZZYART=0;//模糊ART神经网络
    final   int   ART2    =1;
    
    final   int   PERFORM =0;
    final   int   LEARN   =1;
    final   int   INSERT  =2;
    
    private int[]      numInput;//
    private int        numCode;//暂时没懂什么意思
    private double     prevReward=0;
    private double[][] activityF1;//FAlcon的第一层，是一个二维，结点一共有三个输入Field，每个区域的大小分别为18，5，2
    private double[]   activityF2;//Falcon的第二层，是一个一维的(y1,y2,y3......)，大小为F2层所有的结点数
    private double[][][] weight; 
    private int        J;//这个是啥玩意???
    private int        KMax=3;
    private boolean[]  newCode;
    private double[]   confidence; 
    
    private double initConfidence=(double)0.5;
    private double reinforce_rate=(double)0.5;
    private double penalize_rate=(double)0.2;//处罚率
    private double decay_rate=(double)0.0005;//衰减率
    private double threshold=(double)0.01;//阈值
    private int    capacity=9999;//最大的结点数
    
    private double beta=(double)1.0;//β,这是ART权重参数W的学习率
    private double epilson=(double)0.001;//ε，用于增大Field1 的警戒参数，使其略大于 Mj-ck1
    private double gamma[]={(double)1.0, (double)1.0,(double)1.0,(double)0.0};//γ 
    
    // Action enumeration，动作枚举
     
    private double alpha[]={(double)0.1,(double)0.1,(double)0.1};//这个是αk，选择参数 choice parameters
    private double b_rho[]={(double)0.2,(double)0.2,(double)0.5,(double)0.0}; // fuzzy ART baseline vigilances  β_ρ  警戒参数
    private double p_rho[]={(double)0.0,(double)0.0,(double)0.0,(double)0.0}; // fuzzy ART performance vigilances  p_ρ ???这个参数目前还不知道什么意思
    
    // Direct Access
	/*
	private double alpha[]={(double)0.001,  (double)0.001, (double)0.001, (double)0.001};
	private double b_rho[]={(double)0.25, (double)0.1, (double)0.5, (double)0.0}; // fuzzy ART baseline vigilances   
	private double p_rho[]={(double)0.25, (double)0.1, (double)0.5, (double)0.0}; // fuzzy ART performance vigilances
	*/
    // ART 2 Parameter Setting  ART2 是 ART1 的升级版，可以处理灰度输入
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
    public static boolean detect_loop=false; //回路检测
    public static boolean look_ahead =false;
    public static boolean Trace=true; //是否记录路径
       
    private NumberFormat df = NumberFormat.getInstance();   //返回当前默认语言环境的通用数值格式,表示数字的格式化类，即：可以按照本地的风格习惯进行数字的显示。getInstance是一个函数，在java中，可以使用这种方式使用单例模式创建类的实例，所谓单例模式就是一个类有且只有一个实例
		
    public FALCON ( int av_num ) {//初始化函数
    	
    	df.setMaximumFractionDigits (1);    //返回数的小数部分所允许的最大位数
    	
        agentID = av_num;
        numInput = new int[numSpace];   // numSpace:0-State 1-Action 2-Reward 3-New State 
        numInput[0] = numSonarInput+numAVSonarInput+numBearingInput+numRangeInput;//10+0+8+0
        numInput[1] = numAction;//5
        numInput[2] = numReward;//2
        numInput[3] = numInput[0];

        activityF1 = new double[numSpace][];//结点F共有四部分，0-State 1-Action 2-Reward 3-New State ，每一部分的长度分别为 18 5 2 0
        for (int i=0; i<numSpace; i++)
            activityF1[i] = new double[numInput[i]];
//————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————2020.1.2——————————————————————————————————————————
        numCode = 0;//暂时没懂numCode是什么意思, 意思难道是记录目前F2层有多少结点? 
        newCode = new boolean[numCode+1];//newCode是一个布尔值
        newCode[0] = true;//?
        
        confidence = new double[numCode+1];//?
        confidence[0] = initConfidence;//initConfidence=0.5,initConfidence是Q值的意思?
        
        activityF2 = new double[numCode+1];//第二层初始化一个unCommited的结点
        
        weight = new double[numCode+1][][];//权重向量[F2节点的index][numSpace=4][]，W(ck)(j),ck中的k取值为1，2，3，指的是F1层的三个Field; j为F2层的index; 初始化只有一个uncommitted结点
        for (int j=0; j<=numCode; j++) {
            weight[j] = new double[numSpace][];
            for (int k=0; k<numSpace; k++) {
                weight[j][k] = new double[numInput[k]];//w[j][k][18,5,2,18]
                for (int i=0; i<numInput[k]; i++)//对于uncommitted结点，每个[j][k][i]都为1
                    weight[j][k][i] = (double)1.0;
            }
        }
        end_state = false;//初始化endState=false

        current = new int[2];//初始化?? current是什么???
    }
    
    public void setParameters (int AVTYPE, boolean immediateReward) {

		if (AVTYPE==RFALCON) {//如果用的是RFALCON，那么就不需要ε-greedy
    		QEpsilonDecay = (double)0.00000;
    		QEpsilon      = (double)0.00000;
    	}
		else { //  QEpsilonDecay rate for TD-FALCON    
			QEpsilonDecay = (double)0.00050;
			QEpsilon      = (double)0.50000;
    	}
    	
    	if (immediateReward)//如果是及时奖励,Discount factor γ=0.5
    		QGamma = (double) 0.5;
    	else
    		QGamma = (double) 0.9;    	
    }
    
    public void stop()//停止Falcon网络的更新
    {
        end_state = true;
    }

    public void checkAgent (String outfile) {
        PrintWriter pw_agent=null;//PrintWriter Java用于写出的类
        boolean invalid;
        
        try {
            pw_agent = new PrintWriter (new FileOutputStream(outfile),true);
        } catch (IOException ex) {}//用来捕获IO异常
        
        pw_agent.println ("Number of Codes : "+numCode);//把结点的个数写入rule.txt
        for (int j=0; j<numCode; j++) {
            invalid=false;
            for (int i=0; i<numInput[ACTION]; i++)
                if (weight[j][0][i]==1 && weight[j][ACTION][i]==1)//?没懂为什么要这样来判定无效的Node，这样的意思是该结点J代表在i方向上的State为1(离mines或墙很近)，并且还走这个方向的话，显然是很差劲的Node(因为一定会撞到雷上)
                    invalid=true;

            if (invalid) {
                pw_agent.println ("Code "+j);
                for (int k=0; k<numSpace; k++) {
                    pw_agent.print ("Space "+k+" : ");//print不会换行,println会换行
                    for (int i=0; i<numInput[k]-1; i++)
                        pw_agent.print (weight[j][k][i]+", ");
                    pw_agent.println (weight[j][k][numInput[k]-1]);//输出最后一位,并换行
                }
            }
        }
        if (pw_agent!=null)
            pw_agent.close ();
    }
    
    public void clean() {//清除这些不好的Node
        int numClean=0;
        for (int j=0; j<numCode; j++)
            for (int i=0; i<numInput[ACTION]; i++)
                if (weight[j][0][i]==1 && weight[j][ACTION][i]==1) {
                    newCode[j]=true;//把j结点置为Uncommitted Node
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
        newCode = new_newCode;//这么更新的意义何在????? 这一步就是因为新建了一个uncommitted 结点要把新节点加入到newcode中来
        
        double[] new_confidence = new double[numCode+1];
        for (int j=0; j<numCode; j++)
            new_confidence[j] = confidence[j];
        new_confidence[numCode] = initConfidence;
        confidence = new_confidence;//把新节点的Q值加入到confidence中
        
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
        confidence[J] += ((double)1.0-confidence[J])*reinforce_rate;//强化节点J，对于结点J，这个J被初始化为private int | Q = Q + (1 - Q) * α (α = 0.5)
    }
        
    public void penalize () {
        confidence[J] -= confidence[J]*penalize_rate;//惩罚结点J，Q = Q - Q * penalize_rate(penalize_rate=0.2)
    }
    
    public void decay () {
        for (int j=0; j<numCode; j++)
            confidence[j] -= confidence[j]*decay_rate;//随着时间流逝，所有结点的可信度都会衰减
    }

    public void prune () {//剪枝
        for (int j=0; j<numCode; j++)
            if (confidence[j]<threshold)//如果结点的可信度低于阈值，就把它取消，置为新节点
                newCode[j]=true;
    }
            
    public void purge () {//清洗
        int numPurge=0;//用于计算F2层中newCode为True的结点个数
        
        for (int j=0; j<numCode; j++)
            if (newCode[j]==true)
                numPurge++;
        
        if (numPurge>0) {
            double[][][] new_weight = new double[numCode-numPurge+1][][];//这些下标后面都有一个+1就是为了添加那个一直存在的uncommitted Node
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
            new_weight[numCode-numPurge] = weight[numCode];//把最后一个Uncommitted Node 加上
            new_newCode[numCode-numPurge] = newCode[numCode];
            new_confidence[numCode-numPurge] = confidence[numCode];
                
            weight = new_weight;
            newCode = new_newCode;
            confidence = confidence;
            
            numCode -= numPurge;//结点剪枝
            activityF2 = new double[numCode+1];
            System.out.println (numPurge+" rule(s) purged.");
        }
    }
// 2020.1.3>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    public void setState( double [] sonar, double [] av_sonar, int bearing, double range ) //State的输入 10 + 0 + (int)target的方向 + 0
    {
        int index;

        for( int i = 0; i < ( numSonarInput / 2 ); i++ ) //输入10个Sonar的信号，numSonarInput = 10
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
        index += numAVSonarInput;//没有AvSonar的输入

        for( int i = 0; i < numBearingInput; i++ )
            activityF1[0][index+i] = (double)0.0;
        activityF1[0][index+bearing] = (double)1.0;
        index += numBearingInput;

        for( int i = 0; i < ( numRangeInput / 2 ); i++ )//也没有RangeInput的输入
        {
            activityF1[0][index+i] = range;
            activityF1[0][index+i+(numRangeInput/2)] = 1-range;
        }
    }
        
    public void initAction () {//ACtion向量初始化为1
        for (int i=0; i<numInput[ACTION]; i++)
            activityF1[ACTION][i] = 1;
    }

    public void init_path( int maxstep)//path用来记录Agent行走的坐标
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

    public void resetAction () {//这种Rest意义何在??,把动作的值反转
        for (int i=0; i<numInput[ACTION]; i++)
            activityF1[ACTION][i] = 1-activityF1[ACTION][i];
    }
    
    public void setAction (int action) {//把当前动作设置为action
        for (int i=0; i<numInput[ACTION]; i++)
            activityF1[ACTION][i] = 0;
        activityF1[ACTION][action] = (double)1.0;
    }
        
    public void setReward (double r) {
        activityF1[REWARD][0] = r;
        activityF1[REWARD][1] = 1-r;
    }
    
    public void initReward () {//Reward全部初始化为1
        activityF1[REWARD][0] = 1;
        activityF1[REWARD][1] = 1;
    }
    
    public void setNewState( double [] sonar, double [] av_sonar, int bearing, double range ) //和SetState一样
    {
        int index;

        for( int i = 0; i < numSonarInput/2; i++ ) 
        {
            activityF1[NEWSTATE][i] = sonar[i]; //Newsate = 3 curstate = 0
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
        
    public void computeChoice (int type, int numSpace) {//type=0是FUZZART type=1是ART2,计算出F2层所有节点响应的选择函数，存入activityF2中
        double top, bottom;
        //Predicting numSpace的取值范围 1 - 3
        if (type==FUZZYART) { //FUZZYART = 0 
            for (int j=0; j<=numCode; j++) {
                activityF2[j] = (double)0.0;//activityF2=(针对输入层F1[][]，F2层所有结点的给出响应后的选择函数T)
                for (int k=0; k<numSpace; k++)  //Code activation，k:0->3 k从0到numSpace
        //        	if (gamma[k]>0.0)
                {
                    top = 0;//选择函数T的分子
                    bottom = (double)alpha[k];//选择函数T的分母
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
        else if (type==ART2) { //ART2 = 1
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

    public int doChoice () {//选出F2中响应函数最大的节点c
        double max_act=(double)-1.0;
        int   c=-1;
        
        for (int j=0; j<=numCode; j++)
            if (activityF2[j]>max_act) {
                max_act = activityF2[j];
                c = j;
            }
        return (c);
    }
    
    public boolean isNull (double[] x, int n) {//用来判断[]x是否全部为0
        for (int i=0; i<n; i++) 
            if (x[i]!=0) return (false);
        return (true);
    }
    
    public double doMatch (int k, int j) {  //Learning：Template matching,对第k个输入Field作匹配，依次产生m^j^c1, m^j^c2,m^j^c3

        double m=(double)0.0;
        double denominator = (double)0.0; //分母
        
        if (isNull(activityF1[k],numInput[k]))//如果Xck全部为0，那就返回匹配函数Mjck=1，这样做是为了防止分母为0
            return (1);
            
        for (int i=0; i<numInput[k]; i++) {
            m += Math.min (activityF1[k][i], weight[j][k][i]); //fuzzy AND operation
            denominator += activityF1[k][i];
        }
//      System.out.println ("Code "+j+ " match "+m/denominator);
        if (denominator==0)//再次防止分母为0
            return (1);
        return (m/denominator);
    }
    
    public void doComplete (int j, int k) {// ActivityF1[k]<<======weight[j][k]
        for (int i=0; i<numInput[k]; i++)
            activityF1[k][i] = weight[j][k][i];
    }

    public void doInhibit (int j, int k) {//inhibit 抑制?
        for (int i=0; i<numInput[k]; i++)
            if (weight[j][k][i]==1)
                activityF1[k][i] = 0;
    }
        
    public int doSelect (int k) {//选择ACtion Field 中获胜的动作，把最大可能的动作置为1, 其余的置为0,返回获胜者 winner
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
            
    public void doLearn(int J, int type) { //对最相似的节点J进行学习
        double rate;//学习率
        
        if (!newCode[J] || numCode<capacity) {//如果Jcommitted node || J 为 Uncommitted node 但是 当前容量还足够开拓新结点

        	if (newCode[J]) rate=1;//如果是新节点，那么就快速学习，学习率为1 ,即 Wj-ck=X-ck
        	else rate=beta; //*Math.abs(r-reward);β=1
            
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

    public void doOverwrite(int J) {//重写函数，对于J结点 w[j][k][i] <<===== activityF1[k][i]

        for (int k=0; k<numSpace; k++)
        {
            for (int i=0; i<numInput[k]; i++)
                weight[J][k][i] = activityF1[k][i];
        }
    }

    public void displayActivity(int k) {//输出ActivityF1[k]
        System.out.print ("Space "+k+" : ");
        for (int i=0; i<numInput[k]-1; i++)
            System.out.print (df.format(activityF1[k][i])+", ");
        System.out.println (df.format(activityF1[k][numInput[k]-1]));      
    }

    public void displayActivity2( PrintWriter pw, int k ) //把ActivityF1[k]写入文件中
    {
        pw.print ( "AV" + agentID + " Space "+k+" : " );
        for (int i=0; i<numInput[k]-1; i++)
            pw.print (df.format(activityF1[k][i])+", ");
        pw.println (df.format(activityF1[k][numInput[k]-1]));      
    }

    public void displayVector(String s, double[] x, int n) {//输出向量[]x
        System.out.print (s+ " : ");
        for (int i=0; i<n-1; i++)
            System.out.print (df.format(x[i])+", ");
        System.out.println (df.format(x[n-1]));
    }

    public void displayState (String s, double[] x, int n) {//输出状态State (10+8)
        System.out.print (s+ "   Sonar: [");
        int index=0;
        for (int i=0; i<numSonarInput; i++)//输出10个Sonar输入
            System.out.print (df.format(x[index+i])+", ");
        System.out.print (df.format(x[index+numSonarInput-1]));
        
        System.out.println ("]");
        System.out.print ("TargetBearing: [");
        index=numSonarInput;
        for (int i=0; i<numBearingInput; i++)
            System.out.print (df.format(x[index+i])+", ");
        System.out.println (df.format(x[index+numBearingInput-1]) + "]");
                
    }                    
    public void displayVector2( PrintWriter pw, String s, double[] x, int n ) //把向量写入文件
    {
        pw.print( "AV" + agentID + " " + s + " : " );
        for (int i=0; i<n-1; i++)
            pw.print (df.format(x[i])+", ");
        pw.println (df.format(x[n-1]));
    }
                    
    public double doSearchQValue(int mode, int type) {//according to state && action to map reward
        boolean reset=true, perfectMismatch=false;
        double     QValue=(double)0.0;
        double[] rho = new double[4]; //ρ[4] 存放警戒参数
        double[] match = new double[4]; //match[4]存放四个匹配函数
        
        if (mode==INSERT)//三种警戒参数 INSERT = 0
            for (int k=0; k<numSpace; k++)
                rho[k] = 1; // 1 1 1 1 
        else if (mode==LEARN) // LEARN = 1
            for (int k=0; k<numSpace; k++)
                rho[k] = b_rho[k]; // 0.2 0.2 0.5 0
        else if (mode==PERFORM) //predict阶段用不着警戒参数，Learning阶段才用 PERFORM = 2
            for (int k=0; k<numSpace; k++) 
                rho[k] = p_rho[k]; // 0 0 0 0 

//        System.out.println ("Running searchQValue:");
        computeChoice(type,2); //map from state action to reward |||| numSpace = 2 代表 reward
    
        while (reset && !perfectMismatch) {
            reset = false;
            J = doChoice (); //Code competition,函数返回获胜的结点即T值最大的点的Index J
            for (int k = 0; k < numSpace; k++ )
                match[k] = doMatch(k,J);    //Learning：Template matching, 把3 个 Field 的匹配函数 m 算出来存入 match[]中
            if (match[CURSTATE]<rho[CURSTATE]||match[ACTION]<rho[ACTION]||match[REWARD]<rho[REWARD]) {//如果三者之中有一个不满足警戒参数
                if (match[CURSTATE]==1) {//如果说match[0] = 1 则证明所有的结点都没能匹配上当前的状态，那这就是一个新状态
                    perfectMismatch=true;//完美不匹配
                    if (Trace) System.out.println ("Perfect mismatch. Overwrite code "+J);
                }
                else { //如果3个filed发生了不匹配并且当前获胜的节点J不是uncommitted Node
                    activityF2[J] = (double)-1.0;//把结点 J 排除在外, rechoose a node 
                    reset = true;
                
                    for (int k=0; k<1; k++) // raise vigilance of State 对 m^j^c0也就是Filed Curstate 对应的警戒参数提升epilson 
                        if (match[k]>rho[k])
                            rho[k] = Math.min (match[k]+epilson,1);
                }
            }   
        }
        if (mode==PERFORM) { //PERFORM阶段是 F2 层的权值对 F1层的参数产生影响，对应函数doComplate
            doComplete (J,REWARD);//把选中结点的权值W对应的Reward 分量赋值给 ActivityF1[Reward] F2 -> F1
            if(activityF1[REWARD][0]==activityF1[REWARD][1] && activityF1[REWARD][0]==1){ //initialize Q value，如果 选中的结点是 Uncommitted node 就会满足这个条件
                if (INTERFLAG)      QValue= (double)initialQ;//initialQ=0.5
                else                QValue= (double)initialQ;
            }
            else
                QValue = activityF1[REWARD][0];
        }   
        else if (mode==LEARN) { //LEARN阶段是 F1 层的权值对 F2 层的参数产生影响，对应函数doOverWrite
            if (!perfectMismatch) doLearn(J,type); //如果选中的节点J不是新节点就对节点J的权重进行学习
            else doOverwrite (J); //如果选中的节点J是新节点就直接把 F1 层的权重 -> F2层的新节点J
        }
        return (QValue);
    }
//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>2020.1.4>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    public double getMaxQValue ( int method, boolean train, Maze maze ) //输入参数为 Maze-地图类 method- Q(0) or Sarsa(1)  train-是否是训练模式
    { //函数作用应该是求出最大化的Q值
    	int QLEARNING=0; //Q-learning 和 sarsa 两种算法
     	int SARSA=1;
     	double Q=(double)0.0;
        
        if( maze.isHitMine( agentID ) )   //case hit mine agentID是Agent编号，每一个Agent都蕴含着一个FALCON网络
            Q=0.0;
        else if( maze.isHitTarget( agentID ) )
            Q=1.0; //case reach target
		else {            
        	if(method==QLEARNING){ //q learning TDerr = r + γmaxQ'(s',a') - Q(s,a) 因此需要根据当前状态求出下一状态和动作的最大Q值
            	for(int i=0;i<numAction;i++){ //numAction = 5 根据当前状态S2求出所有可选动作中Q值最大的动作，此时还处在状态S2还不知道真实地A2选择了哪个动作
                	setAction(i); //依次把F1层 Action Field 设置为 五个动作，查看每个动作能获得的Q值
                	double tmp_Q=doSearchQValue(PERFORM,FUZZYART); // perform 阶段是 F2对F1层产生影响，doSearchQvalue函数 根据 state && action 确定 reward             
                	if(tmp_Q>Q) Q=tmp_Q;
            	}
            } 
            else {                               //sarsa 根据state来选择节点J从而获得 J节点的Action
            	int next_a = doSelectAction( train, maze );  // 这一步做的是仅仅根据状态state选择动作 S1 -> A1 （1 - ε）概率选择Q最大的动作，ε概率随机选择动作
            	setAction(next_a);  // set action
	            Q=doSearchQValue(PERFORM,FUZZYART); //doSearchQValue 根据state和action 来选择节点J从而获得 J节点的Reward
	        }
        }       

        return Q;
    }
    
	public int doSearchAction(int mode, int type) { //根据 state 来映射动作
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
				doComplete (J,ACTION);//把选中结点的权值W对应的Action 分量赋值给 ActivityF1[Action] F2 -> F1
				action = doSelect (ACTION); //选择ActivityF1[Action]节点中最大的动作，并且获胜的动作取全部1，其余的均置为 0 
			}
		}	
		else if (mode==LEARN) {
			if (!perfectMismatch) doLearn(J,type);
			else doOverwrite (J);
		}
		
		return (action);
	}

    private int loop_path() //判断路径是否成换
    {
        int k;
        
        for( k = ( step - 1 ); k >= 0; k-- ) //step是agent当前已经走了多少步
            if( ( current[0] == path[k][0] ) && ( current[1] == path[k][1] ) ) //当前的坐标是否和之前的坐标重合
                return( k );
        return( -1 );
    }

    private int get_except_action() //如果当前的Agent成环转圈圈了，求出发生回路点的下一步所选择的动作
    {
        int rep_step;
        int a;
        int [] new_pos;

        new_pos = new int[2];
        rep_step = loop_path(); //记录下和当前节点成环的Path之前的节点
        if( rep_step < 0 )
            return( -1 );
        for( a = 0; a < numAction; a++ ) // 这一步应该是求出环路的交界点rep_Step的下一步所选择的动作
        {
            virtual_move( a - 2, new_pos ); //new_pos变成了current按照a动作移动后的位置
            if( ( new_pos[0] == path[rep_step+1][0] ) && ( new_pos[1] == path[rep_step+1][1] ) )
                return( a );
        }
        return( -1 );
    }

	public int doSelectAction (boolean train, Maze maze){ // 根据ε-greedy选择一个最可能的动作 sarsa算法中选择下一个s'的a' 输入参数为 train-是否是训练 maze-地图类
		
		double[] qValues=new double[numAction]; //numAction = 5
		int selectedAction = -1;
		
			
		//get qValues for all available actions
		for(int i=0;i<numAction;i++){
			setAction(i);
			qValues[i]=doSearchQValue(PERFORM,FUZZYART);
		}
				    
	    double maxQ = -Double.MAX_VALUE; //最小值
	    int[] doubleValues = new int[qValues.length];
	    int maxDV = 0; //代表有多个动作的Q值相同
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>2020.2.3>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	    //Explore
		if ( Math.random() < QEpsilon && train==true ) { //如果刚好随机到ε内 直接随机选择
			selectedAction = -1;
	    }
	    else { // ε-greddy
		    
			for( int action = 0 ; action < qValues.length ; action++ ) { //从所有备选动作中选择Q值最大的动作
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
			
			if( maxDV > 0 ) { //多个动作Q值相同且都为最大则随机选一个
			    int randomIndex = (int) ( Math.random() * ( maxDV + 1 ) );
			    selectedAction = doubleValues[ randomIndex ];
			}
	    }
	    // Select random action if all qValues == 0 or exploring.
	    if ( selectedAction == -1 ) {
	    	if(Trace)
	    		System.out.println("random action selected!");
	
			selectedAction = (int) (Math.random() * qValues.length); //如果是探索操作则随便选择一个动作
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
        	else {  //agent没有到边界外，在动作i对应的方向上还能继续移动
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
            int randomIndex = (int) (Math.random() * maxVA); //如果是探索操作，则在可以选择的动作maxVA里随机选择一个
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
        	(selectedAction=doSearchAction (PERFORM, FUZZYART))==-1 || // no close match，只能创建一个新节点
        	maze.withinField (agt, selectedAction-2)==false) { // not valid action，或者这个选择的新动作无法执行，否则会跑出地图外
 				
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

    public int[] findKMax (double[] v, int n, int K) { //寻找v中前k个最大值
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
    
    public boolean doCompleteKMax (int[] k_max) { //k_max数组里存了F2层中数值最大的3个Node的下标
        int actualK, j;
        boolean predict=false;
        
        if (numCode<KMax) //KMax = 3
            actualK=numCode;
        else
            actualK=KMax;
            
        for (int i=0; i<numInput[ACTION]; i++) { //五个动作ACtion，对于每个动作 i 如果 F2层的T函数的值大于0.9 就对F1层的Actin Field做更新
            activityF1[ACTION][i] = 0;
            for (int k=0; k<actualK; k++) { //k_max数组中的值
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
            
    public int doSelectDualAction(int type) { //双重(Dual)动作
        boolean reset=true;
        int     action=0;
        double[] rho = new double[4];
        double[] match = new double[4];
        int[]   k_max;
        
        for (int k=0; k<numSpace; k++)
            rho[k] = p_rho[k]; //0 0 0 0 
        
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
            k_max = findKMax (activityF2, numCode, KMax); //从F2层中选择T函数值最大的KMax(3)个Node的下标
//          for (int j=0; j<K; j++)
//              System.out.println ("k_max["+j+"]= "+k_max[j]);
            if (!doCompleteKMax (k_max) ) //如果该函数返回的是False
                J = doChoice(); //Code competition,函数返回获胜的结点即T值最大的点的Index J
        }
        else
            J = doChoice();
        
        action = doSelect (ACTION);
        return (action);
    }

    public void virtual_move( int a, int [] res ) { //虚拟移动，[]res传入的是new int[2] a是传入的 a - 2 a∈[0, 4] a - 2 就是做了下转化
    // 函数作用对当前的坐标current 和 当前朝向 bearing 计算经过 动作a后的朝向 并且 按当前方向移动一个单位            
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
                                
