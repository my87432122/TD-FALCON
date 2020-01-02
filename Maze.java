import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
public class Maze {
    //final  int size=200;
    final  int size=16;  //地图大小,Final修饰的成员变量终身不变，并且必须提前赋值
    final  int numMines=10;//雷的数量
    final boolean binarySonar=false;//？？声纳是否是二进制？？
    
    final   int LINEAR=0;//
    final   int EXP   =1;
    private int RewardType=EXP; //LINEAR; 这里的暗黄色的意思是检测到这个变量可以使用局部变量替换不用全局定义，建议删除并写成局部变量。
    //RwardYype是线性(0)的还是指数型的(1)
    private int agent_num;//Agent的数量

    private int[][] current;
    private int[][] prev_current;
    private int[] target;
    private int[] currentBearing;//现在的方位
    private int[] prev_bearing;//之前的方位
    private int[] targetBearing;
    private double[][] sonar;//这声纳为啥是个二维数组？？？这个应该就是声纳的坐标吧(x,y)
    private double[][] av_sonar;
    private int[]   range;
    private int[][] mines;

    private int[][] avs;
    private boolean[] end_state;    
    private boolean[] conflict_state;   

    public Maze ( int ag_num ) {
        refreshMaze( ag_num );
    }

    public void set_conflict( int i, int j )//对 agent i 用agent j 设为发生冲突而停止的 Agent
    {
        avs[current[i][0]][current[i][1]] = 0;//这应该是把这个发生冲突的坐标置为0,表示在这个坐标上没有Agent了（如果有Agent 则avs[x][y]=Agent的编号）
        end_state[i] = true;//把 i 和 j 设置为已经停止
        end_state[j] = true;
        conflict_state[i] = true;//这一行暂时不动，为啥要设置两个 一个 end_state 一个 conflict_state？
        conflict_state[j] = true;
//        current[i][0] = -1;
//        current[i][1] = -1;
//        current[j][0] = -1;
//        current[j][1] = -1;
    }

    public boolean check_conflict( int i )//检查是否有与Agent i 冲突的Agent
    {
        int k;
 
        if( ( current[i][0] == target[0] ) && ( current[i][1] == target[1] ) )//如果Agent i的当前状态已经到达目的地，那么就不会冲突了
            return( false );
        if( conflict_state[i] )//如果Agent i 已经被标记为停止了，就直接返回true
            return( true );
        if( ( current[i][0] < 0 ) || ( current[i][1] < 0 ) )//如果Agent i 当前的坐标为负数，那么代表就不会冲突
            return( false );
        for( k = 0; k < agent_num; k++ )//遍历所有的 Agent
        {
            if( k == i )//自己不会与自己冲突
                continue;
            if( ( current[k][0] == current[i][0] ) && ( current[k][1] == current[i][1] ) )//如果两个 Agent 的坐标相等
            {
                set_conflict( i, k );//那么就把 i 和 j 两个Agent设置为冲突
                    return( true );
            }
         }
         return( false );
      }

      public boolean check_conflict( int agt, int pos[], boolean actual )//这里又重载了一个函数，猜测应该是在pos[0],pos[1]这个坐标处？？还是不懂,这里的actual是判断是不是真正移动，是virtual_move or actual move
      {
        int k;
        //我猜测应该是 Agt这个agent在pos[0]pos[1]这个位置，检查有么有其他的Agent也在这个位置
        for( k = 0; k < agent_num; k++ )
        {
            if( k == agt )
                continue;
            if( ( current[k][0] == pos[0] ) && ( current[k][1] == pos[1] ) )
            {
                if( actual )//如果是真的移动那就意味着两个agent发生撞击了
                {
                    set_conflict( agt, k );
                }
                    return( true );
            }
        }
          return( false );
      }

