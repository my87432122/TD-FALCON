/*
 * @(#)MNT.java 1.0 03/12/30
 *
 * You can modify the template of this file in the
 * directory ..\JCreator\Templates\Template_1\Project_Name.java
 *
 * You can also create your own project template by making a new
 * folder in the directory ..\JCreator\Template\. Use the other
 * templates as examples.
 *
 */

import java.io.*;
import java.lang.*;
import java.text.*;
import java.math.*;
import java.util.*;
import java.sql.Time;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.io.*;
import java.lang.*;
import java.text.*;	 

class MNT extends JFrame implements ActionListener {
	private final int sonar_num = 1;
    private JButton jbtHardLeft, jbtLeft, jbtStraight, jbtRight, jbtHardRight;
    private JButton jbtStep, jbtAuto, jbtReset;
    private Maze maze;
    private int step;
    private MazePanel p_field;
    private SonarPanel [] p_sonar;
    private SonarPanel [] p_avsonar;
    private BearingPanel [] p_bearing;
    private MessagePanel p_msg;
    private JPanel p_sense, p_fieldmsg, p_left, p_mannual, p_auto, p_control;
    private JPanel [] p_sonarPane;
    private JPanel [] p_avsonarPane;
    private JPanel [] p_bearingPane;
    private JPanel p_autoPane;
    
    private JPanel p_autoPane_1;
    private JPanel p_autoPane_view;
    private JPanel p_autoPane_ID;
    
    private JTextField jtfTrial, jtfStep, jtfAgent, jtfDelay;
    private JCheckBox immediateFlag, boundFlag, trackFlag, moveFlag;
    
	private int     Delay;
    private boolean Bound;
    private boolean ImmediateReward;
    
    private final static int RFALCON =0;  //AVTYPE的参数 RFALCON,TDFALCON,BPN
    private final static int TDFALCON=1;
    private final static int BPN     =2;
    
    private final static int QLEARNING=0; //TDMethod QLEARNING,SARSA
    private final static int SARSA    =1;
     
    // Important RunTime Parameters
    
    private static int     TDMethod=QLEARNING;
    //private static int     TDMethod=SARSA;
    private static int     AVTYPE  =TDFALCON;
    //private static int     AVTYPE  =BPN;
    //private static int     AVTYPE  =RFALCON;

         
	private static int     numRuns =1;
	private static int     interval =100;

    public  static boolean graphic=true;
    public  static boolean Track  =false;
    private static boolean Target_Moving=false;
    private static boolean Trace  = true;
    
    private int      agent_num;
    private AGENT [] agent;
    
    private int inter_int;
    private TitledBorder [] sonar_title;
    private TitledBorder [] av_sonar_title;
    private TitledBorder [] bearing_title;
    private PrintWriter pw_score = null;
    private PrintWriter pw_avg = null;
    private PrintWriter pw_code = null;
    private PrintWriter pw_step = null;
    private int[][][] path;
    private Container container;

    private int maxTrial;
    private int maxStep;
	private int [] minStep;
	private boolean lastFlag;
    
    private NumberFormat df = NumberFormat.getInstance();   //NumberFormat:所有数值格式的抽象基类; getInstance():返回当前默认语言环境的通用数值格式
        
    public void displayVector(String s, double[] x, int n) {
    	df.setMaximumFractionDigits (2);
        System.out.print (s+ " : ");
        for (int i=0; i<n-1; i++)
            System.out.print (df.format(x[i])+", ");
        System.out.println (df.format(x[n-1]));
    }

   public static void main(String args[]) {
        System.out.println("Starting Minefield Navigation Simulator ...");
        MNT mainFrame = new MNT( 1 );
    }
    
    public MNT (int agt) {
        initMNT (agt);
    }
    
    public void initMNT (int agt) {
        agent_num = agt;
        init_agent ();
        init_mnt ();
        init_parameters ();
    }

	/* 
	 *	Method initializing the simulation parameters and display panels 
	 */
	 
	public void init_agent ()
    {
        int k;

        maze = new Maze (agent_num);
		agent = new AGENT[agent_num];
		        
        if (AVTYPE==RFALCON) {
        	for( k = 0; k < agent_num; k++ )
            	agent[k] = new FALCON( k );
            System.out.println("Agent Type: R-FALCON");
        }
        if (AVTYPE==TDFALCON) {
        	for( k = 0; k < agent_num; k++ )
            	agent[k] = new FALCON( k );
            System.out.println("Agent Type: TD-FALCON");
        }
        else if (AVTYPE==BPN) {   
        	for( k = 0; k < agent_num; k++ )
            	agent[k] = new BP( k );
            System.out.println("Agent Type: BPN");        	
    	}
    }
    
	public void init_parameters ()
    {	
    	for( int k = 0; k < agent_num; k++ )
            agent[k].setParameters (AVTYPE, ImmediateReward);
    }
    
