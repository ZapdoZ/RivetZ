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

import javax.swing.*;
import java.awt.*;

public class XPB extends MFSK {

	private double baudRate=65.79;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	private long sampleCount=0;
	private long syncingsampleCount=0;;
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private long syncFoundPoint;
	private int correctionFactor;
	private int eightcounter = 0;
	private int currentsampleoffset = 121;
	private double deviation;

	public XPB(Rivet tapp, double baud)	{
		baudRate=baud;
		theApp=tapp;
	}

	// Change the state and update the status label
	public void setState(int state) {
		this.state=state;
		// Change the status label
		if (state==1) theApp.setStatusLabel("Waiting for start tone 8");
		else if (state==2) theApp.setStatusLabel("Decoding");
	}

	public int getState() {
		return state;
	}

	// The main decode routine
	public boolean decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()>11025)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nXPA recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.getSampleRate());
			System.out.println(waveData.getSampleRate());
			setState(1);
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			correctionFactor=0;
			currentsampleoffset = 1;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			theApp.writeLine("XPB, as of now, is in an experimental stage. Decodes come at absolutely no warranty", Color.RED, theApp.italicFont);
			return true;
		}
		// Hunting for a start tone
		if (state==1)	{
				/* TODO: Support for Custom starting numbers (will maybe add in later version)*/
			String dout;
			dout = startToneHunt(circBuf, waveData);
			if (dout!=null){
				theApp.writeLine(dout, Color.BLACK, theApp.italicFont);
				theApp.newLineWrite();
				}
			}
		// Start decoding
		// There is really nothing to set on XPB so I hope this works out
		if (state==2)	{
			// Only do this at the end of each symbol
			// For this to work the audio needs a fixed starting position
			if (syncingsampleCount%currentsampleoffset == 0) {
				int freq = doXPBFFT(circBuf, waveData, 0);
				// freq = freq + correctionFactor;
				displayMessage(freq, waveData.isFromFile());
				currentsampleoffset = getNextSampleOffset(currentsampleoffset);
				syncingsampleCount = 0;
				}
			syncingsampleCount++;
		}
		sampleCount++;
		return true;
	}

	// Hunt for an XPB start tone
	private String startToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		String line;
		final int StartTone=1940;
		// Get the last tone that isn't noise
	    int tone1=doXPBFFT(circBuf,waveData,0);
		if (getPercentageOfTotal()<20.0) return null;
		// if (!getChar(tone1).equals("UNID")) System.out.println(getChar(tone1) + ": " + tone1);
		if (!getChar(tone1).equals("8")) return null;
		eightcounter++;
		// TODO: User controllable 8counter (maybe in later version...)
		if (eightcounter == 64){
			syncingsampleCount = 0;
			setState(2);
	    	// Tones found
	    	// Calculate the correction factor, which is not used due to FFT100 being not detailed enough anyway
	    	correctionFactor=StartTone-tone1;
	    	// Tell the user
			line=theApp.getTimeStamp()+" XPB Start 8 Found (corr. factor (not used): "+Integer.toString(correctionFactor)+" Hz)";
			eightcounter = 0;
			return line;
		}
		return null;
	}

	private int getNextSampleOffset(int lastsampleoffset){
		// Calculates the deviation from the "perfect" sample match so that we don't drift off
		switch(lastsampleoffset){
			case 122 -> deviation = deviation + 0.41;
			case 121 -> deviation = deviation - 0.59;
			case 123 -> deviation = deviation + 1.41;
		}
		if(deviation<=-1){
			return 123;
		}
		else {
			if (lastsampleoffset==121) return 122;
			return 121;
		}
	}
	// Return a String for a tone
	private String getChar (int tone)	{
		final int errorAllowance=86;
		if ((tone>(540-errorAllowance))&&(tone<(540+errorAllowance))) return ("0");
		else if ((tone>=(715-errorAllowance))&&(tone<(715+errorAllowance))) return ("1");
		else if ((tone>=(890-errorAllowance))&&(tone<(890+errorAllowance))) return ("2");
		else if ((tone>=(1065-errorAllowance))&&(tone<(1065+errorAllowance))) return ("3");
		else if ((tone>=(1240-errorAllowance))&&(tone<(1240+errorAllowance))) return ("4");
		else if ((tone>=(1415-errorAllowance))&&(tone<(1415+errorAllowance))) return ("5");
		else if ((tone>=(1590-errorAllowance))&&(tone<(1590+errorAllowance))) return ("6");
		else if ((tone>=(1765-errorAllowance))&&(tone<(1765+errorAllowance))) return ("7");
		else if ((tone>=(1940-errorAllowance))&&(tone<(1940+errorAllowance))) return ("8");
		else if ((tone>=(2115-errorAllowance))&&(tone<(2115+errorAllowance))) return ("9");
		else if ((tone>=(2290-errorAllowance))&&(tone<(2290+errorAllowance))) return ("A");
		else if ((tone>=(2465-errorAllowance))&&(tone<(2465+errorAllowance))) return ("B");
		else if ((tone>=(2640-errorAllowance))&&(tone<(2640+errorAllowance))) return ("C");
		else if ((tone>=(2815-errorAllowance))&&(tone<(2815+errorAllowance))) return ("D");
		else if ((tone>=(2990-errorAllowance))&&(tone<(2990+errorAllowance))) return ("E");
		else if ((tone>=(3165-errorAllowance))&&(tone<(3165+errorAllowance))) return ("F");
		else return ("UNID");
	}

	private void displayMessage (int freq,boolean isFile) {
		String tChar = getChar(freq);
		if (tChar.equals("UNID")){
			tChar = "UNID " + freq;
			theApp.writeLine(tChar, Color.BLACK, theApp.boldFont);
			theApp.newLineWrite();
			return;
		}
		theApp.writeChar(tChar, Color.BLACK, theApp.boldFont);
		if (theApp.getcurrentLine().length() > 75){
			theApp.newLineWrite();
			}
		}
	}

