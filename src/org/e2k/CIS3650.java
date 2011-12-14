package org.e2k;

import javax.swing.JOptionPane;

public class CIS3650 extends FSK {

	private int state=0;
	private double samplesPerSymbol50;
	private double samplesPerSymbol36;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	public StringBuffer lineBuffer=new StringBuffer();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int highTone;
	private int lowTone;
	private int centre;
	private long syncFoundPoint;
	private int syncState;
	private String line="";
	private int buffer32=0;
	private int buffer16=0;
	int characterCount;
	int startCount;
	
	public CIS3650 (Rivet tapp)	{
		theApp=tapp;
	}
	
	// The main decode routine
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
		
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()>11025.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nCIS 36-50 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			samplesPerSymbol36=samplesPerSymbol(36.0,waveData.getSampleRate());
			samplesPerSymbol50=samplesPerSymbol(50.0,waveData.getSampleRate());
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			state=1;
			line="";
			lineBuffer.delete(0,lineBuffer.length());
			syncState=0;
			buffer32=0;
			buffer16=8;
			characterCount=0;
			return null;
		}
		
		
		// Look for a 36 baud or a 50 baud alternating sequence
		if (state==1)	{
			sampleCount++;
			if (sampleCount<0) return null;
			
			if ((syncState==0)&&(detect36Sync(circBuf,waveData)==true))	{
				outLines[0]=theApp.getTimeStamp()+" CIS 36-50 36 baud sync sequence found";
				syncState=1;
				return outLines;
			}
			
			if (detect50Sync(circBuf,waveData)==true)	{
				outLines[0]=theApp.getTimeStamp()+" CIS 36-50 50 baud sync sequence found";
				// Jump the next stage to acquire symbol timing
				state=3;
				syncState=1;
				syncFoundPoint=sampleCount;
				return outLines;
			}
			
			
			
		}
			
		// Acquire symbol timing
		if (state==3)	{
			do64FFT(circBuf,waveData,0);
			energyBuffer.addToCircBuffer(getHighSpectrum());
			sampleCount++;
			symbolCounter++;
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()<(int)(samplesPerSymbol50*1)) return null;
			long perfectPoint=energyBuffer.returnHighestBin()+syncFoundPoint+(int)samplesPerSymbol50;
			// Calculate what the value of the symbol counter should be
			symbolCounter=(int)samplesPerSymbol50-(perfectPoint-sampleCount);
			state=4;
		}
		
		// Read in symbols
		if (state==4)	{
			if (symbolCounter>=(long)samplesPerSymbol50)	{
				symbolCounter=0;		
				boolean bit=getSymbolBit(circBuf,waveData,0);
				if (theApp.isDebug()==false)	{
					if (syncState==1)	{
						addToBuffer16(bit);
						// Look for 101010101011100 (0x555C) which appears to be the start of the message
						if (buffer16==0x555C)	{
							syncState=2;
							outLines[0]="Message Start";
							buffer32=0;
							startCount=0;
						}	
						// Cope with an inverted message
						else if (buffer16==0x2AA3)	{
							syncState=2;
							outLines[0]="Message Start (INV)";
							theApp.setInvertSignal(true);
							buffer32=0;
							startCount=0;
						}	
					}
					// Once we have the start sequence look for the header
					else if (syncState==2)	{
						addToBuffer32(bit);
						startCount++;
						if (startCount==32)	{
							syncState=3;
							outLines[0]="Header "+Integer.toHexString(buffer32);
						}
					}
					// Read in and display the main body of the message
					else if (syncState==3)	{
						addToBuffer16(bit);
						if (buffer16==0x4081)	{
							outLines[0]=lineBuffer.toString();
							lineBuffer.delete(0,lineBuffer.length());
							characterCount=0;
							syncState=4;
						}
						if (bit==true)	lineBuffer.append("1");
						else lineBuffer.append("0");
						if (characterCount==60)	{
							outLines[0]=lineBuffer.toString();
							lineBuffer.delete(0,lineBuffer.length());
							characterCount=0;
						}
						else characterCount++;
					}
					// The message must have ended
					else if (syncState==4)	{
						outLines[0]="End of Message";
						syncState=1;
					}
				}
				else	{
					// Debug mode so just display raw binary
					if (bit==true)	lineBuffer.append("1");
					else lineBuffer.append("0");
					if (characterCount==60)	{
						outLines[0]=lineBuffer.toString();
						lineBuffer.delete(0,lineBuffer.length());
						characterCount=0;
					}
					else characterCount++;
					
				}
				
			}
				
		}
		
		sampleCount++;
		symbolCounter++;
		return outLines;
	}
	
	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}
	

	// Get the frequency at a certain symbol
	private int getSymbolFreq (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int fr=do64FFT(circBuf,waveData,start);
		return fr;
	}
	
	// Return the bit value for a certain symbol
	private boolean getSymbolBit (CircularDataBuffer circBuf,WaveData waveData,int start)	{
		int f=getSymbolFreq(circBuf,waveData,start);
		boolean bit=freqDecision(f,centre,theApp.isInvertSignal());
		return bit;
	}
	
	// Add the bit to a 32 bit long sbuffer
	private void addToBuffer32(boolean bit)	{
		buffer32=buffer32<<1;
		buffer32=buffer32&0xFFFFFFFF;
		if (bit==true) buffer32++;
	}
	
	// Get a 31 bit version of the buffer
	private int getBuffer31 ()	{
		return buffer32&0x7FFFFFFF;
	}
	
	// Add a bit to the 16 bit buffer
	private void addToBuffer16(boolean bit)	{
		buffer16=buffer16<<1;
		buffer16=buffer16&0xFFFF;
		if (bit==true) buffer16++;
	}
	
	private boolean detect36Sync(CircularDataBuffer circBuf,WaveData waveData)	{
		int pos=0;
		int f0=getSymbolFreq(circBuf,waveData,pos);
		// Check this first tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		pos=(int)samplesPerSymbol36*1;
		int f1=getSymbolFreq(circBuf,waveData,pos);
		if (f0==f1) return false;
		pos=(int)samplesPerSymbol36*2;
		int f2=getSymbolFreq(circBuf,waveData,pos);
		pos=(int)samplesPerSymbol36*3;
		int f3=getSymbolFreq(circBuf,waveData,pos);
		// Look for a 36 baud alternating sequence
		if ((f0==f2)&&(f1==f3)&&(f0!=f1)&&(f2!=f3))	{
			if (f0>f1)	{
				highTone=f0;
				lowTone=f1;
			}
			else	{
				highTone=f1;
				lowTone=f0;
			}
			centre=(highTone+lowTone)/2;
			int shift=highTone-lowTone;
			// Check for an incorrect shift
			if ((shift>300)||(shift<150)) return false;
			return true;
		}
		return false;
	}
	
	private boolean detect50Sync(CircularDataBuffer circBuf,WaveData waveData)	{
		int pos=0;
		int f0=getSymbolFreq(circBuf,waveData,pos);
		// Check this first tone isn't just noise the highest bin must make up 10% of the total
		if (getPercentageOfTotal()<10.0) return false;
		pos=(int)samplesPerSymbol50*1;
		int f1=getSymbolFreq(circBuf,waveData,pos);
		if (f0==f1) return false;
		pos=(int)samplesPerSymbol50*2;
		int f2=getSymbolFreq(circBuf,waveData,pos);
		if (f1==f2) return false;
		pos=(int)samplesPerSymbol50*3;
		int f3=getSymbolFreq(circBuf,waveData,pos);
		// Look for a 50 baud alternating sequence
		if ((f0==f2)&&(f1==f3)&&(f0!=f1)&&(f2!=f3))	{
			if (f0>f1)	{
				highTone=f0;
				lowTone=f1;
			}
			else	{
				highTone=f1;
				lowTone=f0;
			}
			centre=(highTone+lowTone)/2;
			int shift=highTone-lowTone;
			// Check for an incorrect shift
			if ((shift>300)||(shift<150)) return false;
			return true;
		}
		return false;
	}
	
	
}