	/* 
	 *	Method initializing the simulation parameters and display panels 
	 */ 
	public void init_mnt ()
    {
    	int k;
    	            	
        container = getContentPane();
        container.setLayout( new GridLayout ( 1, 2, 0, 0 ) );
        
        p_sonar = new SonarPanel[sonar_num];
        p_sonarPane = new JPanel[sonar_num];
        sonar_title = new TitledBorder[sonar_num];
        p_avsonar = new SonarPanel[sonar_num];
        p_avsonarPane = new JPanel[sonar_num];
        av_sonar_title = new TitledBorder[sonar_num];
        p_bearing = new BearingPanel[sonar_num];
        p_bearingPane = new JPanel[sonar_num];
        bearing_title = new TitledBorder[sonar_num];

        for( k = 0; k < sonar_num; k++ )
        {       
            p_sonar[k] = new SonarPanel( k, true, Color.green, maze );
            p_sonarPane[k] = new JPanel();          
            p_sonarPane[k].setLayout( new BorderLayout() );
            sonar_title[k] = new TitledBorder( "Sonar Signal Input (Agent " + k + ")");
            p_sonarPane[k].setBorder( sonar_title[k] );
            p_sonarPane[k].add( p_sonar[k], BorderLayout.CENTER );

            p_avsonar[k] = new SonarPanel( k, false, Color.yellow, maze );
            p_avsonarPane[k] = new JPanel();            
            p_avsonarPane[k].setLayout( new BorderLayout() );
            av_sonar_title[k] = new TitledBorder( "AV Sonar Signal (Agent " + k + ")");
            p_avsonarPane[k].setBorder( av_sonar_title[k] );
            p_avsonarPane[k].add( p_avsonar[k], BorderLayout.CENTER );

            p_bearing[k] = new BearingPanel( k, maze );
            p_bearingPane[k] = new JPanel();
            p_bearingPane[k].setLayout( new BorderLayout( 50, 50 ) );
            bearing_title[k] = new TitledBorder("            Current Bearing of Agent " + k + "                         Target Bearing of Agent " + k );
            p_bearingPane[k].setBorder( bearing_title[k] );
            p_bearingPane[k].add( p_bearing[k], BorderLayout.CENTER );
        }

        p_sense = new JPanel();
        p_sense.setLayout( new GridLayout( 3 * sonar_num, 1, 0, 0 ) );

        for( k = 0; k < sonar_num; k++ )
        {
            p_sense.add( p_sonarPane[k] );
            p_sense.add( p_avsonarPane[k] );
            p_sense.add( p_bearingPane[k] );
        }
        
        p_mannual = new JPanel();
        p_mannual.setLayout(new FlowLayout());
        p_mannual.setBorder (new TitledBorder("Mannual Control"));
        p_mannual.add(jbtHardLeft = new JButton(""));
        p_mannual.add(jbtLeft = new JButton("Left"));
        p_mannual.add(jbtStraight = new JButton("Ahead"));      
        p_mannual.add(jbtRight = new JButton("Right"));
        p_mannual.add(jbtHardRight = new JButton(""));
        jbtHardLeft.setIcon (new ImageIcon("./images/left.gif"));
        jbtHardRight.setIcon (new ImageIcon("./images/right.gif"));        
        
        p_control = new JPanel();
        p_control.setLayout(new BorderLayout());
        p_control.add(p_sense,BorderLayout.CENTER);
        p_control.add(p_mannual,BorderLayout.SOUTH);
    
        p_autoPane = new JPanel();
        p_autoPane_1 = new JPanel();
        p_autoPane_1.setLayout( new GridBagLayout());
        p_autoPane_view = new JPanel();
        p_autoPane_view.setLayout( new GridBagLayout());
        p_autoPane_ID = new JPanel();
        p_autoPane_ID.setLayout( new GridBagLayout());
        JPanel temp;
        
        p_autoPane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        p_auto = new JPanel();
        p_auto.setLayout(new GridBagLayout());
        p_auto.setBorder (new TitledBorder("Automatic Control"));
        
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0.05;
        c.weighty = 0.001;
        c.ipady =-2;
      
        temp = new JPanel();
        temp.setLayout( new FlowLayout());
        temp.add(jbtReset = new JButton("Reset"));
        temp.add(jbtStep = new JButton("Step"));
        temp.add(jbtAuto = new JButton("Auto"));
                
        c.gridx = 0;
        c.gridy = 0;
        p_auto.add(temp, c);
        c.ipady = 0;

        p_autoPane_1.setBorder( new TitledBorder("Experiment Settings"));
        
        // Setting number of agents
        temp = new JPanel(); temp.setLayout( new FlowLayout(FlowLayout.LEADING, 0, 0 ));
        temp.add(new Label( "Agents :" ));
        temp.add(jtfAgent = new JTextField( new Integer( agent_num ).toString(), 3 ));
        c.gridx = 0;
        c.gridy = 0;
        p_autoPane_1.add(temp, c);
        
        // Setting number of trials
        temp = new JPanel(); temp.setLayout( new FlowLayout(FlowLayout.LEADING, 0, 0 ));
        temp.add(new Label("Trials   :"));
        
        maxTrial = 2000;
        temp.add(jtfTrial = new JTextField( new Integer( maxTrial ).toString(), 3 ));
        c.gridx = 0;
        c.gridy = 1;
        p_autoPane_1.add( temp ,c );
        
        // Setting maximum number of steps per trial
        temp = new JPanel(); temp.setLayout( new FlowLayout(FlowLayout.LEADING, 0, 0 ));
        temp.add(new Label("Steps   :") );
        maxStep = 30;
        temp.add(jtfStep = new JTextField( new Integer( maxStep ).toString(), 3 ));
        c.gridx = 1;
        c.gridy = 0;
        p_autoPane_1.add(temp, c);
        
        // Setting if the target is moving
        temp=new JPanel(); temp.setLayout( new FlowLayout(FlowLayout.LEADING, 0, 0 ));
        temp.add(new Label( "Target Moving :" ));
        temp.add(moveFlag = new javax.swing.JCheckBox());
        c.gridx = 1;
        c.gridy = 1;
        p_autoPane_1.add(temp, c);
        
        c.gridx = 0;
        c.gridy = 0;
        p_autoPane.add(p_autoPane_1, c);
        
        p_autoPane_view.setBorder( new TitledBorder("View") );
        
        // Setting whether to display the path taken by agent
        temp = new JPanel(); temp.setLayout( new FlowLayout(FlowLayout.LEADING, 0, 0 ));
        temp.add(new Label( "Tracked :" ));
        temp.add(trackFlag = new javax.swing.JCheckBox());
        c.gridx = 1;
        c.gridy = 0;
        p_autoPane_view.add(temp, c);
        
        // Setting the time delay per step
        temp = new JPanel(); temp.setLayout( new FlowLayout(FlowLayout.LEADING, 0, 0 ));
        temp.add(new Label( "Time Delay :" ));
		Delay = 0;
        temp.add(jtfDelay = new JTextField( new Integer( Delay ).toString(), 3 ));
        c.gridx = 0;
        c.gridy = 0;
        p_autoPane_view.add(temp, c);
        
        c.gridx = 0;
        c.gridy = 1;
        p_autoPane.add(p_autoPane_view, c);
       
        p_autoPane_ID.setBorder(new TitledBorder("Learning Parameters"));
        
        // Setting the reward scheme: immediate or delayed
        temp = new JPanel(); temp.setLayout( new FlowLayout(FlowLayout.LEADING, 0, 0 ));
        temp.add( new Label( "Immediate Reward   :" ));
        temp.add( immediateFlag = new javax.swing.JCheckBox());
        c.gridx =0;
        c.gridy = 0;
        p_autoPane_ID.add(temp,c);
        
        // Setting the TD learning method
        temp=new JPanel(); temp.setLayout( new FlowLayout(FlowLayout.LEADING, 0, 0 ));
        temp.add(new Label ( "Bounded TD Rule    :" ) );
        temp.add(boundFlag = new javax.swing.JCheckBox());
        c.gridx = 0;
        c.gridy = 1;
        p_autoPane_ID.add(temp,c);
        
        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.LINE_END;
        p_autoPane.add(p_autoPane_ID, c);
        c.anchor = GridBagConstraints.CENTER;
        
        ImmediateReward = false;
        Bound = true;
 	    Track = false;
		Target_Moving = false;
        
        immediateFlag.setSelected(ImmediateReward);
        boundFlag.setSelected( Bound );
		trackFlag.setSelected( Track );
		moveFlag.setSelected( Target_Moving );
                     
        c.anchor = GridBagConstraints.CENTER;
        c.gridx=0;
        c.gridy=1;
        p_auto.add(p_autoPane ,c);
        
        // Display Panels
      
        p_msg = new MessagePanel();
        p_msg.setBackground (Color.white);
        p_msg.setBorder (new LineBorder (Color.black,1));
        p_msg.setMessage ("");
        
        p_field = new MazePanel( agent_num, maze );

        p_fieldmsg = new JPanel();
        p_fieldmsg.setLayout (new BorderLayout());
        p_fieldmsg.setBorder (new TitledBorder("Minefield (View from the Top)"));
        p_fieldmsg.add(p_field,BorderLayout.CENTER);
        p_fieldmsg.add(p_msg,BorderLayout.SOUTH);
        
        p_left = new JPanel();
        p_left.setLayout (new BorderLayout());
        p_left.add(p_fieldmsg,BorderLayout.CENTER);
        p_left.add(p_auto,BorderLayout.SOUTH);
                                
        container.add(p_left);
        container.add(p_control);
                
        // Register listeners
        
        jbtHardLeft.addActionListener(this);
        jbtLeft.addActionListener(this);
        jbtStraight.addActionListener(this);
        jbtRight.addActionListener(this);
        jbtHardRight.addActionListener(this);
        jbtStep.addActionListener(this);
        jbtAuto.addActionListener(this);
        jbtReset.addActionListener(this);

        immediateFlag.addActionListener(this);
		boundFlag.addActionListener(this);
		trackFlag.addActionListener(this);
		moveFlag.addActionListener(this);
        
        jtfAgent.addActionListener( this );      
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }});       

        setSize( 900, 650 );
        setTitle( "Minefield Navigation Simulator" );
        setVisible( true );

		init_path();
    }
            
    /* 
	 *	Method handling actions of user 
	 */
	 
	public void actionPerformed( ActionEvent e ) 
    {
        String actionCommand = e.getActionCommand();
        int agt;
            
        double [] r = new double[agent_num];
        for( agt = 0; agt < agent_num; agt++ )
            r[agt] = ( double )maze.getReward( agt, ImmediateReward );
                        
        // Handle button events
        if( e.getSource() instanceof JButton ) 
        {
            if ("Reset".equals(actionCommand))
                doReset();          
            else if ("Auto".equals(actionCommand))
                doAuto();               
			else if (!maze.endState( Target_Moving )) 
            {
                if (e.getSource()==jbtHardLeft)
                    goHardLeft();
                else if ("Left".equals(actionCommand))
                    goLeft();
                else if ("Ahead".equals(actionCommand))
                    goStraight();
                else if ("Right".equals(actionCommand))
                    goRight();
                else if (e.getSource()==jbtHardRight)
                    goHardRight();
                else if ("Step".equals(actionCommand))
                {
					get_values();   //获取maxStep, Delay信息
					
					graphic=true;
		    		Trace = true;
       				for( agt = 0; agt < agent_num; agt++ )
              			agent[agt].setTrace( true );    //设置智能体轨迹为true

                    for( agt = 0; agt < agent_num; agt++ )
                    {
						if( ( !maze.endState( Target_Moving ) ) && ( step < maxStep ) ) //没有达到终止状态且没有达到maxStep
						{
							if( Target_Moving )
								maze.go_target();
							doStep( agt, maze.getRange( agt ), false ); //step by step
						}
                    }
                }
            }
                        
            if (!("Auto".equals(actionCommand))) {
                p_field.setCurrent(maze);
                for( agt = 0; agt < sonar_num; agt++ )
                {
                    p_sonar[agt].setSonar( agt, maze );
                    p_avsonar[agt].setSonar( agt, maze );
                    p_bearing[agt].setBearing( agt, maze );
                    r[agt] = maze.getReward( agt, ImmediateReward );
                    if( r[agt] == 1.0 )
                        p_msg.setMessage( "AV" + agt + " Success: Target achieved!" );
                }
            }
            
        }
        // handle Immediate Reward button
        else if( e.getSource().equals( immediateFlag ) )
        {
            ImmediateReward = immediateFlag.isSelected();
            System.out.println( "Immediate reward = " + ImmediateReward );
        }
        // handle Bound button
        else if( e.getSource().equals( boundFlag ) )
        {
            Bound = boundFlag.isSelected();
            System.out.println( "Bound = " + Bound );
        }
		// handle Track button
		else if( e.getSource().equals( trackFlag ) )
		{
			Track = trackFlag.isSelected();
			System.out.println( "Track = " + Track );
		}
		// handle Target-Moving button
		else if( e.getSource().equals( moveFlag ) )
		{
			Target_Moving = moveFlag.isSelected();
			System.out.println( "Target_Moving = " + Target_Moving );
		}
		// handle text field event
        else if( e.getSource() instanceof JTextField )
        {
            JTextField j = ( JTextField )e.getSource();
            int agt_num = Integer.parseInt( j.getText().trim() );
            System.out.println( "# of agents is " + agt_num );
            container.remove( p_left );
            container.remove( p_control );
            repaint();
            get_values();
            initMNT(agt_num);
//            init_mnt( agt_num, false );
        }
    }
    
    /* 
	 *	Method getting simulation parameters from interface 
	 */
	 
	public void get_values()
    {
        String sTrial = jtfTrial.getText().trim();
        if( sTrial.length() > 0 )
            maxTrial = ( Integer.parseInt( sTrial ) );  //Integer.parseInt( sTrial ) 将string转换为int
        else
            maxTrial = 0;
		if( maxTrial < 0 )
			maxTrial = 0;

		if (maxTrial>50000)
			interval = 5000;
		else if (maxTrial>10000)
			interval = 1000;		
		
        String sStep = jtfStep.getText().trim();
        if( sStep.length() > 0 )
            maxStep = ( Integer.parseInt( sStep ) );
        else
            maxStep = 0;
		if( maxStep < 0 )
			maxStep = 0;

		String sDelay = jtfDelay.getText().trim();
		if( sDelay.length() > 0 )
			Delay = ( Integer.parseInt( sDelay ) );
		else
			Delay = 0;
		if( Delay < 0 )
			Delay = 0;
	}

    private void goHardLeft( int agt ) 
    {
        maze.move( agt, -2 );
    }
    
    private void goHardLeft() 
    {
        int agt;

        for( agt = 0; agt < agent_num; agt++ )
            goHardLeft( agt );
    }
    
    private void goLeft( int agt ) 
    {
        maze.move( agt, -1 );
    }

    private void goLeft() 
    {
        int agt;

        for( agt = 0; agt < agent_num; agt++ )
            goLeft( agt );
    }
    
    private void goStraight( int agt ) 
    {
        maze.move( agt, 0 );
    }
    
    private void goStraight() 
    {
        int agt;

        for( agt = 0; agt < agent_num; agt++ )
            goStraight( agt );
    }
    
    private void goRight( int agt ) 
    {
        maze.move( agt, 1 );
    }
    
    private void goRight() 
    {
        int agt;

        for( agt = 0; agt < agent_num; agt++ )
            goRight( agt );
    }
    
    private void goHardRight( int agt ) 
    {
        maze.move( agt, 2 );
    }
    
    private void goHardRight() 
    {
        int agt;

        for( agt = 0; agt < agent_num; agt++ )
            goHardRight( agt );
    }
    
    /* 
	 *	Method simulating the sense-act-learn cycle of R-FALCON 
	 */
	     
   	private void doRStep(int agt, double lastReward, boolean last) {
		final int PERFORM=0;
		final int LEARN=1;
		final int INSERT=2;
		
		final int mode=0;  //0-PERFORM 1-LEARN 
		final int type=0;  //0-fuzzART 1-ART2
		
		double r;
		int   action;
        double [] this_Sonar = new double[5];
        double [] that_Sonar = new double[5];
        double [] this_AVSonar = new double[5];
        double [] that_AVSonar = new double[5];
        int    this_bearing;
       	double this_targetRange;        	
       	
       	do {	
	    maze.getSonar( agt, that_Sonar );    
    	maze.getAVSonar( agt, that_AVSonar );
        
        for( int k = 0; k < 5; k++ ) {
           	this_Sonar[k] = that_Sonar[k];
           	this_AVSonar[k] = that_AVSonar[k];
        }

        this_bearing = ( 8 + maze.getTargetBearing( agt ) - maze.getCurrentBearing( agt ) ) % 8;
        this_targetRange = maze.getTargetRange( agt );
        	
       	agent[agt].setState (this_Sonar, this_AVSonar, this_bearing, this_targetRange);
		agent[agt].initAction();   // initialize action to all 1's
		agent[agt].setReward (1);  // search for actions with good reward

		if (Trace) System.out.println ("Sense and Search for an Action:");

		action = agent[agt].doDirectAccessAction(agt, true, maze ); 

		if (action==-1) {   // No valid action; deadend, backtrack
            System.out.println ( "*** No valid action, backtracking ***");        		    	               
	        	agent[agt].turn(4);
	        	maze.turn (agt,4);	        	
	     	}
	    } while (action==-1);

		if (Trace) System.out.println ("Performing the Action:");
		
	    if (!maze.withinField (agt, action-2))
           System.out.println ( "*** Invalid action " + action + " will cause out of field *** ");        		    	
	     
		double v = maze.move( agt, action-2 );          // actual movement, maze direction is from -2 to 2
        
        if( v != -1 ) {  // if valid move
            if (last==true && ImmediateReward==false)  //run out of time (without immediate reward)
                r = 0.0;
            else
                r = (double) maze.getReward (agt,ImmediateReward);                
            agent[agt].move (action-2, true);           // actually move, agent direction is from -2 to 2
        }
        else {   // invalid move
            r = 0.0;
            System.out.println ( "*** Invalid action " + action + " taken *** ");        	
            agent[agt].move (action-2, false);          // don't move, agent direction is from -2 to 2
        }

		if (Trace && r==1.0) System.out.println ( "Success");
		
		if (action!=-1) {
       	    maze.getSonar( agt, that_Sonar );    
    		maze.getAVSonar( agt, that_AVSonar );
        
        	for (int k = 0; k < 5; k++) {
           		this_Sonar[k] = that_Sonar[k];
           		this_AVSonar[k] = that_AVSonar[k];
        	}

	        this_bearing = ( 8 + maze.getTargetBearing( agt ) - maze.getCurrentBearing( agt ) ) % 8;
    	    this_targetRange = maze.getTargetRange( agt );
        
        	agent[agt].setNewState(this_Sonar, this_AVSonar, this_bearing, this_targetRange);
			agent[agt].setAction(action);	// set action
	    	agent[agt].setReward(r);
		    
   			if (r>agent[agt].getPrevReward()) {
				if (Trace) System.out.println("\nLearn from positive outcome");
	    		agent[agt].setReward(1);    // instead of r
	    		action = agent[agt].doSearchAction (LEARN,type);  // learn current action lead to reward
				agent[agt].reinforce ();
			}
			else			
			if (r==0 || r<=agent[agt].getPrevReward()) {
				if (Trace)	System.out.println("\nReset and Learn");
				agent[agt].setReward(r);                           // (or 1-r) marks as good action/
	    		agent[agt].resetAction ();                         // seek alternative actions
				action = agent[agt].doSearchAction(LEARN,type);    // learn alternative actions
				agent[agt].penalize();
			}		
		}
	}

    /* 
	 *	Method simulating the sense-act-learn cycle of TD-FALCON and BPN
	 */	
	 
	private void doStep (int agt, double lastReward, boolean last) //一步一步运行
    {
        int k;
        final int PERFORM=0;
        final int LEARN=1;
        final int INSERT=2;
        
        final int type=0;  //0-fuzzART 1-ART2
        
        double this_Q;
        double max_Q=0.0;
        double new_Q=0.0;
                    
        double [] this_Sonar = new double[5];
        double [] that_Sonar = new double[5];
        double [] this_AVSonar = new double[5];
        double [] that_AVSonar = new double[5];

        int x, y, px, py;
        int action;
      	int this_bearing;
       	double this_targetRange;        

        if (Trace) 
        System.out.println ( "\nSelecting action ....");

        do {
	        maze.getSonar( agt, that_Sonar );   //获得mines和边界的声纳信息
    	    maze.getAVSonar( agt, that_AVSonar );   //autonomous vehicle (AV) 获得其他智能体和边界的声纳信息
        
        	for( k = 0; k < 5; k++ ) {
            	this_Sonar[k] = that_Sonar[k];
            	this_AVSonar[k] = that_AVSonar[k];
        	}

        	this_bearing = ( 8 + maze.getTargetBearing( agt ) - maze.getCurrentBearing( agt ) ) % 8;//获得方向
        	this_targetRange = maze.getTargetRange( agt ); //获得目标范围

        	agent[agt].setState( this_Sonar, this_AVSonar, this_bearing, this_targetRange );    //给agent置入状态

 	        if (agent[agt].direct_access) //方向是否可用
	  	        action = agent[agt].doDirectAccessAction(agt, true, maze );   // action is from 0 to numAction
			else
 	        	action = agent[agt].doSelectValidAction( true, maze );        // action is from 0 to numAction
 	        			
            if (action==-1) {   // No valid action; deadend, backtrack       		    	               
	        	agent[agt].turn(4);
	        	maze.turn (agt,4);	        	
	     	}
	    } while (action==-1);
	    
	    if (!maze.withinField (agt, action-2))
        System.out.println ( "*** Invalid action " + action + " will cause out of field *** ");

        double v = maze.move( agt, action-2 );          // actual movement, maze direction is from -2 to 2
        double r;
        
        if( v != -1 ) {  // if valid move
            if (last==true && ImmediateReward==false)  //run out of time (without immediate reward)
                r = 0.0;
            else
                r = (double) maze.getReward (agt,ImmediateReward); //获得奖励
            agent[agt].move (action-2, true);           // actually move, agent direction is from -2 to 2
        }
        else {   // invalid move
            r = 0.0;
            System.out.println ( "*** Invalid action " + action + " taken *** ");        	
            agent[agt].move (action-2, false);          // don't move, agent direction is from -2 to 2
        }

		if (Trace && r==1.0) System.out.println ( "Success");
		
		//	Calculate new Q value from reward function if possible
		boolean new_Q_value_assigned = true;
		if (agent[agt].direct_access || ImmediateReward) {
			if (r==1.0) new_Q = 1.0;
			else if (r==0.0) new_Q = 0.0;
			else if (ImmediateReward) new_Q = r;
			else new_Q_value_assigned = false;
		}
		else
			new_Q_value_assigned = false;
				
		//	Estimate new Q value through TD formula
		if (!new_Q_value_assigned) {
				
			agent[agt].setAction( action);         
	    	this_Q = agent[agt].doSearchQValue( PERFORM, type );
	         	
        	double [] new_sonar = new double[5]; 
        	maze.getSonar( agt, new_sonar );
        	double [] new_av_sonar = new double[5];
        	maze.getAVSonar( agt, new_av_sonar );
        
        	int new_target_bearing = maze.getTargetBearing( agt );
        	int new_current_bearing = maze.getCurrentBearing( agt ); 
        	double new_target_range = maze.getTargetRange( agt );
        
        	agent[agt].setState( new_sonar, new_av_sonar, ( 8 + new_target_bearing - new_current_bearing ) % 8, new_target_range );        
        	max_Q = agent[agt].getMaxQValue(TDMethod, true, maze );
            
        	// learn QValue for this state and action
        	if(Bound==false) {
            	new_Q = this_Q + agent[agt].QAlpha * ( r + agent[agt].QGamma * max_Q - this_Q );//Q-FALCON or S-FALCON
	        	// thresholding - limit the Q value to 0 and 1
				// new_Q = 1.0/(double) (1.0 + (double) Math.exp (-5*(new_Q-0.5)));
				
				if (AVTYPE==TDFALCON) {
					if (new_Q<0) new_Q = 0;
        			if (new_Q>1) new_Q = 1;
        		}
        	}
        	else {
            	new_Q = this_Q + agent[agt].QAlpha * ( r + agent[agt].QGamma * max_Q - this_Q ) * (1 - this_Q);//BQ-FALCON or BS-FALCON

        		if (new_Q<0 || new_Q>1) {
	        		System.out.println ( "*** Bounded rule breached *** ");
	        		System.out.println ( "r = " + r + " this_Q = " + this_Q + " max_Q = " + max_Q + " new_Q = " + new_Q);
	        	}
	        	
			}
		}

		// Learning with state, action, and Q_value 
		
		if (Trace)
        System.out.println ( "\nLearning state action value ....");

        agent[agt].setState( this_Sonar, this_AVSonar, this_bearing, this_targetRange ); //set back to old state
        agent[agt].setAction( action ); 
        agent[agt].setReward( new_Q );
        
        if (agent[agt].direct_access)
	        agent[agt].doSearchAction( LEARN, type );
        else
        	agent[agt].doSearchQValue( LEARN, type );
            
        if (Trace) System.out.println ( "Action = " + action + " Reward = " + r + 
            " new_Q = " + new_Q + " max_Q = " + max_Q);     
    }
    
    private void doReset() //重置
    {
        int agt;

        maze.refreshMaze( agent_num );
        for( agt = 0; agt < agent_num; agt++ )
            agent[agt].setPrevReward( 0 );
        init_path();        
        p_field.doRefresh( maze );
        p_msg.setMessage( "");
    }           

	private void init_path()
	{
		int agt; 

		step = 0;
		path = new int[maxStep+1][agent_num][2];                
		minStep = new int[agent_num];

		for( agt = 0; agt < agent_num; agt++ )
		{
			maze.getCurrent( agt, path[step][agt] );
			minStep[agt] = maze.getRange( agt );
			agent[agt].init_path( maxStep);
		}
		p_field.setCurrentPath( maze, path[step], step );
		lastFlag = false;
	}

    private void doAuto() //自动
	{
        int sample = 0;
        int success = 0;
        int failure = 0;
        int time_out = 0;
        int conflict = 0;

        int agt;
        int total_step = 0;
        int total_min_step = 0;
        int [] numCode = new int[agent_num];
		double [] reward = new double[agent_num];
		boolean result;
		NumberFormat nf = NumberFormat.getInstance(); //返回当前默认语言环境的数字格式
		nf.setMaximumFractionDigits (2); //小数显示最多位数超出四舍五入
        get_values(); //从接口获取仿真参数

		if (maxTrial>1 || numRuns>1) { 
			graphic=false;
		    Trace = false;
       		for( agt = 0; agt < agent_num; agt++ )
              	agent[agt].setTrace( false );
        }    			
		else {
			graphic=true;
		    Trace = true;
		    for( agt = 0; agt < agent_num; agt++ )
              agent[agt].setTrace( true );        
		}			       

        try {
              pw_score = new PrintWriter( new FileOutputStream ( "score.txt"), true );
              pw_avg = new PrintWriter( new FileOutputStream ( "result.txt"), true );
        } catch ( IOException ex ) {}
        
 
	int numReadings = maxTrial/interval+1;
	
	double [] totalSuccess     = new double[numReadings];
	double [] totalHitMine     = new double[numReadings];
	double [] totalOutOfTime   = new double[numReadings];
	double [] totalNSteps      = new double[numReadings];
	double [] totalNCodes      = new double[numReadings];
	double [] totalSqSuccess   = new double[numReadings];
	double [] totalSqHitMine   = new double[numReadings];
	double [] totalSqOutOfTime = new double[numReadings];
	double [] totalSqNSteps    = new double[numReadings];
	double [] totalSqNCodes    = new double[numReadings];

	totalSuccess[0]    =0.0;
	totalHitMine[0]    =50.0;
	totalOutOfTime[0]  =50.0;
	totalNSteps[0]     =5.0;
	totalNCodes[0]     =0.0;
	totalSqSuccess[0]  =0.0;
	totalSqHitMine[0]  =2500.0;
	totalSqOutOfTime[0]=2500.0;
	totalSqNSteps[0]   =25.0;
	totalSqNCodes[0]   =0.0;
	
	for (int i=1; i<numReadings; i++) {
		totalSuccess[i]    =0.0;
		totalHitMine[i]    =0.0;
		totalOutOfTime[i]  =0.0;
		totalNSteps[i]     =0.0;
		totalNCodes[i]     =0.0;
		totalSqSuccess[i]  =0.0;
		totalSqHitMine[i]  =0.0;
		totalSqOutOfTime[i]=0.0;
		totalSqNSteps[i]   =0.0;
		totalSqNCodes[i]   =0.0;
	}
	
	int totalSteps  = 0;
 	Date st = new Date ();
	long start = st.getTime ();
	
	for (int run=0; run<numRuns; run++) {
		int rd=1;
		int trial = 0;
		int k;
		
		if (numRuns>1) {
        	if (AVTYPE==RFALCON) {
        		for( k = 0; k < agent_num; k++ )
            		agent[k] = new FALCON (k);
        	} 
        	else if (AVTYPE==TDFALCON) {
        		for( k = 0; k < agent_num; k++ )
            		agent[k] = new FALCON (k);
        	}
        	else if (AVTYPE==BPN) {   
        		for( k = 0; k < agent_num; k++ )
            		agent[k] = new BP (k);         	
    		}
    	
    		for( k = 0; k < agent_num; k++ )
            	agent[k].setParameters (AVTYPE, ImmediateReward);
        }
		
        while( trial < maxTrial ) 
        {
            maze.refreshMaze( agent_num );
            p_field.doRefresh( maze );
			init_path();            
			reward = maze.getReward(ImmediateReward);

            while( !maze.endState( Target_Moving ) && step < maxStep ) {
                
                if( step == ( maxStep - 1 ) ) 
                    lastFlag=true;
                
                for( agt = 0; agt < agent_num; agt++ )
                {
                	agent[agt].setPrevReward (reward[agt]);
                	
                    if( maze.endState( agt ) )
                        continue;
                    
					if( Target_Moving )
						maze.go_target();
                    
                    if (AVTYPE==RFALCON)
                    	doRStep( agt, ( double )( minStep[agt] / ( step + 1 ) ), lastFlag );
                    else
                        doStep( agt, ( double )( minStep[agt] / ( step + 1 ) ), lastFlag );
                         
                    agent[agt].decay();
                }
                step++;
                maze.getCurrent( path[step] );
                p_field.setCurrentPath( maze, path[step], step );
				reward = maze.getReward(ImmediateReward);

	            p_field.setCurrent( maze );
                for( agt = 0; agt < sonar_num; agt++ ) {
                    p_sonar[agt].setSonar( agt, maze );                
                    p_avsonar[agt].setSonar( agt, maze );
					p_bearing[agt].setBearing( agt, maze );
				}
   	            
   	            if (graphic) {
   	              	p_field.paintComponent( p_field.getGraphics() );
                	for( agt = 0; agt < sonar_num; agt++ ) {
                	  	p_sonar[agt].paintComponent( p_sonar[agt].getGraphics() );
   	                	p_avsonar[agt].paintComponent( p_avsonar[agt].getGraphics() );
            	        p_bearing[agt].paintComponent( p_bearing[agt].getGraphics() );
            	    }
            	
                	try {
                    	Thread.sleep( Delay, 0 );
                	} catch( Exception e ) {}
            	}
            }
            
            trial++;
            totalSteps += step;
			
			if( !Target_Moving )
			{
				for( agt = 0; agt < agent_num; agt++ )
				{
					numCode[agt] = agent[agt].getNumCode(); 
					if( reward[agt] == 1 ) 
					{
						success++;
						total_step += step;
						total_min_step += minStep[agt];
					}
					else if( step == maxStep ) 
						time_out++;
					else if( maze.isConflict( agt ) )
						conflict++;
					else 
						failure++;
				}          

				if( trial%interval==0 )
					sample = interval;
				else
					sample = trial % interval;
				p_msg.setMessage( "Success rate: " + success*100/(sample*agent_num) + "%  Hit Mine: " + failure*100/(sample*agent_num) + "%  Timeout: " + time_out*100/(sample*agent_num) + "%  Collision: " + conflict*100/(sample*agent_num) + "%" );

				if( trial%interval==0 ) 
				{
					double success_rate = success*100/(sample*agent_num);
					double failure_rate = failure*100/(sample*agent_num);
					double time_out_rate = time_out*100/(sample*agent_num);
					double n_steps = total_step/(double)total_min_step;
					double n_codes =numCode[0];
									
					totalSuccess[rd]  += success_rate;
					totalHitMine[rd]  +=failure_rate;
					totalOutOfTime[rd]+=time_out_rate;
					totalNSteps[rd]   +=n_steps;
					totalNCodes[rd]   +=n_codes;
					
					totalSqSuccess[rd]  +=success_rate*success_rate;
					totalSqHitMine[rd]  +=failure_rate*failure_rate;
					totalSqOutOfTime[rd]+=time_out_rate*time_out_rate;
					totalSqNSteps[rd]     +=n_steps*n_steps;
					totalSqNCodes[rd]     +=n_codes*n_codes;
					rd++;
					
					pw_score.println(trial + " " + success*100/(sample*agent_num) + " " + failure*100/(sample*agent_num) + " " + 
									 time_out*100/(sample*agent_num)  + " " + nf.format (total_step/(double)total_min_step) + " " + numCode[0]);

					System.out.println( "Trial " + trial + ": Success: " + success*100/(sample*agent_num) + "%  Hit Mine: " + failure*100/(sample*agent_num) + "%  Timeout: " + time_out*100/(sample*agent_num) + 
									    "% NSteps: " + " " + nf.format (total_step/(double)total_min_step) + " NCodes: " + numCode[0]);

					success = 0;
					failure = 0;
					time_out = 0;
					conflict = 0;
					total_step = 0;
					total_min_step = 0;
				}
			}
			else
			{
				result = false;
				for( agt = 0; agt < agent_num; agt++ )
				{
					numCode[agt] = agent[agt].getNumCode();
					if( reward[agt] == 1 )
					{
						result = true;
						p_msg.setMessage( "AV" + agt + " Success achieved in " + step + " steps" + " with " + numCode[agt] + " codes of agent " + agt );
						if (trial%interval==0) System.out.println( "AV" + agt + " Success: Target achieved in " + step + " steps" + " with " + numCode[agt] + " codes of agent " + agt );
					}
				}
				if( result )
					success++;
				else
				{
					failure++;
					p_msg.setMessage( "All fail!!!" );
					System.out.println( "All fail!!!" );
				}
			
				if( trial%interval==0 )
					sample = interval;
				else
					sample = trial%interval;

				p_msg.setMessage( "Trial " + trial + "  Success: " + (success*100/sample) + "%  Failure: " + (failure*100/sample) + "%" );

				if( trial%interval == 0 )
				{
					pw_score.println( trial + " " + success*100/sample + " " + failure*100/sample );
					success = 0;
					failure = 0;
				}
			}
            
			// decay for Epsilon-greedy strategy
			
			for( agt = 0; agt < agent_num; agt++ )
				if (agent[agt].QEpsilon > agent[agt].minQEpsilon)
                    agent[agt].QEpsilon -= agent[agt].QEpsilonDecay;
        	}   

		if (AVTYPE==RFALCON || AVTYPE==TDFALCON)        
	        for (agt = 0; agt < agent_num; agt++) {   
            	agent[agt].saveAgent( "rule.txt");            
        }
	}

 	Date et = new Date ();
 	long end = et.getTime ();

	float avgTime  = (end-start)/(float)(numRuns*agent_num);  // per agent per experiment
	float avgSteps = totalSteps/(float)(numRuns*agent_num);
		
	pw_avg.println("Trial" + "\t" + "SuccessRate" + "\t" + "StdDev" + "\t" +
				"HitMine" + "\t" + 
				"Timeout" + "\t"+  
				"NormalizedStep" + "\t" + 
				"NumberOfCodes");
				
		for (int i=0; i<numReadings; i++) {
			double es  = totalSuccess[i]/numRuns;
			double ess = totalSqSuccess[i]/numRuns;
			
			pw_avg.println (i*interval + "\t" + es + "\t\t" + 
			    nf.format (Math.sqrt (ess - es*es)) + "\t\t" +
				totalHitMine[i]/numRuns + "\t\t" + 
				totalOutOfTime[i]/numRuns + "\t\t"+  
				nf.format (totalNSteps[i]/numRuns) + "\t\t\t" + 
				totalNCodes[i]/numRuns);
		}
		
		pw_avg.println ("Average Time (msec)" + "\t : " + avgTime);
		pw_avg.println ("Average Number of Steps" + "\t : " + avgSteps);							 
		pw_avg.println ("Average Time per Step" + "\t : " + avgTime/avgSteps);
				
		if( pw_score != null ) pw_score.close();
        if( pw_avg   != null ) pw_avg.close();

        Trace = true;
    }
        
}