    public void refreshMaze( int agt )  //跟新迷宫
    {
        int k, w;
        int x, y;
        double d;
        
        // limit the agent number between 1 and 10
        if( agt < 1 )
            agent_num = 1;
        else if( agt > 10 )
            agent_num = 10;
        else
            agent_num = agt;
        current = new int[agent_num][];
        target = new int[2];//就是旗子的坐标
        prev_current = new int[agent_num][];
        currentBearing = new int[agent_num];//所有Agent的当前方向
        prev_bearing = new int[agent_num];//所有Agnet之前的方向
        targetBearing = new int[agent_num];//所有Agent期望的目标方向
        avs = new int[size][size];//avs到底是什么，和地图大小一样的二维数组
        mines = new int[size][size];
        end_state = new boolean[agent_num];//判断Agent是否已经停止了
        conflict_state = new boolean[agent_num];//判断Agent是否发生了冲突

        sonar = new double[agent_num][];//先初始化第一维
        av_sonar = new double[agent_num][];

        for( k = 0; k < agent_num; k++ )
        {
            current[k] = new int[2];
            prev_current[k] = new int[2];
            end_state[k] = false;
            conflict_state[k] = false;
            sonar[k] = new double[5];//五个方向的声纳探测输入信号
            av_sonar[k] = new double[5];//现在暂时没理解sonar和av_sonar的区别
        }
        
        for( k = 0; k < 3; k++ ) {
            d = Math.random();  //返回一个随机数，[0.0,1.0]
        }

        for (int i=0; i<size; i++)
            for (int j=0; j<size; j++)
                avs[i][j] = 0;
                
        for( k = 0; k < agent_num; k++ )//给每个Agent都随机生成一个初始位置
        {
            do
            {
                x = (int) (Math.random()*size);
                current[k][0] = x;
                y = (int) (Math.random()*size);
                current[k][1] = y;
            }
            while( avs[x][y] > 0 );

            avs[x][y] = k + 1;//在地图上标出来Agent的位置

            for( w = 0; w < 2; w++ )
                prev_current[k][w] = current[k][w];//之前的位置状态也标记成这个，反正是初始化无所谓的

            end_state[k] = false;
            conflict_state[k] = false;
        }

        do {
            x = (int) (Math.random()*size);
            target[0] = x;
            y = (int) (Math.random()*size);
            target[1] = y;
        } while ( avs[x][y] > 0 );
            
        for (int i=0; i<size; i++)//初始化雷区，
            for (int j=0; j<size; j++)
                mines[i][j] = 0;
                
        for( int i = 0; i < numMines; i++ ) //初始化雷的位置
        {
            do 
            {
                x = ( int )( Math.random() * size );
                y = ( int )( Math.random() * size );
            } 
            while( ( avs[x][y] > 0 ) || ( mines[x][y] == 1 ) || ( x == target[0] && y == target[1] ) );
            mines[x][y] = 1;
        }
        for( int a = 0; a < agent_num; a++ )//禁止套娃！！调整所有Agent的方向为朝向旗子的一侧，只初始为上下左右
        {
            this.setCurrentBearing( a, this.adjustBearing( this.getTargetBearing( a ) ) );
            prev_bearing[a] = this.currentBearing[a];
        }
    }
//————————————————————————————————————2019.12.18——————————————————————————
    public int adjustBearing( int old_bearing )
    {
        if( ( old_bearing == 1 ) || ( old_bearing == 7 ) )
            return( 0 );//右上左上都归为上
        if( ( old_bearing == 3 ) || ( old_bearing == 5 ) )
            return( 4 );//右下左下都归为下
        return( old_bearing );
    }

    public int getTargetBearing( int i )    //获得目标方位
    {
        if( ( current[i][0] < 0 ) || ( current[i][1] < 0 ) )
            return( 0 );
        int [] d = new int[agent_num];

        d = new int[2];
        d[0] = target[0] - current[i][0];
        d[1] = target[1] - current[i][1];
        
        if( d[0] == 0 && d[1] < 0 )//我是以左上角为坐标系向下向右建立坐标轴
            return( 0 );//向上
        if( d[0] > 0 && d[1] < 0 ) 
            return( 1 );//右上
        if( d[0] > 0 && d[1] == 0 ) 
            return( 2 );//右
        if( d[0] > 0 && d[1] > 0 )  
            return( 3 );//右下
        if( d[0] == 0 && d[1] > 0 )  
            return( 4 );//下
        if( d[0] < 0 && d[1] > 0 )  
            return( 5 );//左下
        if( d[0] < 0 && d[1] == 0 ) 
            return( 6 );//左
        if( d[0] < 0 && d[1] < 0 )  
            return( 7 );//左上
        return( 0 );
    }

