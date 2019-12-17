abstract class AGENT {//Agent是一个抽象类
   // variable for Q learning
    
    public static double QAlpha        = (double)0.5;  //TD Learning rate α

//  default QGamma suitable for Immediate Reward Scheme
//	To be overriden for Delayed reward 
	public static double QGamma        = (double)0.1;	//Discount factor γ
    
    public static double minQEpsilon   = (double)0.00500;  //ε-greedy的ε的最小值，即衰减到这个值就不会再衰减了
    public static double initialQ      = (double)0.5;	//初始的Q函数的值
    
// 	default QEpsilonDecay and initialQEpsilon for TD-FALCON
// 	The values are to be override when using RFALCON, BPN and DNDP in the setParameters method     
    public static double QEpsilonDecay = (double)0.00050;//ε的衰减速率
    public static double QEpsilon      = (double)0.50000;//ε的初始值
           
    public static boolean direct_access=false;       
    public static boolean forgetting =false;   
    public static boolean INTERFLAG  =false;
 //   public static boolean detect_loop=false;
 //   public static boolean look_ahead =false;
    public static boolean Trace      =true;    
	
	abstract public void saveAgent  (String outfile);//用abstract关键字的是抽象类，只需要在父类声明，无需在父类实现。
	abstract public void checkAgent (String outfile);
	abstract public void setParameters (int AVTYPE, boolean ImmediateReward); //AV的类型和是否是immediate reward
		    		    
	abstract public void setAction (int action);//设置动作动作向量
	abstract public void initAction ();//初始化动作向量，我猜测应该是全部初始化为1
	abstract public void resetAction ();//重置动作向量
	abstract public void setState( double [] sonar, double [] av_sonar, int bearing, double range );//设置状态向量，参数分别为，输入的声纳向量，av当前的声纳向量，防卫，range暂时不知道是什么意思
	abstract public void setNewState( double [] sonar, double [] av_sonar, int bearing, double range );
	  
	abstract public int  doSearchAction(int mode, int type);//做搜寻动作，这应该是ε-greegy策略里的搜索操作
	abstract public int  doSelectAction(boolean train, Maze maze );//做ε-greedy里的最优动作，参数为是否在训练中 以及地图的类
	abstract public int  doSelectValidAction(boolean train, Maze maze ); //做选择的有效的操作
	abstract public int  doDirectAccessAction(int agt, boolean train, Maze maze ); //做直接移动的操作，参数的agt应该为agent的编号
	
	abstract public void doLearnACN ();//猜测是做ACN网络的学习算法
	abstract public void setprev_J ();//设置之前的J函数？？
	abstract public double computeJ (Maze maze);//计算J函数
	abstract public void setNextJ (double J);//设置之后的J函数
		
	abstract public void turn (int d);//转向
	abstract public void move (int d, boolean actual);//移动

	abstract public double doSearchQValue (int mode, int type);//做Search操作的Q函数的值
	abstract public double getMaxQValue (int method, boolean train, Maze maze);//获得最大的价值函数的值
	
	abstract public void   setReward     (double reward);//设置奖励向量
	abstract public void   setPrevReward (double reward);//设置之前的奖励
	abstract public double getPrevReward ();//获取之前的奖励
	
	abstract public void init_path (int maxStep);//初始化路径
	abstract public void setTrace (boolean trace);//是否设置轨迹追踪

	abstract public int getNumCode();
	abstract public void decay ();//衰减
	abstract public void prune ();//修剪
	abstract public void purge ();//清楚，清晰，净化
	abstract public void reinforce ();//强化
	abstract public void penalize ();//惩罚
}
