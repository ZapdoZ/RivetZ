# Read this before attempting XPB decodes
(2021-01-12; RivetZ Build 91.1; - ZapdoZ)  
XPB Decoding is still in Beta stage. Decodes come at no warranty.  
Questions? Feedback? Concerns? Send an e-mail to zapdoz9@gmx.de or submit an issue / pull request
## Use recordings
Since your chances on getting the correct starting offset
"first try" might be low, always record a transmission, and then
play back the sound file, so you have the option to retry. **As of Build 91.1, WAV files are not supported. Use the soundcard input and just play back
the recording.**  
Also, if you use a remote SDR such as WebSDR or KiwiSDR, there might be
lag in the audio stream. This could cause the decoder to get "off track" and
decode wrong symbols. Always record transmissions, and use the
recordings to decode them!



## Be sure to configure your decoder!
(Options->XPB Decoder Options)  
### 1. The starting offset  
When the decoder is ready for a message start, it performs an FFT of the last 100
samples of the soundcard input every sample. As soon as it hits the defined starting symbol (see 2.),
it starts counting how often that symbol has been found => For how many samples it
has been "visible" to the FFT.  
The starting offset defines at how many samples of the symbol being identified
the decoder should jump to the actual decoding routine.  
**Minimum**: 1  
**Maximum**: 225-ish  
The default is 64. When the start of your decoding does not seem plausible (see _How XPB starts_), play
around with the value until you find it working.  
### 2. The starting symbol  
XPB messages always start with the Symbol "8", however there is an option
for the decoder to accept another value as the symbol to search for.  
This setting should not be changed unless you know what you're trying to decode
isn't a regular transmission.

**After setting new options, ALWAYS reset the decoding state (Main-> Reset Decoding State),
otherwise changes will not be applied to the decoder!**

## How XPB starts
This is what the first 40 characters of an XPB transmission always look like:   
```8003000A003C008803300AA03FC08083030A0A3C```  
If you get something different, please try to change your starting offset.

## Turn off the decoder when the transmission ends!
As of now, we don't know if XPB messages end in a specific way. That's why the decoder
can't figure out when a messsage is actually over. **As soon as the message is over and UNIDs
start appearing, disable the soundcard input or freeze the screen.**