    public int [] getTargetBearing()//又重载了一个获取目标方位的函数，这个是直接返回所有Agent的目标方位
    {
        int [] ret = new int[agent_num];
        int k;

        for( k = 0; k < agent_num; k++ )
            ret[k] = getTargetBearing( k );
        return( ret );
    }
    
    public int getCurrentBearing( int i ) {//获取 Agent i的当前方位
        return ( currentBearing[i] );
    }
    
    public int [] getCurrentBearing() {//获取所有Agent的当前方位
        return ( currentBearing );
    }
    
    public void setCurrentBearing( int i, int b ) {//把 Agent i 的方位设成 b
        currentBearing[i] = b;
    }
    
    public void setCurrentBearing(int [] b) {
        currentBearing = b;//这种直接赋值的方法非常不安全，因为b如果被删除了CurrentBearing也就没了
        //推荐使用 Syetem.arraycopy(b,0,currentBearing,0,b.length)
    }
    
    public double getReward( int agt, int [] pos, boolean actual, boolean immediate)//immediate应该是是否是即时奖励
    {//这个函数应该是获得agt当前的奖励值
        int x = pos[0];
        int y = pos[1];

        if( ( x == target[0] ) && ( y == target[1] ) ) // reach target
        {
            end_state[agt] = true;
            avs[x][y] = 0;//agt从地图上消失了，他所在的位置可以通过了
            return (1);//获得奖励1
        }
        if( ( x < 0 ) || ( y < 0 ) ) // out of field
            return( -1 );
            
        if( mines[x][y] == 1 )       // hit mines
            return( 0 );
//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>这里去除了注释，我感觉如果检测到agt，在这个pos[0]pos[1]位置上，已经发生冲突停止了，那么应该不会获得奖励
       if( check_conflict( agt, pos, actual ) )
           return( 0 );
//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>如果程序出错了，那么可能就是因为这里>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  
        if( immediate) {//如果不是即时奖励，那么一个trial结束后就不会得到基于与target的距离的Reward
        	if (RewardType==LINEAR) {
        	   	int r = getRange( agt );
        		if (r>10) r = 10;
				return( ( double )1.0 - (double)r /10.0 ); //adjust intermediate reward Reward要限定在[0,1]之间
			}
            else//utility=1/(1+rd)
                return( ( double )1.0 / ( double )( 1 + getRange( agt ) ) ); //adjust intermediate reward
		}
		return (double)0.0; //no intermediate reward
    }

    public double getReward( int i, boolean immediate ) //获取agent i 的奖励
    {
        return( getReward( i, current[i], true, immediate ) );
    }

    public double [] getReward(boolean immediate) {
        int k;
        double [] r;
    
        r = new double[agent_num];
        for( k = 0; k < agent_num; k++ )
        {
            r[k] = getReward( k, immediate );  
        }
        return( r );
    }

    public int getRange( int [] a, int [] b ) {//两个点 a和 b 返回两者x和y坐标相差较大的那一段距离
        int range;
        int[] d = new int[2];

        d[0] = Math.abs( a[0] - b[0] );
        d[1] = Math.abs( a[1] - b[1] );     
        range = Math.max( d[0], d[1] );     
        return( range );
    }

    public int getRange( int i ) //返回 I 和 target 坐标相差较大的那一段距离
    {
        return( getRange( current[i], target ) );
    }

    public int getRange( int i, int j ) //返回 I 和 j 相差较大的那个坐标轴的距离
    {
        return( getRange( current[i], current[j] ) );
    }

