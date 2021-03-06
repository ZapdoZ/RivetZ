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

import javax.swing.JComponent;
import java.util.Observer;
import java.util.Observable;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class DisplayView extends JComponent implements Observer {
	
	public static final long serialVersionUID=1;
	private static final int DISPLAYCOUNT=329;
	private String displayString[]=new String[DISPLAYCOUNT];
	private Color displayColour[]=new Color[DISPLAYCOUNT];
	private Font displayFont[]=new Font[DISPLAYCOUNT];
	private int displayCounter=0;
	private Rivet theApp;	
	
	public DisplayView (Rivet theApp) {
		this.theApp=theApp;	
		//screenTest();
	}
			
	public void update (Observable o,Object rectangle)	{			
	}
			
	// Draw the main screen //
	public void paint (Graphics g) {
		// Oldest line first
		int count=0,pos=20,i=displayCounter+1;
		// Check it hasn't reached its maximum size
		if (i>=DISPLAYCOUNT) i=0;
		Graphics2D g2D=(Graphics2D)g;	
		// Draw in the lines on the screen
		// taking account of the fact that the data is stored in a circular buffer
		// we need to display the oldest line stored first and then go backwards from
		// that point onwards
		while(count<DISPLAYCOUNT)	{
			// Only display info if something is stored in the display string
			if (displayString[i]!=null)	{
				g.setColor(displayColour[i]);
				g.setFont(displayFont[i]);
				g2D.drawString(displayString[i].toString(),(5-theApp.horizontal_scrollbar_value),(pos-theApp.vertical_scrollbar_value));	
				pos=pos+20;
			}	
			i++;
			if (i==DISPLAYCOUNT) i=0;
			count++;
		}	
		// Detect if the last line written was outside the current viewing area
		if ((theApp.isAutoScroll()==true)&&(theApp.isAdjusting()==false))	{
			if ((pos-theApp.vertical_scrollbar_value)>theApp.getCurrentHeight())	{
				theApp.scrollDown(pos);
				repaint();
			}
		}
		
	}
	
	// Add a line to the display circular buffer //
	public void addLine (String line,Color tcol,Font tfont) {		
		// There may be data on the current line which was added by addChar() so we need to move to the next line
		// Increment the circular buffer
		displayCounter++;
		// Check it hasn't reached its maximum size
		if (displayCounter==DISPLAYCOUNT) displayCounter=0;
		// Add this line
		displayString[displayCounter]=line;
		displayColour[displayCounter]=tcol;
		displayFont[displayCounter]=tfont;
		// Test if autoscroll needs to be on
		theApp.setAutoScroll(autoScrollSet());
		// Repaint the screen
		repaint();
	}
	
	// Gets all the text on the screen and returns it as a string
	public String getText()	{
		StringBuilder buffer=new StringBuilder();
		int i=displayCounter+1,count=0;
		// Check it hasn't reached its maximum size
		if (i>=DISPLAYCOUNT) i=0;
		while(count<DISPLAYCOUNT)	{
			if (displayString[i]!=null)	{
				buffer.append(displayString[i]);
				buffer.append("\n");
			}	
			i++;
			if (i>=DISPLAYCOUNT) i=0;
			count++;
		}
		return buffer.toString();
	}
	// Gets current line from Display
	public String getcurrentLine(){
		return displayString[displayCounter];
	}

	// Deletes the current line from the Display
	public void deletecurrentLine(){
		displayString[displayCounter-1]="";
		displayCounter = displayCounter-2; //Does kinda weird stuff but seems ok so far
	}
	
	// Adds a single character to the current line
	public void addChar (String ch,Color col,Font font)	{
		if (displayString[displayCounter]==null) displayString[displayCounter]="";
		StringBuilder sb=new StringBuilder(displayString[displayCounter]);
        sb.append(ch);
		displayColour[displayCounter]=col;
		displayFont[displayCounter]=font;
		displayString[displayCounter]=sb.toString();
		// Test if autoscroll needs to be on
		theApp.setAutoScroll(autoScrollSet());
		// Redraw
		repaint();
	}
	
	// Newline
	public void newLine ()	{
		// Increment the circular buffer
		displayCounter++;
		// Check it hasn't reached its maximum size
		if (displayCounter==DISPLAYCOUNT) displayCounter=0;
		displayString[displayCounter]="";
		// Test if auto scroll needs to be on
		theApp.setAutoScroll(autoScrollSet());
		// Redraw
		repaint();
	}
	
	// Clear the display screen
	public void clearScreen	()	{
		int a;
		displayCounter=0;
		for (a=0;a<DISPLAYCOUNT;a++)	{
			displayString[a]=null;
		}
		// Scroll right back up the top
		theApp.scrollDown(0);
		// Repaint
		repaint();
	}
	
	// A screen display test routine
	private void screenTest()	{
		int a;
		for (a=0;a<DISPLAYCOUNT;a++)	{
			String line="Line "+Integer.toString(a)+" test";
			addLine(line,Color.BLACK,theApp.italicFont);
		}
	}
	
	// Check if autoscroll should be turned on
	private boolean autoScrollSet()	{
		// Get the current time
		long currentTime=System.currentTimeMillis()/1000;
		// Is it 30 seconds or more since the last user scroll operation
		// if so return true
		if (currentTime-theApp.getLastUserScroll()>=30) return true;
		else return false;
	}
	

}
