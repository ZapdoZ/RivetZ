// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.*;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Rivet {

	private static boolean RUNNING=true;
	private DisplayModel display_model;
	private DisplayView display_view;
	private static Rivet theApp;
	private static DisplayFrame window;
	public final String program_version="RivetZ (Build 91.1)";
	public int vertical_scrollbar_value=0;
	public int horizontal_scrollbar_value=0;
	public boolean pReady=false;
	private int system=1;
	public final Font plainFont=new Font("SanSerif",Font.PLAIN,12);
	public final Font boldFont=new Font("SanSerif",Font.BOLD,12);
	public final Font italicFont=new Font("SanSerif",Font.ITALIC,12);
    public XPA xpaHandler=new XPA(this,10);	
    public XPA2 xpa2Handler=new XPA2(this);
    public XPB xpbHandler=new XPB(this, 65.79);
    public CROWD36 crowd36Handler=new CROWD36(this,40);	
    public FSK200500 fsk200500Handler=new FSK200500(this,200);
    public FSK2001000 fsk2001000Handler=new FSK2001000(this,200);
    public CIS3650 cis3650Handler=new CIS3650(this);
    public CCIR493 ccir493Handler=new CCIR493(this);
    public RTTY rttyHandler=new RTTY(this);
    public GW gwHandler=new GW(this);
    public FSKraw fskHandler=new FSKraw(this);
    //public RDFT rdftHandler=new RDFT(this);
    //public AT3x04 at3x04Handler=new AT3x04(this);
    public InputThread inputThread=new InputThread(this);
    private DataInputStream inPipeData;
	private PipedInputStream inPipe;
	private CircularDataBuffer circBuffer=new CircularDataBuffer();
	private WaveData waveData=new WaveData();
	private boolean logging=false;
	public FileWriter file;
	public FileWriter bitStreamFile;
	private boolean debug=false;
	private boolean soundCardInput=false;
	private boolean wavFileLoadOngoing=false;
	private boolean invertSignal=false;
	private int soundCardInputLevel=0;
	private boolean soundCardInputTemp;
	private boolean bitStreamOut=false;
	private boolean viewGWChannelMarkers=true;
	private int bitStreamOutCount=0;
	private List<Trigger> listTriggers=new ArrayList<Trigger>();
	private int activeTriggerCount=0;
	private boolean pauseDisplay=false;
	private boolean autoScroll=true;
	private long lastUserScroll=0;
	private boolean smallScreen=false;
	private boolean displayBadPackets=false;
	private boolean logInUTC=true;
	private List<Ship> listLoggedShips=new ArrayList<Ship>();
	
	// Mode names
	public final String[] MODENAMES ={
			"CROWD36",
			"XPA (10 Baud)",
			"XPA2",
			"XPA (20 Baud)",
			"Experimental",
			"CIS 36-50",
			"FSK200/500",
			"CCIR493-4",
			"FSK200/1000",
			"GW FSK (100 Baud)",
			"Baudot",
			"FSK (Raw)",
			"XPB"
			};
    
	public static void main(String[] args) {
		theApp=new Rivet();
		SwingUtilities.invokeLater(new Runnable(){public void run(){theApp.createGUI();}});
		// Get data from the sound card thread
		try	{
			// Connected a piped input stream to the piped output stream in the thread
			theApp.inPipe=new PipedInputStream(theApp.inputThread.getPipedWriter());
			// Now connect a data input stream to the piped input stream
			theApp.inPipeData=new DataInputStream(theApp.inPipe);
			}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,"Error in main()","Rivet", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
			}
		// The main loop
		while (RUNNING)	{
			if ((theApp.wavFileLoadOngoing)&&(theApp.pReady)) theApp.getWavData();
			else if ((theApp.inputThread.getAudioReady())&&(theApp.pReady)) theApp.getAudioData();
			else	{
				// Add the following so the thread doesn't eat all of the CPU time
				try	{Thread.sleep(1);}
				catch (Exception e)	{JOptionPane.showMessageDialog(null,"Error in main2()\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);}
			}
		}
		
	}
	
	// Setup the window //
	public void createGUI() {
		window=new DisplayFrame(program_version,this);
		Toolkit theKit=window.getToolkit();
		Dimension wndsize=theKit.getScreenSize();
		// Calculate the screen position and size in the form x,y,width,height
		int x=wndsize.width/6;
		int y=wndsize.height/6;
		int width=2*wndsize.width/3;
		int height=2*wndsize.height/3;
		window.setBounds(x,y,width,height);
		window.addWindowListener(new WindowHandler());
		display_model=new DisplayModel();
		display_view=new DisplayView(this);
		display_model.addObserver(display_view);
		window.getContentPane().add(display_view,BorderLayout.CENTER);
		window.setVisible(true);
		// If this width is less than 600 then this is a very small screen
		// so certain elements may need to be removed
		if (width<600) window.setSmallScreen();		
		// Make certain the program knows the GUI is ready
		pReady=true;
		}

	class WindowHandler extends WindowAdapter {
		public void windowClosing(WindowEvent e) {	
			}
		}

	public DisplayFrame getWindow()	{
		return window;	
		}

	public DisplayModel getModel() {
		return display_model;
		}

	public DisplayView getView() {
		return display_view;	
		}

	public void setSystem(int system) {
		this.system=system;
		// 10 Baud XPA
		if (system==1) xpaHandler.setBaudRate(10);
		// 20 Baud XPA
		else if (system==3) xpaHandler.setBaudRate(20);
	}

	public int getSystem() {
		return system;
	}
	
	public boolean isCROWD36()	{
		return system == 0;
	}
	
	public boolean isXPA_10()	{
		return system == 1;
	}
	
	public boolean isXPA_20()	{
		return system == 3;
	}
	
	public boolean isXPA2()	{
		return system == 2;
	}
	
	public boolean isExperimental()	{
		return system == 4;
	}
	
	public boolean isCIS3650()	{
		return system == 5;
	}
	
	public boolean isFSK200500()	{
		return system == 6;
		}
	
	public boolean isCCIR493()	{
		return system == 7;
		}
	
	public boolean isFSK2001000()	{
		return system == 8;
		}	
	
	public boolean isGW()	{
		return system == 9;
	}
	
	public boolean isRTTY()	{
		return system == 10;
	}
	
	public boolean isFSK()	{
		return system == 11;
	}

	public boolean isXPB(){
		return system==12;
	}
		
	// Tell the input thread to start to load a .WAV file
	public void loadWAVfile(String fileName)	{
		String disp;
		disp=getTimeStamp()+" Loading file "+fileName;
		writeLine(disp,Color.BLACK,italicFont);
		waveData=inputThread.startFileLoad(fileName);
		// Make sure the program knows this data is coming from a file
		waveData.setFromFile(true);
		// Clear the data buffer
		circBuffer.setBufferCounter(0);
		// Reset the system objects
		// CROWD36
		if (system==0) crowd36Handler.setState(0);
		// XPA
		else if ((system==1)||(system==3)) xpaHandler.setState(0);
		// XPA2
		else if (system==2) xpa2Handler.setState(0);
		// Experimental
		//else if (system==4)
		// CIS36-50
		else if (system==5) cis3650Handler.setState(0);
		// FSK200/500
		else if (system==6) fsk200500Handler.setState(0);
		// CCIR493-4
		else if (system==7) ccir493Handler.setState(0);
		// FSK200/1000
		else if (system==8) fsk2001000Handler.setState(0);	
		// GW
		else if (system==9) gwHandler.setState(0);
		// RTTY
		else if (system==10) rttyHandler.setState(0);
		// FSK (raw)
		else if (system==11) fskHandler.setState(0);
		//XPB
		else if (system==12) xpbHandler.setState(0);
		// Ensure the program knows we have a WAV file load ongoing
		wavFileLoadOngoing=true;
	}
	
	// This is called when the input thread is busy getting data from a WAV file
	private void getWavData()	{
		// Get the sample from the input thread
		try	{
			// Add the data from the thread pipe to the circular buffer
			circBuffer.addToCircBuffer(inPipeData.readInt());
			// Process this data
			processData();
    		// Update the progress bar
    		updateProgressBar();
			// Check if the file has now all been read but we need to process all the data in the buffer
			if (inputThread.getLoadingFileState()==false)	{
				int a;
				for (a=0;a<circBuffer.retMax();a++)	{
					processData();
					// Keep adding null data to the circular buffer to move it along
					circBuffer.addToCircBuffer(0);
				}
				// Check if there is anything left to display
				if (system==0)	{
					//if (crowd36Handler.getLineCount()>0) writeLine(crowd36Handler.getLineBuffer(),Color.BLACK,plainFont);
					writeLine(crowd36Handler.lowHighFreqs(),Color.BLACK,plainFont);
					crowd36Handler.toneResults();
				}
				else if (system==5)	{
					//writeLine(cis3650Handler.lineBuffer.toString(),Color.BLACK,plainFont);
				}
				else if (system==6)	{
					writeLine(fsk200500Handler.getQuailty(),Color.BLACK,plainFont);
				}
				else if (system==8)	{
					writeLine(fsk2001000Handler.getQuailty(),Color.BLACK,plainFont);
				}
				
				// Once the buffer data has been read we are done
				if (wavFileLoadOngoing)	{
					String disp=getTimeStamp()+" WAV file loaded and analysis complete ("+Long.toString(inputThread.getSampleCounter())+" samples read)";
					writeLine(disp,Color.BLACK,italicFont);		
					wavFileLoadOngoing=false;
					}
				}
			}
		catch (Exception e)	{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,"Error in getWavData()","Rivet", JOptionPane.ERROR_MESSAGE);
			}	
	}
	
	// This is called when the input thread is busy getting data from the sound card
	private void getAudioData()	{
			// Get the sample from the input thread
			try	{
				// Add the data from the thread pipe to the circular buffer
				circBuffer.addToCircBuffer(inPipeData.readInt());
				// Process this data
				processData();
	    		// Update the volume bar every 50 samples
	    		if (inputThread.getSampleCounter()%50==0) updateVolumeBar();
				}
			catch (Exception e)	{
				e.printStackTrace();
				JOptionPane.showMessageDialog(null,"Error in getAudioData()","Rivet", JOptionPane.ERROR_MESSAGE);
				}	
		}
	
			
	// A central data processing class
	private void processData ()	{		
		try	{
			boolean res=false;
			switch(system){
				case 0 -> res=crowd36Handler.decode(circBuffer,waveData); //CROWD36
				case 1, 3 -> res=xpaHandler.decode(circBuffer,waveData); //XPA1
				case 2 -> res=xpa2Handler.decode(circBuffer,waveData); //XPA2
				// case 4: res=	Experimental would go here
				case 5 -> res=cis3650Handler.decode(circBuffer,waveData); //CIS36-50
				case 6 -> res=fsk200500Handler.decode(circBuffer,waveData); // FSK200/500
				case 7 -> res=ccir493Handler.decode(circBuffer,waveData); //CCIR493-4
				case 8 -> res=fsk2001000Handler.decode(circBuffer,waveData); // FSK200/1000
				case 9 -> res=gwHandler.decode(circBuffer,waveData); // GW
				case 10 -> res=rttyHandler.decode(circBuffer,waveData); // RTTY
				case 11 -> res=fskHandler.decode(circBuffer,waveData); // FSK (raw)
				case 12 -> res=xpbHandler.decode(circBuffer, waveData); //XPB
			}

			// Tell the user there has been an error and stop the WAV file from loading
			if (!res)	{
				if (soundCardInput==false)	{
					inputThread.stopReadingFile();
					wavFileLoadOngoing=false;
					writeLine("Error Loading WAV File",Color.RED,theApp.boldFont);
				}
				
			}
			
		}
		catch (Exception e){
			StringWriter sw=new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String str=sw.toString();
			JOptionPane.showMessageDialog(null,"Error in processData()\n"+str,"Rivet", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	// Write a line to the debug file
	public void debugDump (String line)	{
	    try	{
	    	FileWriter dfile=new FileWriter("debug.csv",true);
	    	dfile.write(line);
	    	dfile.write("\r\n");
	    	dfile.flush();  
	    	dfile.close();
	    	}catch (Exception e)	{
	    		System.err.println("Error: " + e.getMessage());
	    		}
		}
	
	// Return a time stamp
	public String getTimeStamp() {
		Date now=new Date();
		DateFormat df=DateFormat.getTimeInstance();
		// If we are logging in UTC time then set the time zone to that
		// Other wise logs will be in local time
		if (logInUTC) df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df.format(now);
	}
	
	private void updateProgressBar ()	{
		window.progressBarUpdate(inputThread.returnFileLoadPercentage());
	}
	
	// Get the average current volume and modify this so it is a number between 0 and 100
	// which is then passed to the progress indicator on the status bar
	private void updateVolumeBar ()	{
		// Calculate as a percentage of 18000 (the max value)
		// Reduce this to 3500 to give a more useful display
		int pval=(int)(((float)inputThread.returnVolumeAverage()/(float)3000.0)*(float)100);
		window.progressBarUpdate(pval);
	}
	
	public void setStatusLabel (String st)	{
		window.setStatusLabel(st);
	}

	public void setLogging(boolean logging) {
		this.logging = logging;
	}

	public boolean getLogging() {
		return logging;
	}
	
	// Write to a string to the logging file
	public boolean fileWriteLine(String fline) {
		try {
			file.write(fline);
			file.write("\r\n");
			file.flush();
		} catch (Exception e) {
			// Stop logging as we have a problem
			logging=false;
			JOptionPane.showMessageDialog(null,"Error writing to the log file in fileWriteLine().\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	// Write a line char to the logging file
	public boolean fileWriteChar(String ch) {
		try {
			file.write(ch);
		} catch (Exception e) {
			// Stop logging as we have a problem
			logging=false;
			JOptionPane.showMessageDialog(null,"Error writing to the log file in fileWriteChar().\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}	
	
	// Write a newline to the logging file
	public boolean fileWriteNewline() {
		try {
			file.write("\r\n");
		} catch (Exception e) {
			// Stop logging as we have a problem
			logging=false;
			JOptionPane.showMessageDialog(null,"Error writing to the log file in fileWriteNewline().\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}	
	// Write a string to the bit stream file
	public boolean bitStreamWrite(String fline) {
		try {
			// Have only 80 bits per line
			bitStreamOutCount++;
			if (bitStreamOutCount==80)	{
				fline=fline+"\n";
				bitStreamOutCount=0;
			}
			bitStreamFile.write(fline);
		} catch (Exception e) {
			// We have a problem
			bitStreamOut=false;
			JOptionPane.showMessageDialog(null,"Error writing to the bit stream file.\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}	
	

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isSoundCardInput() {
		return soundCardInput;
	}

	// Change the sound source
	public void setSoundCardInput(boolean s) {
		// Try to close the audio device if it is already in operation
		if (this.soundCardInput) inputThread.closeAudio();
		// If the soundcard is already running we need to close it
		if (!s)	{
			this.soundCardInput=false;
		}
		else	{
			// CROWD36 , XPA , XPA2 , CIS36-50 , FSK200/500 , FSK200/1000 , CCIR493-4 , GW , RTTY , RDFT , Experimental, XPB
			if ((system==0)||(system==1)||(system==2)||(system==3)||(system==4)||(system==5)||(system==6)||(system==8)||(system==7)||(system==9)||(system==10)||(system==11)||(system==12))	{
				WaveData waveSetting=new WaveData();
				waveSetting.setChannels(1);
				waveSetting.setEndian(true);
				waveSetting.setSampleSizeInBits(16);
				waveSetting.setFromFile(false);
				waveSetting.setSampleRate(8000.0);
				waveSetting.setBytesPerFrame(2);
				inputThread.setupAudio(waveSetting);
				waveData=waveSetting;
				this.soundCardInput=true;	
			}
			
		}
	}
	
	public void setSoundCardInputOnly(boolean s)	{
		this.soundCardInput=s;
	}
	
	// Reset the decoder state
	public void resetDecoderState()	{
		switch(system){
			case 0 -> crowd36Handler.setState(0);// CROWD36
			case 1, 3 -> xpaHandler.setState(0);// XPA1
			case 2 -> xpa2Handler.setState(0); // XPA2
			// case 4: Experimental would go here
			case 5 -> cis3650Handler.setState(0); // CIS36-50
			case 6 -> fsk200500Handler.setState(0); // FSK200/500
			case 7 -> ccir493Handler.setState(0); // CCIR493-4
			case 8 -> fsk2001000Handler.setState(0); // FSK200/1000
			case 9 -> gwHandler.setState(0); // GW
			case 10 -> rttyHandler.setState(0); // RTTY
			case 11 -> fskHandler.setState(0); // FSK (raw)
			case 12 -> xpbHandler.setState(0); //XPB
		}
	}
	
	// Gets all the text on the screen and returns it as a string
	public String getAllText()	{
		return display_view.getText();
	}
	
	// Allows the user to set the CROWD36 high sync tone number
	public void getCROWD36SyncHighTone ()	{
		 // Create a panel that will contain the sync number
		 JPanel panel=new JPanel();
		 // Set JPanel layout using GridLayout
		 panel.setLayout(new GridLayout(2,1));
		 // Create a label with text (Username)
		 JLabel label=new JLabel("High Sync Tone Number (0 to 33)");
		// Create text field that will use to enter the high sync tone
		 JTextField toneField=new JTextField(2);
		 toneField.setText(Integer.toString(crowd36Handler.getSyncHighTone()));
		 panel.add(label);
		 panel.add(toneField);
		 panel.setVisible(true);
		 // Show JOptionPane that will ask user for this information
		 int resp=JOptionPane.showConfirmDialog(window,panel,"Enter the CROWD36 High Sync Tone Number",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		 if (resp==JOptionPane.OK_OPTION)	{
			 String sval=new String (toneField.getText());
			 crowd36Handler.setSyncHighTone(Integer.parseInt(sval));
		 }	
	}
	
	// A dialog box to allow the user to set CIS36-50 options
	public void setBEEOptions()	{
		 // Create a panel that contains the FSK and RTTY options
		 JPanel panel=new JPanel();
		 // Set JPanel layout using GridLayout
		 panel.setLayout(new GridLayout(2,1));
		 // Shift
		 JLabel labelShift=new JLabel("Shift : ");		
		 final String[] SHIFTS ={"75 Hz","200 Hz","250 Hz","400 Hz","500 Hz"};
		 JComboBox<String> shiftList=new JComboBox <String>(SHIFTS);
		 if (cis3650Handler.getShift()==75) shiftList.setSelectedIndex(0);
		 else if (cis3650Handler.getShift()==200) shiftList.setSelectedIndex(1);
		 else if (cis3650Handler.getShift()==250) shiftList.setSelectedIndex(2);
		 else if (cis3650Handler.getShift()==400) shiftList.setSelectedIndex(3);
		 else if (cis3650Handler.getShift()==500) shiftList.setSelectedIndex(4);
		 panel.add(labelShift);
		 panel.add(shiftList);
		 // Show JOptionPane that will ask user for this information
		 int resp=JOptionPane.showConfirmDialog(window,panel,"CIS36-50 Options",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		 // If the user has clicked on the OK option then change values in the RTTY object
	     if (resp==JOptionPane.OK_OPTION)	{
	    	if (shiftList.getSelectedIndex()==0) cis3650Handler.setShift(75);
	    	else if (shiftList.getSelectedIndex()==1) cis3650Handler.setShift(200);
	    	else if (shiftList.getSelectedIndex()==2) cis3650Handler.setShift(250);
	    	else if (shiftList.getSelectedIndex()==3) cis3650Handler.setShift(400);
	    	else if (shiftList.getSelectedIndex()==4) cis3650Handler.setShift(500);
	    }
	}
	
	// A dialog box to allow the user to set the FSK and RTTY options
	public void setRTTYOptions()	{
		 // Create a panel that contains the FSK and RTTY options
		 JPanel panel=new JPanel();
		 // Set JPanel layout using GridLayout
		 panel.setLayout(new GridLayout(3,2));
		 // Baud Rate
		 JLabel labelBaud=new JLabel("Baud Rate : ");		
		 final String[] BAUDRATES ={"45.5 baud","50 baud","75 baud","100 baud","145 baud","150 baud","200 baud","300 baud","600 baud"};
		 JComboBox<String> baudRateList=new JComboBox <String>(BAUDRATES);
		 if (rttyHandler.getBaudRate()==45.45) baudRateList.setSelectedIndex(0);
		 else if (rttyHandler.getBaudRate()==50) baudRateList.setSelectedIndex(1);
		 else if (rttyHandler.getBaudRate()==75) baudRateList.setSelectedIndex(2); 
		 else if (rttyHandler.getBaudRate()==100) baudRateList.setSelectedIndex(3);
		 else if (rttyHandler.getBaudRate()==145) baudRateList.setSelectedIndex(4);
		 else if (rttyHandler.getBaudRate()==150) baudRateList.setSelectedIndex(5); 
		 else if (rttyHandler.getBaudRate()==200) baudRateList.setSelectedIndex(6); 
		 else if (rttyHandler.getBaudRate()==300) baudRateList.setSelectedIndex(7); 
		 else if (rttyHandler.getBaudRate()==600) baudRateList.setSelectedIndex(8);
		 panel.add(labelBaud);
		 panel.add(baudRateList);
		 // Shift 
		 JLabel labelShift=new JLabel("Shift : ");		
		 final String[] SHIFTS ={"75 Hz","150 Hz","170 Hz","200 Hz","250 Hz","300 Hz","400 Hz","425 Hz","450 Hz","500 Hz","600 Hz","625 Hz","800 Hz","850 Hz","1000 Hz"};
		 JComboBox <String> shiftList=new JComboBox <String>(SHIFTS);
		 // Sets the List entry based on the current RTTY/FSK config
		 switch(rttyHandler.getShift()){
		 	case 75 -> shiftList.setSelectedIndex(0);
		 	case 150 -> shiftList.setSelectedIndex(1);
		 	case 170 -> shiftList.setSelectedIndex(2);
		 	case 200 -> shiftList.setSelectedIndex(3);
		 	case 250 -> shiftList.setSelectedIndex(4);
		 	case 300 -> shiftList.setSelectedIndex(5);
		 	case 400 -> shiftList.setSelectedIndex(6);
		 	case 425 -> shiftList.setSelectedIndex(7);
		 	case 450 -> shiftList.setSelectedIndex(8);
		 	case 500 -> shiftList.setSelectedIndex(9);
		 	case 600 -> shiftList.setSelectedIndex(10);
		 	case 625 -> shiftList.setSelectedIndex(11);
		 	case 800 -> shiftList.setSelectedIndex(12);
		 	case 850 -> shiftList.setSelectedIndex(13);
		 	case 1000 -> shiftList.setSelectedIndex(14);
		 }
		 panel.add(labelShift);
		 panel.add(shiftList);
		 // Stop Bits
		 JLabel labelStop=new JLabel("Stop Bits (Baudot only) : ");
		 final String[] STOPBITS = {"1 Bit","1.5 Bits","2 Bits","2.5 Bits"};
		 JComboBox <String> stopBitsList=new JComboBox <String>(STOPBITS);
		 if (rttyHandler.getStopBits()==1.0) stopBitsList.setSelectedIndex(0);
		 else if (rttyHandler.getStopBits()==1.5) stopBitsList.setSelectedIndex(1);
		 else if (rttyHandler.getStopBits()==2.0) stopBitsList.setSelectedIndex(2);
		 else if (rttyHandler.getStopBits()==2.5) stopBitsList.setSelectedIndex(3);
		 panel.add(labelStop);
		 panel.add(stopBitsList);
		 // Show JOptionPane that will ask user for this information
		 int resp=JOptionPane.showConfirmDialog(window,panel,"Baudot & FSK Options",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		 // If the user has clicked on the OK option then change values in the RTTY object
		 if (resp==JOptionPane.OK_OPTION)	{
			// Baud Rate
			 switch (baudRateList.getSelectedIndex()){
				 case 0 -> setRTTY_FSK_settings(2, 45.45);
				 case 1 -> setRTTY_FSK_settings(2, 50);
				 case 2 -> setRTTY_FSK_settings(2, 75);
				 case 3 -> setRTTY_FSK_settings(2, 100);
				 case 4 -> setRTTY_FSK_settings(2, 145);
				 case 5 -> setRTTY_FSK_settings(2, 150);
				 case 6 -> setRTTY_FSK_settings(2, 200);
				 case 7 -> setRTTY_FSK_settings(2, 300);
				 case 8 -> setRTTY_FSK_settings(2, 600);
			 }
			 // Shift
			 switch (shiftList.getSelectedIndex()){
			 	case 0 -> setRTTY_FSK_settings(1, 75);
				 case 1 -> setRTTY_FSK_settings(1, 150);
				 case 2 -> setRTTY_FSK_settings(1, 170);
				 case 3 -> setRTTY_FSK_settings(1, 200);
				 case 4 -> setRTTY_FSK_settings(1, 250);
				 case 5 -> setRTTY_FSK_settings(1, 300);
				 case 6 -> setRTTY_FSK_settings(1, 400);
				 case 7 -> setRTTY_FSK_settings(1, 425);
				 case 8 -> setRTTY_FSK_settings(1, 450);
				 case 9 -> setRTTY_FSK_settings(1, 500);
				 case 10 -> setRTTY_FSK_settings(1, 600);
				 case 11 -> setRTTY_FSK_settings(1, 625);
				 case 12 -> setRTTY_FSK_settings(1, 800);
				 case 13 -> setRTTY_FSK_settings(1, 850);
				 case 14 -> setRTTY_FSK_settings(1, 1000);
			 }
			// Stop Bits
			 switch (stopBitsList.getSelectedIndex())
				 {
				 	 case 0 -> rttyHandler.setStopBits(1.0);
					 case 1 -> rttyHandler.setStopBits(1.5);
					 case 2 -> rttyHandler.setStopBits(2.0);
					 case 3 -> rttyHandler.setStopBits(2.5);
				 }
		}
	}

	public void setXPBOptions(){
		// Create a panel that contains the FSK and RTTY options
		JPanel panel=new JPanel();
		// Set JPanel layout using GridLayout
		panel.setLayout(new GridLayout(2,2));
		// Starting offet
		JLabel lbStartingOffset= new JLabel("Starting offset: ");
		JTextField StartingOffsetField=new JTextField(1);
		StartingOffsetField.setText(Integer.toString(xpbHandler.getStartingoffset()));
		panel.add(lbStartingOffset);
		panel.add(StartingOffsetField);
		// Starting symbol
		JLabel lbStartingSymbol = new JLabel("Starting Symbol: ");
		final String[] Symbols = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
		JComboBox<String> StartingSymbolList=new JComboBox <String>(Symbols);
		StartingSymbolList.setSelectedIndex(xpbHandler.getStartingSymbol_hex());
		panel.add(lbStartingSymbol);
		panel.add(StartingSymbolList);
		int resp=JOptionPane.showConfirmDialog(window,panel,"XPB Decoder Options",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		if (resp==JOptionPane.OK_OPTION){
			try{
			xpbHandler.setStartingoffset(Integer.parseInt(StartingOffsetField.getText()));}
			catch (Exception e){
				xpbHandler.setStartingoffset(64);
				xpbHandler.setStartingsymbol("8");
				return;
			}
			xpbHandler.setStartingsymbol(Integer.toString(StartingSymbolList.getSelectedIndex(), 16).toUpperCase());
		}
	}
	public void setRTTY_FSK_settings(int whatToSet, double value){
		switch (whatToSet) {
			// 1 = Shift
			case 1 -> {
				rttyHandler.setShift((int) value);
				fskHandler.setShift((int) value);
			}
			// 2 = Baud Rate
			case 2 -> {
				rttyHandler.setBaudRate(value);
				fskHandler.setBaudRate(value);
			}
		}

	}

	public boolean isInvertSignal() {
		return invertSignal;
	}

	public void setInvertSignal(boolean invertSignal) {
		this.invertSignal = invertSignal;
	}
	
	// Save the programs settings in the rivet_settings.xml file
	public void saveSettings()	{
		FileWriter xmlfile;
		String line;
		// Open the default file settings //
		try {
			xmlfile=new FileWriter("rivet_settings.xml");
			// Start the XML file //
			line="<?xml version='1.0' encoding='utf-8' standalone='yes'?>\n<settings>\n";
			xmlfile.write(line);
			// Invert
			line="<invert val='";
			if (invertSignal) line=line+"TRUE";
			else line=line+"FALSE";
			line=line+"'/>\n";
			xmlfile.write(line);
			// Debug mode
			line="<debug val='";
			if (debug) line=line+"TRUE";
			else line=line+"FALSE";
			line=line+"'/>\n";
			xmlfile.write(line);
			// Mode
			line="<mode val='"+Integer.toString(system)+"'/>\n";
			xmlfile.write(line);
			// CROWD36 sync tone
			line="<c36tone val='"+Integer.toString(crowd36Handler.getSyncHighTone())+"'/>\n";
			xmlfile.write(line);
			// Soundcard Input Level
			line="<soundcard_level val='"+Integer.toString(soundCardInputLevel)+"'/>\n";
			xmlfile.write(line);
			// Soundcard Input
			if (soundCardInput) line="<soundcard_input val='1'/>\n";
			else line="<soundcard_input val='0'/>\n";
			xmlfile.write(line);
			// View GW Free Channel Markers
			if (viewGWChannelMarkers) line="<view_gw_markers val='1'/>\n";
			else line="<view_gw_markers val='0'/>\n";
			xmlfile.write(line);
			// RTTY & FSK
			// Baud
			line="<rttybaud val='"+Double.toString(rttyHandler.getBaudRate())+"'/>\n";
			xmlfile.write(line);
			// Shift
			line="<rttyshift val='"+Integer.toString(rttyHandler.getShift())+"'/>\n";
			xmlfile.write(line);
			// Stop bits
			line="<rttystop val='"+Double.toString(rttyHandler.getStopBits())+"'/>\n";
			xmlfile.write(line);			
			// Save the current audio source
			line="<audioDevice val='"+inputThread.getMixerName()+"'/>\n";
			xmlfile.write(line);
			// Display bad packets
			if (displayBadPackets) line="<display_bad_packets val='1'/>\n";
			else line="<display_bad_packets val='0'/>\n";
			xmlfile.write(line);
			// Show UTC time
			if (logInUTC) line="<UTC val='1'/>\n";
			else line="<UTC val='0'/>\n";
			xmlfile.write(line);
			// CIS36-50 shift
			line="<cis3650shift val='"+Integer.toString(cis3650Handler.getShift())+"'/>\n";
			xmlfile.write(line);
			// All done so close the root item //
			line="</settings>";
			xmlfile.write(line);
			// Flush and close the file //
			xmlfile.flush();
			xmlfile.close();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null,"Error : Unable to create the file rivet_settings.xml\n"+e.toString(),"Rivet", JOptionPane.ERROR_MESSAGE);
			}
	}
	
	// Read in the rivet_settings.xml file //
	public void readDefaultSettings() throws SAXException, IOException,ParserConfigurationException {
			// Create a parser factory and use it to create a parser
			SAXParserFactory parserFactory=SAXParserFactory.newInstance();
			SAXParser parser=parserFactory.newSAXParser();
			// This is the name of the file you're parsing
			String filename="rivet_settings.xml";
			// Instantiate a DefaultHandler subclass to handle events
			DefaultXMLFileHandler handler=new DefaultXMLFileHandler();
			// Start the parser. It reads the file and calls methods of the handler.
			try {
				parser.parse(new File(filename), handler);
			}
			catch (java.io.FileNotFoundException e){
				JOptionPane.showMessageDialog(null,"Error loading rivet_settings.xml - does it exist?","Rivet",JOptionPane.INFORMATION_MESSAGE);
			}
		}

	
	// This class handles the rivet_settings.xml SAX events
	public class DefaultXMLFileHandler extends DefaultHandler {
			String value;
			
			public void endElement(String namespaceURI,String localName,String qName) throws SAXException {	
			}

			public void characters(char[] ch,int start,int length) throws SAXException {
				// Extract the element value as a string //
				String tval=new String(ch);
				value=tval.substring(start,(start+length));
			}
			
			// Handle an XML start element //
			public void startElement(String uri, String localName, String qName,Attributes attributes) throws SAXException {
				// Check an element has a value //
				if (attributes.getLength()>0) {
					// Get the elements value //
					String aval=attributes.getValue(0);
					// Debug mode //
					if (qName.equals("debug")) {
						if (aval.equals("TRUE")) setDebug(true);
						else setDebug(false);	
					}
					// Invert
					else if (qName.equals("invert")) {
						if (aval.equals("TRUE")) setInvertSignal(true);
						else setInvertSignal(false);	
					}
					// Mode
					else if (qName.equals("mode"))	{
						system=Integer.parseInt(aval);
					}
					// Crowd36 sync tone
					else if (qName.equals("c36tone"))	{
						crowd36Handler.setSyncHighTone(Integer.parseInt(aval));
					}
					// Soundcard input level
					else if (qName.equals("soundcard_level"))	{
						soundCardInputLevel=Integer.parseInt(aval);
						// Check if this is to high or to low
						if (soundCardInputLevel<-10) soundCardInputLevel=-10;
						else if (soundCardInputLevel>10) soundCardInputLevel=10;
					}
					// Soundcard input
					else if (qName.equals("soundcard_input"))	{
						if (Integer.parseInt(aval)==1) soundCardInputTemp=true;
						else soundCardInputTemp=false;
					}
					// View GW Free Channel Markers
					else if (qName.equals("view_gw_markers"))	{
						if (Integer.parseInt(aval)==1) viewGWChannelMarkers=true;
						else viewGWChannelMarkers=false;
					}
					// RTTY & FSK Options
					// Baud rate
					else if (qName.equals("rttybaud"))	{
						rttyHandler.setBaudRate(Double.parseDouble(aval));
						fskHandler.setBaudRate(Double.parseDouble(aval));
					}
					// Shift
					else if (qName.equals("rttyshift"))	{
						rttyHandler.setShift(Integer.parseInt(aval));
						fskHandler.setShift(Integer.parseInt(aval));
					}
					// Stop bits
					else if (qName.equals("rttystop"))	{
						rttyHandler.setStopBits(Double.parseDouble(aval));
					}
					// The audio input source
					else if (qName.equals("audioDevice"))	{
						if (inputThread.changeMixer(aval)==false) {
							JOptionPane.showMessageDialog(null,"Read XML Error changing mixer\n"+inputThread.getMixerErrorMessage()+"\n"+aval,"Rivet",JOptionPane.ERROR_MESSAGE);
						}
					}
					// Display bad packets
					else if (qName.equals("display_bad_packets"))	{
						if (Integer.parseInt(aval)==1) displayBadPackets=true;
						else displayBadPackets=false;
					}
					// Show UTC time
					else if (qName.equals("UTC"))	{
						if (Integer.parseInt(aval)==1) logInUTC=true;
						else logInUTC=false;
					}
					// CIS36-50 Shift
					else if (qName.equals("cis3650shift"))	{
						cis3650Handler.setShift(Integer.parseInt(aval));
					}
					
				}	
				
			}
		}
	
	
	// Change the invert setting
	public void changeInvertSetting ()	{
		invertSignal = !invertSignal;
	}
	
	// Set the soundcard input level in the input thread
	public void setSoundCardLevel (int sli)	{
		soundCardInputLevel=sli;
		// Pass this to the input thread
		inputThread.setInputLevel(sli);
	}
	
	// Returns the current sound card input level
	public int getSoundCardLevel()	{
		return soundCardInputLevel;
	}
	
	public boolean issoundCardInputTemp()	{
		return soundCardInputTemp;
	}

	public boolean isBitStreamOut() {
		return bitStreamOut;
	}

	public void setBitStreamOut(boolean bitStreamOut) {
		this.bitStreamOut = bitStreamOut;
	}

	public boolean isViewGWChannelMarkers() {
		return viewGWChannelMarkers;
	}

	public void setViewGWChannelMarkers(boolean viewGWChannelMarkers) {
		this.viewGWChannelMarkers = viewGWChannelMarkers;
	}
	
	public void clearBitStreamCountOut()	{
		bitStreamOutCount=0;
	}
	
	// Adds a line to the display
	public void writeLine(String line,Color col,Font font) {
		if (line!=null)	{
			if (logging) fileWriteLine(line);
			if (!pauseDisplay) display_view.addLine(line,col,font);
		}
	}
	
	// Adds a single char to the current line on the display
	public void writeChar (String ct,Color col,Font font)	{
		if (ct!=null)	{
			if (!pauseDisplay) display_view.addChar(ct,col,font);
			if (logging) fileWriteChar(ct);
		}
	}

	public String getcurrentLine(){
		return display_view.getcurrentLine();
	}

	// Deletes the current line from the Display
	public void deletecurrentLine(){
		display_view.deletecurrentLine();
	}
	
	// Clear the display screen
	public void clearScreen()	{
		display_view.clearScreen();
	}
	
	// Writes a new line to the screen
	public void newLineWrite()	{
		if (!pauseDisplay) display_view.newLine();
		if (logging) fileWriteNewline();
	}

	public List<Trigger> getListTriggers() {
		return listTriggers;
	}

	public void setListTriggers(List<Trigger> listTriggers) {
		this.listTriggers = listTriggers;
		// Count the number of active triggers
		activeTriggerCount=0;
		int a;
		for (a=0;a<listTriggers.size();a++)	{
			if (listTriggers.get(a).isActive()) activeTriggerCount++;
		}
	}
	
	// Read in the trigger.xml file //
	public void readTriggerSettings() throws SAXException, IOException,ParserConfigurationException {
			// Create a parser factory and use it to create a parser
			SAXParserFactory parserFactory=SAXParserFactory.newInstance();
			SAXParser parser=parserFactory.newSAXParser();
			// This is the name of the file you're parsing
			String filename="trigger.xml";
			// Instantiate a DefaultHandler subclass to handle events
			TriggerXMLFileHandler handler=new TriggerXMLFileHandler();
			// Start the parser. It reads the file and calls methods of the handler.
			parser.parse(new File(filename),handler);
		}

	
	public int getActiveTriggerCount() {
		return activeTriggerCount;
	}

	// This class handles the rivet_settings.xml SAX events
	public class TriggerXMLFileHandler extends DefaultHandler {
			String value,description,sequence;
			int type,backward,forward;
			// Handle an XML start element
			public void endElement(String namespaceURI,String localName,String qName) throws SAXException {	
				// Look for a <trigger> end tag
				if (qName.equals("trigger"))	{
					// Put the values in a Trigger object
					Trigger trigger=new Trigger();
					trigger.setTriggerDescription(description);
					trigger.setTriggerSequence(sequence);
					trigger.setTriggerType(type);
					// If type 3 (GRAB) load the forward and backward values
					if (type==3)	{
						trigger.setForwardGrab(forward);
						trigger.setBackwardGrab(backward);
					}
					// Add this to the Trigger list
					listTriggers.add(trigger);
				}
			}

			public void characters(char[] ch,int start,int length) throws SAXException {
				// Extract the element value as a string //
				String tval=new String(ch);
				value=tval.substring(start,(start+length));
			}
			
			// Handle an XML start element //
			public void startElement(String uri, String localName, String qName,Attributes attributes) throws SAXException {
				// Check an element has a value //
				if (attributes.getLength()>0) {
					// Get the elements value //
					String aval=attributes.getValue(0);
					// Trigger Description //
					if (qName.equals("description")) {
						description=aval;
					}
					// Trigger Sequence
					if (qName.equals("sequence")) {
						sequence=aval;
					}					
					// Trigger Type
					if (qName.equals("type"))	{
						type=Integer.parseInt(aval);
					}
					// Forward grab value
					if (qName.equals("forward"))	{
						forward=Integer.parseInt(aval);
					}
					// Backward grab value
					if (qName.equals("backward"))	{
						backward=Integer.parseInt(aval);
					}
				}	
				
			}
		}
	
	// Save the current Trigger list to the file trigger.xml
	public boolean saveTriggerXMLFile () 	{
		try	{
			FileWriter xmlfile;
			String line;
			xmlfile=new FileWriter("trigger.xml");
			// Start the XML file //
			line="<?xml version='1.0' encoding='utf-8' standalone='yes'?>";
			xmlfile.write(line);
			line="\n<settings>";
			xmlfile.write(line);
			// Run through each Trigger object in the list
			int a;
			for (a=0;a<listTriggers.size();a++)	{
				line="\n <trigger>";
				xmlfile.write(line);
				// Description
				line="\n  <description val='"+listTriggers.get(a).getTriggerDescription()+"'/>";
				xmlfile.write(line);
				// Sequence
				line="\n  <sequence val='"+listTriggers.get(a).getTriggerSequence()+"'/>";
				xmlfile.write(line);
				// Type
				line="\n  <type val='"+Integer.toString(listTriggers.get(a).getTriggerType())+"'/>";
				xmlfile.write(line);
				// If a GRAB (type 3) then save the forward and backward values
				if (listTriggers.get(a).getTriggerType()==3)	{
					// Forward bits
					line="\n  <forward val='"+Integer.toString(listTriggers.get(a).getForwardGrab())+"'/>";
					xmlfile.write(line);
					// Backward bits
					line="\n  <backward val='"+Integer.toString(listTriggers.get(a).getBackwardGrab())+"'/>";
					xmlfile.write(line);	
				}
				line="\n </trigger>";
				xmlfile.write(line);
			}
			// All done so close the root item //
			line="\n</settings>";
			xmlfile.write(line);
			// Flush and close the file //
			xmlfile.flush();
			xmlfile.close();
			
		}
		catch (Exception e)	{
			debugDump("Error writing Triger.xml :"+e.toString());
			return false;
		}
		return true;
	}
	
	// Change the audio mixer
	public boolean changeMixer(String mixerName)	{
		// Tell the audio in thread to change its mixer
		return inputThread.changeMixer(mixerName);
	}

	public boolean isPauseDisplay() {
		return pauseDisplay;
	}

	public void setPauseDisplay(boolean pauseDisplay) {
		this.pauseDisplay = pauseDisplay;
	}

	public boolean isAutoScroll() {
		return autoScroll;
	}

	public void setAutoScroll(boolean autoScroll) {
		this.autoScroll = autoScroll;
	}	
	
	// Return the current height of the window
	public int getCurrentHeight ()	{
		return window.getBounds().height;
	}
	
	// Tell the window to scroll down by a set amount
	public void scrollDown(int v)	{
		window.scrollDown((v-window.getBounds().height)+200);
	}
	
	// Return if the vertical scroll bar is being adjusted
	public boolean isAdjusting()	{
		return window.isAdjusting();
	}

	public long getLastUserScroll() {
		return lastUserScroll;
	}

	public void setLastUserScroll(long lastUserScroll) {
		this.lastUserScroll = lastUserScroll;
	}
	
	// Write system information for diagnostic purposes to the screen
	public void displaySystemInfo ()	{
		// First clear the screen
		clearScreen();
		// Version
		writeLine(program_version,Color.BLACK,theApp.boldFont);
		// Cores
		String cores="Available processors (cores): "+Runtime.getRuntime().availableProcessors();
		writeLine(cores,Color.BLACK,theApp.boldFont);
		// Memory available to the JVM
		String jmem="JVM Free memory (bytes): "+Runtime.getRuntime().freeMemory();
		writeLine(jmem,Color.BLACK,theApp.boldFont);
		// OS
		String os="OS : "+System.getProperty("os.name")+" ("+System.getProperty("os.version")+")";
		writeLine(os,Color.BLACK,theApp.boldFont);
		// Screen info
		Toolkit theKit=window.getToolkit();
		Dimension wndsize=theKit.getScreenSize();
		String res="Screen Resolution - Width "+Integer.toString(wndsize.width)+" : Height "+Integer.toString(wndsize.height);
		writeLine(res,Color.BLACK,theApp.boldFont);
		// Java version
		String jver="Java : "+System.getProperty("java.vendor")+" ("+System.getProperty("java.version")+")";
		writeLine(jver,Color.BLACK,theApp.boldFont);
		// Folder
		String folder="Working directory : "+System.getProperty("user.dir");
		writeLine(folder,Color.BLACK,theApp.boldFont);
		// Current Time
		String time="Current Time : "+getTimeStamp();
		writeLine(time,Color.BLACK,theApp.boldFont);
		// Write all of this to clipboard
		String contents=getAllText();
		window.setClipboard(contents);
	}

	public boolean isSmallScreen() {
		return smallScreen;
	}

	public void setSmallScreen(boolean smallScreen) {
		this.smallScreen = smallScreen;
	}

	public boolean isDisplayBadPackets() {
		return displayBadPackets;
	}

	public void setDisplayBadPackets(boolean displayBadPackets) {
		this.displayBadPackets = displayBadPackets;
	}

	public boolean isLogInUTC() {
		return logInUTC;
	}

	public void setLogInUTC(boolean logInUTC) {
		this.logInUTC = logInUTC;
	}
	
	// Clear the list of logged ships
	public void clearLoggedShipsList()	{
		listLoggedShips.clear();
	}
	
	// Given a ships MMSI check if it is in ships.xml or not and log it
	public void logShip (String mmsi)	{
		UserIdentifier uid=new UserIdentifier();
		// First check if a ship has already been logged and is in the list
		int a;
		for (a=0;a<listLoggedShips.size();a++)	{
			if (listLoggedShips.get(a).getMmsi().equals(mmsi))	{
				// Increment the log count and return
				listLoggedShips.get(a).incrementLogCount();
				return;
			}
		}
		// Now check if the ship is in ships.xml
		Ship loggedShip=uid.getShipDetails(mmsi);
		// If null then we need to create a ship object
		if (loggedShip==null)	{
			Ship newShip=new Ship();
			newShip.setMmsi(mmsi);
			newShip.incrementLogCount();
			listLoggedShips.add(newShip);
		}
		else	{
			// Increment the ship objects log counter and add it to the list
			loggedShip.incrementLogCount();
			listLoggedShips.add(loggedShip);
		}	
	}
	
	// Return a list of all logged ships
	public String getShipList ()	{
		StringBuilder sb=new StringBuilder();
		// No ships logged
		if (listLoggedShips.isEmpty()) return "\r\n\r\nNo ships were logged.";
		// Show the number of ships logged
		if (listLoggedShips.size()==1) sb.append("\r\n\r\nYou logged one ship.");
		else sb.append("\r\n\r\nYou logged "+Integer.toString(listLoggedShips.size())+" ships.");
		// Display the ships
		int a;
		for (a=0;a<listLoggedShips.size();a++)	{
			// MMSI
			sb.append("\r\nMMSI "+listLoggedShips.get(a).getMmsi());
			// Name and flag (if we have them)
			if (listLoggedShips.get(a).getName()!=null)	{
				sb.append(" "+listLoggedShips.get(a).getName()+" "+listLoggedShips.get(a).getFlag());
			}
			// Number of times logged
			sb.append(" ("+Integer.toString(listLoggedShips.get(a).getLogCount())+")");
		}
		return sb.toString();
	}
	
	
}