    public int [] getRange() //返回所有agent与target的相差较大的那个坐标轴的距离
    {
        int k;
        int [] range;
    
        range = new int[agent_num];
        for( k = 0; k < agent_num; k++ )
        {
            range[k] = getRange( k );   
        }
        return( range );
    }
         
    public double getTargetRange( int i ) 
    {
        return (double)1.0/(double)(1+getRange( i ));
    }   
    
    public double [] getTargetRange() 
    {
        int k;
        double [] range;
    
        range = new double[agent_num];
        for( k = 0; k < agent_num; k++ )
        {
            range[k] = getTargetRange( k ); 
        }
        return( range );
    }
         
    public void getSonar( int agt, double [] new_sonar ) 
    {
        int r;
        int x = current[agt][0];
        int y = current[agt][1];

        if( ( x < 0 ) || ( y < 0 ) )
        {
            for (int k=0; k<5; k++) 
                new_sonar[k] = 0;
            return;
        }
        
        double[] aSonar = new double[8];//八个方位输入
        
        r=0;//r就是当前位置(x,y)距离墙或者雷的距离
        while( y-r>=0 && mines[x][y-r]!=1 )//从(x,y)位置向上摸索，看看有没有雷或者墙
            r++;
        if (r==0)// || y-r<0)//也就是说在（x,y)上有颗雷，这时候显然就不能有输入了
            aSonar[0] = (double)0.0;
        else//
            aSonar[0] = (double)1.0/(double)r;
        
        r=0;
        while (x+r<=size-1 && y-r>=0 && mines[x+r][y-r]!=1)
            r++;
        if (r==0)
            aSonar[1] = (double)0.0;
        else
            aSonar[1] = (double)1.0/(double)r;

        r=0;
        while (x+r<=size-1 && mines[x+r][y]!=1)
            r++;
        if (r==0)
            aSonar[2] = (double)0.0;
        else
            aSonar[2] = (double)1.0/(double)r;
        
        r=0;
        while (x+r<=size-1 && y+r<=size-1 && mines[x+r][y+r]!=1)
            r++;
        if (r==0)
            aSonar[3] = (double)0.0;
        else
            aSonar[3] = (double)1.0/(double)r;
            
        r=0;
        while (y+r<=size-1 && mines[x][y+r]!=1)
            r++;
        if (r==0)
            aSonar[4] = (double)0.0;
        else
            aSonar[4] = (double)1.0/(double)r;
        
        r=0;
        while (x-r>=0 && y+r<=size-1 && mines[x-r][y+r]!=1)
            r++;
        if (r==0)
            aSonar[5] = (double)0.0;
        else
            aSonar[5] = (double)1.0/(double)r;

        r=0;
        while (x-r>=0 && mines[x-r][y]!=1)
            r++;
        if (r==0)
            aSonar[6] = (double)0.0;
        else
            aSonar[6] = (double)1.0/(double)r;
        
        r=0;
        while (x-r>=0 && y-r>=0 && mines[x-r][y-r]!=1)
            r++;
        if (r==0)
            aSonar[7] = (double)0.0;
        else
            aSonar[7] = (double)1.0/(double)r;
        
        currentBearing = getCurrentBearing ();
            
        for (int k=0; k<5; k++) 
        {
            new_sonar[k] = aSonar[(currentBearing[agt]+6+k)%8];//这也太绕了我靠，new_sonar的方位是顺时针，从左方向开始计数，左方向为0 右方向为4，aSonar的方位是从上方向开始的 左侧为6 右侧为2
            if (binarySonar)//上面那式子就是做一个转换，把all_Sonar的八个方向的五个方向取过来放到new_sonar中
                if (new_sonar[k]<1)
                    new_sonar[k]=0; // binary sonar signal
        }
        return;
    }
    
    public void getAVSonar( int agt, double [] new_av_sonar ) //获取视野内的agent的距离
    {
        int r;
        int x = current[agt][0];
        int y = current[agt][1];

        if( ( x < 0 ) || ( y < 0 ) )
        {
            for (int k=0; k<5; k++) 
                new_av_sonar[k] = 0;//初始化当前Agent的五个感知信号的输入
            return;
        }
        
        double[] aSonar = new double[8];//初始化八个方向的探测Agent的声纳信号
        
        r=0;
        while( y-r>=0 && (avs[x][y-r]==(agt+1) || avs[x][y-r]==0) )//y-r>=0限制有没有到墙边，程序里的Agent的编号是0-7，实际编号为1-8，这里的Agt+1就是指本身，向上探测，看是否有其余的Agent
            r++;
        if (r==0)
            aSonar[0] = (double)0.0;//
        else
            aSonar[0] = (double)1.0/(double)r;
        
        r=0;
        while (x+r<=size-1 && y-r>=0 && ( avs[x+r][y-r]==(agt+1) || avs[x+r][y-r]==0 ) )//右上
            r++;
        if (r==0)
            aSonar[1] = (double)0.0;
        else
            aSonar[1] = (double)1.0/(double)r;

        r=0;
        while (x+r<=size-1 && ( avs[x+r][y]==(agt+1) || avs[x+r][y]==0 ) )//右侧
            r++;
        if (r==0)
            aSonar[2] = (double)0.0;
        else
            aSonar[2] = (double)1.0/(double)r;
        
        r=0;
        while (x+r<=size-1 && y+r<=size-1 && ( avs[x+r][y+r]==(agt+1) || avs[x+r][y+r]==0 ) )//右下
            r++;
        if (r==0)
            aSonar[3] = (double)0.0;
        else
            aSonar[3] = (double)1.0/(double)r;
            
        r=0;
        while (y+r<=size-1 && ( avs[x][y+r]==(agt+1) || avs[x][y+r]==0 ) )//下
            r++;
        if (r==0)
            aSonar[4] = (double)0.0;
        else
            aSonar[4] = (double)1.0/(double)r;
        
        r=0;
        while (x-r>=0 && y+r<=size-1 && ( avs[x-r][y+r]==(agt+1) || avs[x-r][y+r]==0 ) )//左下
            r++;
        if (r==0)
            aSonar[5] = (double)0.0;
        else
            aSonar[5] = (double)1.0/(double)r;

        r=0;
        while (x-r>=0 && ( avs[x-r][y]==(agt+1) || avs[x-r][y]==0 ) )//左
            r++;
        if (r==0)
            aSonar[6] = (double)0.0;
        else
            aSonar[6] = (double)1.0/(double)r;
        
        r=0;
        while (x-r>=0 && y-r>=0 && ( avs[x-r][y-r]==(agt+1) || avs[x-r][y-r]==0 ) )//左上
            r++;
        if (r==0)
            aSonar[7] = (double)0.0;
        else
            aSonar[7] = (double)1.0/(double)r;
        
        currentBearing = getCurrentBearing ();
            
        for (int k=0; k<5; k++) {
            new_av_sonar[k] = aSonar[(currentBearing[agt]+6+k)%8];//方向转换，这里的+6要谨慎对待，可以替换为-2，不过不同的编译器对于负数求余的认知不同，还是尽量使用正数求余
            if( binarySonar )//二值化输入的声纳信号，只有0和1
                if( new_av_sonar[k] < 1 )//
                    new_av_sonar[k] = 0; // binary sonar signal
        }
        return;             
    }
    //这个virtual_move函数用于虚拟执行下一步的行走以计算奖励，不实际改变方向和坐标，而把虚拟行走后的坐标存入res[0]&&res[1]中
    public void virtual_move( int agt, int d, int [] res ) //Agent的虚拟行走函数，一次行走1，agt为Agent的编号，d为要前进的方向（d为相对方向，d的取值应为-2 -1 0 1 2），res为Agt对于Agent的坐标
    {
        int k;
        int bearing = ( currentBearing[agt] + d + 8 ) % 8;//计算按d行走后的绝对方向

        res[0] = current[agt][0];
        res[1] = current[agt][1];

        switch( bearing )
        {
            case 0: 
                if( res[1] > 0 ) 
                    res[1]--;
                break;
            case 1:
                if( ( res[0] < size - 1 ) && ( res[1] > 0 ) ) 
                {
                    res[0]++;       
                    res[1]--;
                } 
                break;
            case 2: 
                if( res[0] < size - 1 ) 
                    res[0]++;
                break;
            case 3:
                if( ( res[0] < size - 1 ) && ( res[1] < size - 1 ) ) 
                {
                    res[0]++;       
                    res[1]++;       
                } 
                break;
            case 4: 
                if( res[1] < size - 1 ) 
                    res[1]++;
                break;
            case 5:
                if( ( res[0] > 0 ) && ( res[1] < size - 1 ) ) 
                {
                    res[0]--;       
                    res[1]++;
                } 
                break;
            case 6: 
                if( res[0] > 0 ) 
                    res[0]--;
                break;
            case 7:
                if( ( res[0] > 0 ) && ( res[1] > 0 ) ) 
                {
                    res[0]--;       
                    res[1]--;
                } 
                break;
            default: 
                break;
        }
        return;
    }

    public void turn( int i, int d )//转向，这个函数后面没有使用
    {
        int bearing = getCurrentBearing( i );
        bearing = ( bearing + d ) % 8;
        setCurrentBearing( i, bearing );
    }

    public int move( int i, int d ) {//这是Agent实际的移动函数,d为相对方向，移动成功就返回1，移动不成功就返回-1 
        int k;

        if( ( current[i][0] < 0 ) || ( current[i][1] < 0 ) )
            return( -1 );

        for( k = 0; k < 2; k++ )
            prev_current[i][k] = current[i][k];

        prev_bearing[i] = currentBearing[i];
        
        currentBearing[i] = ( currentBearing[i] + d + 8 ) % 8;
        
        switch (currentBearing[i]) {
            case 0: 
                if (current[i][1]>0) current[i][1]--;
                else 
                {
 //                   turn( i );
                    return( -1 );
                }
                break;
            case 1:
                if (current[i][0]<size-1 && current[i][1]>0) {
                    current[i][0]++;        
                    current[i][1]--;
                } 
                else 
                {
 //                   turn( i );
                    return( -1 );
                }
                break;
            case 2: 
                if (current[i][0]<size-1) current[i][0]++;
                else 
                {
//                    turn( i );
                    return( -1 );
                }
                break;
            case 3:
                if (current[i][0]<size-1 && current[i][1]<size-1) {
                    current[i][0]++;        
                    current[i][1]++;        
                } 
                else 
                {
 //                   turn( i );
                    return( -1 );
                }
                break;
            case 4: 
                if (current[i][1]<size-1) 
                    current[i][1]++;
                else 
                {
 //                   turn( i );
                    return( -1 );
                }
                break;
            case 5:
                if (current[i][0]>0 && current[i][1]<size-1) {
                    current[i][0]--;        
                    current[i][1]++;
                } 
                else 
                {
//                    turn( i );
                    return( -1 );
                }
                break;
            case 6: 
                if (current[i][0]>0) current[i][0]--;
                else 
                {
//                    turn( i );
                    return( -1 );
                }
                break;
            case 7:
                if (current[i][0]>0 && current[i][1]>0) {
                    current[i][0]--;        
                    current[i][1]--;
                } 
                else 
                {
//                    turn( i );
                    return( -1 );
                }
                break;
            default: break;
        }
        avs[prev_current[i][0]][prev_current[i][1]] = 0;
        avs[current[i][0]][current[i][1]] = i + 1;

        return (1);
    }
    
    
	// return true if the move still keeps the agent within the field 检查是否超出边界
    public boolean withinField ( int i, int d ) {//检测AGent i 还能否在 相对方向d 上继续移动
        int testBearing;
        
        testBearing = ( currentBearing[i] + d + 8 ) % 8;
        switch (testBearing) {
            case 0: 
                if (current[i][1]>0)
                	return (true);
                break;
            case 1:
                if (current[i][0]<size-1 && current[i][1]>0)
                    return( true );
                break;
            case 2: 
                if (current[i][0]<size-1) return (true);
                break;
            case 3:
                if (current[i][0]<size-1 && current[i][1]<size-1)
                    return( true );
                break;
            case 4: 
                if (current[i][1]<size-1) 
                    return( true );
                break;
            case 5:
                if (current[i][0]>0 && current[i][1]<size-1)
                	return (true);
                break;
            case 6: 
                if (current[i][0]>0)
                    return( true );
                break;
            case 7:
                if (current[i][0]>0 && current[i][1]>0)
                    return( true );
                break;
            default: break;
        }
//	    System.out.println ( "OutOfField: current = ("+current[i][0]+","+current[i][1]+")  testBearing = " + testBearing);
        return (false);
    }
    
    public int [] move( int [] d )//一次移动所有的Agent
    {
        int k;
        int [] res;

        res = new int[agent_num];
        for( k = 0; k < agent_num; k++ )
            res[k] = move( k, d[k] );
        return res;
    }

    public void undoMove(){//取消上次的移动
        this.currentBearing=this.prev_bearing;
        current[0] = prev_current[0];
        current[1] = prev_current[1];   
    }

    public double nextReward( int agt, int d, boolean immediate )
    {
        double r;
        int [] next_pos = new int[2];

        virtual_move( agt, d, next_pos );//next_pos接收虚拟移动后agt的坐标
        r = this.getReward( agt, next_pos, false, immediate ); //consider revise
        // this.undoMove();
        return r;
    }

    public boolean endState( int agt )//修改并返回agt的end_state 用于判断当前agt是否已经停止
    {
        int x = current[agt][0];
        int y = current[agt][1];

        if( conflict_state[agt] )//agt已经发生冲突了
        {
            end_state[agt] = true;  
            return( end_state[agt] );
        }
        if( ( x < 0 ) || ( y < 0 ) )//出界了
        {
            end_state[agt] = true;  
            return( end_state[agt] );
        }
        if( ( x == target[0] ) && ( y == target[1] ) )//到达终点了
        {
            end_state[agt] = true;
            avs[x][y] = 0;
            return( end_state[agt] );
        }
        if( ( mines[x][y] == 1 ) || ( check_conflict( agt ) ) || ( end_state[agt] ) )// 踩雷||检测冲突||agt已经停止
        {
            avs[x][y] = 0;
            end_state[agt] = true;
        }
        else
            end_state[agt] = false; 
        return( end_state[agt] );
    }

	public boolean endState( boolean target_moving )//这个函数用于检测 target_moving 模式下 是否所有的Agent都停止工作了，如果是的话返回true;
	{
		int k;
		boolean bl = true;//这一参数是用来当target是移动的时候，返回Agent k 当前是否已经进入endState
		for( k = 0; k < agent_num; k++ )
		{
			if( target_moving )
			{
				if( isHitTarget( k ) )
					return( true );
				if( !endState( k ) )//如果Agent k 还没有停止
					bl = false;
			}
			else
			{
				if( !endState( k ) )
					return( false );
			}
		}
		if( target_moving )
			return( bl );
		else
			return( true );
	}

    public boolean endState()//这个函数用于检测普通模式下是否所有的Agent都停止运动了，如果有Agent尚未！endState(k) 则 返回false
    {
        int k;

        for( k = 0; k < agent_num; k++ )
        {
            if( !endState( k ) )
                return( false );
        }       
        return( true );
    }

    public boolean isHitMine( int i )//判断是否踩雷，踩雷就返回true
    {
        if( ( current[i][0] < 0 ) || ( current[i][1] < 0 ) )
            return( false );
        if( mines[current[i][0]][current[i][1]] == 1 )
            return true;
        else 
            return false;
    }

    public boolean isConflict( int i )//判断i是否发生冲突
    {
        return( conflict_state[i] );
    }

    public boolean isHitTarget( int i )//在target_move模式下判断是否已经到达目标
    {
        if( ( current[i][0] == target[0] ) && ( current[i][1] == target[1] ) )
            return true;
        else 
            return false;
    }

    public boolean test_mines( int i, int j ) //判断坐标(i,j)是否有雷
    {
        if( mines[i][j] == 1 )
            return( true );
        else
            return( false );
    }

    public boolean test_current( int agt, int i, int j ) {//判断agt当前的坐标是不是(i,j)
        if ( current[agt][0] == i && current[agt][1] == j )
            return( true );
        else
            return( false );
        }

    public boolean test_target( int i, int j ) //判断当前target的坐标是不是（i，j）
    {
        if( ( target[0] == i ) && ( target[1] == j ) )
            return( true );
        else
            return( false );
    }

    public int getMines( int i, int j ) //获得当前坐标是否有雷
    {
        return( mines[i][j] );
    }

    public int [] getCurrent( int agt ) //获取当前agt的坐标
    {
        return( current[agt] );
    }

    public int [][] getCurrent() //获取当前所有agt的坐标
    {
        return( current );
    }

    public void getCurrent( int agt, int [] path ) //把agt的坐标存入path[]
    {
        int k;
        for( k = 0; k < 2; k++ )
            path[k] = current[agt][k];
        return;
    }

    public void getCurrent( int [][] path )//把所有agt的坐标存入path[][] 
    {
        int i, j;

        for( i = 0; i < agent_num; i++ )
            for( j = 0; j < 2; j++ )
                path[i][j] = current[i][j];
        return;
    }

    public int[] getPrevCurrent( int agt ) 
    {
        return( prev_current[agt] );
    }

    public int[][] getPrevCurrent() 
    {
        return( prev_current );
    }

    public int[] getTarget() 
    {
        return( target );
    }

	public void go_target()
	{
		int [] new_pos = new int[2];
		int b;
		int k;
		double d;

		for( k = 0; k < 3; k++ )
			d = Math.random();//random()随机[0,1]内
		do
		{
			b = ( int )( Math.random() * size );//在0-15随机去一个数，这里的范围大于8的原因是target不会每一个时刻都在动，可以
			virtual_move_target( b, new_pos );
		}
		while( !valid_target_pos( new_pos ) );//如果virtual_move_target移动后的结果满足要求，则就移动target
		move_target( b );
		return;
	}

	public boolean valid_target_pos( int [] new_pos )//判断这个坐标是否可用
	{
		int x = new_pos[0];
		int y = new_pos[1];

		if( ( x < 0 ) || ( x >= size ) )
			return( false );
		if( ( y < 0 ) || ( y >= size ) )
			return( false );
		if( avs[x][y] > 0 )
			return( false );
		if( mines[x][y] == 1 )
			return( false );
		return( true );
	}

	public void virtual_move_target( int d, int [] new_pos )//
	{
		new_pos[0] = target[0];
		new_pos[1] = target[1];
		switch( d ) 
		{
			case 0: 
				new_pos[1]--;
				break;
			case 1:
				new_pos[0]++;		
				new_pos[1]--;
				break;
			case 2: 
				new_pos[0]++;
				break;
			case 3:
				new_pos[0]++;		
				new_pos[1]++;		
				break;
			case 4: 
				new_pos[1]++;
				break;
			case 5:
				new_pos[0]--;		
				new_pos[1]++;
				break;
			case 6: 
				new_pos[0]--;
				break;
			case 7:
				new_pos[0]--;		
				new_pos[1]--;
				break;
			default: 
				break;
		}
	}

	public void move_target( int d )
	{
		switch( d ) 
		{
			case 0: 
				target[1]--;
				break;
			case 1:
				target[0]++;		
				target[1]--;
				break;
			case 2: 
				target[0]++;
				break;
			case 3:
				target[0]++;		
				target[1]++;		
				break;
			case 4: 
				target[1]++;
				break;
			case 5:
				target[0]--;		
				target[1]++;
				break;
			case 6: 
				target[0]--;
				break;
			case 7:
				target[0]--;		
				target[1]--;
				break;
			default: 
				break;
		}
	}
}
