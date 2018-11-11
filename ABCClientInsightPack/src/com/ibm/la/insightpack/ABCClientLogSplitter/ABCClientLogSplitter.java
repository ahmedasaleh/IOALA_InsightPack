package com.ibm.la.insightpack.ABCClientLogSplitter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.tivoli.unity.common.logging.LoggerConstants;
import com.ibm.tivoli.unity.common.logging.UnityLogger;
import com.ibm.tivoli.unity.splitterannotator.splitter.IJavaSplitter;

/**
 * @author ahmedasaleh
 *
 */
public class ABCClientLogSplitter implements IJavaSplitter {
	private static UnityLogger logger = (UnityLogger) UnityLogger.getLogger(LoggerConstants.GENERIC_RECEIVER_LOG_APPENDER);
	private static ConcurrentHashMap<String, SimpleDateFormat> dateFormats = new ConcurrentHashMap<String, SimpleDateFormat>();
	String DateTimeRegex ="[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1]) (2[0-3]|[01][0-9]):[0-5][0-9]:[0-5][0-9],[0-9]{3}";
	Pattern DateTimePattern = Pattern.compile(DateTimeRegex);
	String LineStartRegex = "\\[[0-9]{2}m";
	Pattern LineStartPattern = Pattern.compile(LineStartRegex);
	boolean foundLineStart = false;
	Matcher LineStartMatcher = null;
	Pattern AsteriskPattern = Pattern.compile("[*]{8,}[-]?");
	Matcher AsteriskMatcher = null;
	Matcher DateTimeMatcher = null;
	JSONObject object  = null;
	JSONObject lineContent = null;
	JSONObject lineMetadata = null;
	private static int ErrorCounter = 0;

	public ArrayList split(JSONObject batch) throws Exception {
		JSONObject content = (JSONObject) batch.get("content");
		JSONArray timestampFormats = (JSONArray) batch.get("timestampFormats");

		String datasource = (String) batch.get("datasource");
		if (datasource == null) {
			throw new Exception("Missing datasource in splitter input");
		}
//		String stream = (String) batch.get("stream");
		String streamIdentifier = datasource;
//		if (stream != null && !stream.trim().isEmpty())
//			streamIdentifier += "_" + stream.trim();

		SimpleDateFormat timestampSdf = dateFormats.get(streamIdentifier);
		if (timestampSdf == null) {
			if (timestampFormats == null || timestampFormats.size() == 0) {
				throw new Exception("Missing timestamp formats in splitter input");
			}

			String timestampFormat = (String) timestampFormats.get(0);
			timestampSdf = new SimpleDateFormat(timestampFormat);
			timestampSdf.setLenient(false);
			dateFormats.put(streamIdentifier, timestampSdf);
		}
		// get the input text
		String text = (String) content.get("text");
		boolean lastLineComplete = text.endsWith("\n");
		// break input text to individual lines
		List<String> lines = this.splitString(text, '\n',false);
		ArrayList<JSONObject> output = new ArrayList<JSONObject>();

		int lineNumber = 0;

		boolean foundDateTimeMatch = false;

		// go through each line
		for (; lineNumber < lines.size(); lineNumber++) {
			// Extract timstamp from log line
			// The first 22 characters contain timestmap.

			String line = null;
			try{
				line = rtrim(lines.get(lineNumber));				
			} catch(Exception e){
				if (logger.isDebugEnabled()) {
					logger.error(getClass(),"ABCClientLogSplitter", e);
				}		
				
			}

			try{
				line = ltrim(line);				
			} catch(Exception e){
				if (logger.isDebugEnabled()) {
					logger.error(getClass(),"ABCClientLogSplitter", e);
				}		
			}

			String timestamp = "";
			try{
				timestamp = this.substring(line, 0, 23);				
				DateTimeMatcher = DateTimePattern.matcher(timestamp);
				foundDateTimeMatch = DateTimeMatcher.find();
			}
			catch(Exception e){
				if (logger.isDebugEnabled()) {
					logger.error(getClass(),"ABCClientLogSplitter", e);
				}		
				foundDateTimeMatch = false;
			}

			if(!foundDateTimeMatch) {
				ErrorCounter++;
				line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date()) + " | UNKNOWN-PRODUCER | ERROR | UNKOWN-METHOD(UNKOWN-CLASS.java:-1) | Synthetic Log by IBM Log Analysis-found unformatted log line: "+line;
				timestamp = this.substring(line, 0, 23);
			}

			try{
				line = replaceUnwantedPatterns(line);				
			} catch(Exception e){
				if (logger.isDebugEnabled()) {
					logger.error(getClass(),"ABCClientLogSplitter", e);
				}		
			}

			// check if the timestamp is in correct format
			try {
				// create a JSON Object for each line to pass to annotator
				object = new JSONObject();
				lineContent = new JSONObject();
				object.put("content", lineContent);
				lineContent.put("text", line);
				lineMetadata = new JSONObject();
				object.put("metadata", lineMetadata);

				// if it's last line and does not contain newline at the end then it's partial line
				// mark it as type B
				if (lastLineComplete == false && lineNumber == (lines.size() - 1)) lineMetadata.put("type", "B");
				else lineMetadata.put("type", "A");
			
				lineMetadata.put("timestamp", timestamp);
				output.add(object);
			} 
			catch	(Exception e){
				if (logger.isDebugEnabled()) {
					logger.error(getClass(),"ABCClientLogSplitter", e);
				}		
			}
		}
		return output;
	}

private List<String> splitString(String str, char separatorChar, boolean preserveAllTokens)
  {
    if (str == null) {
      return null;
    }
    int len = str.length();
    List<String> list = new ArrayList<String>();
    if (len == 0) {
      return list;
    }
    int i = 0;int start = 0;
    boolean match = false;
    boolean lastMatch = false;
    while (i < len) {
      if (str.charAt(i) == separatorChar)
      {
        if ((match) || (preserveAllTokens))
        {
          list.add(str.substring(start, i));
          match = false;
          lastMatch = true;
        }
        i++;start = i;
      }
      else
      {
        lastMatch = false;
        match = true;
        i++;
      }
    }
    if ((match) || ((preserveAllTokens) && (lastMatch))) {
      list.add(str.substring(start, i));
    }
    return list;
  }	

/**
 * @param line
 * @return
 */
private String rtrim(String line) {
	if (!StringUtils.endsWith(line, "\r"))
		return line;

	if (line.isEmpty())
		return line;
	int lastPosition = line.length() - 1;

	while (line.charAt(lastPosition) == '\r' && --lastPosition >= 0){
	}
		
	if (lastPosition < 0)
		return "";
	return this.substring(line,0, lastPosition + 1);
}

/**
 * @param value
 * @param beginIndex
 * @param endIndex
 * @return
 */
private String substring(String value,int beginIndex, int endIndex) {
    String theSubstring = "";
    if (beginIndex < 0) {
        throw new StringIndexOutOfBoundsException(beginIndex);
    }
    if (endIndex > value.length()) {
        throw new StringIndexOutOfBoundsException(endIndex);
    }
    int count = endIndex - beginIndex  ;
    if (count < 0) {
        throw new StringIndexOutOfBoundsException(count);
    }
    else if(count == 0){
    	return theSubstring;
    }
    List<String> buffer = new ArrayList<>();
	buffer.add(Character.toString(value.charAt(beginIndex)));
	theSubstring += buffer.get(0).toString();
	count -=1;
	int i=beginIndex+1;
    while((count--)>0){           
    	buffer.add(Character.toString(value.charAt(i)));
    	theSubstring += buffer.get(i-beginIndex).toString();
    	i++;
    }
    
    return theSubstring;
}

/**
 * @param line
 * @return
 */
private String ltrim(String line) {
	if (line.isEmpty())
		return line;
	int lastPosition = line.length() - 1;

	//to save time we need to search in the first 8 characters only
	LineStartMatcher = LineStartPattern.matcher(this.substring(line,0, 8));
	foundLineStart = LineStartMatcher.find();
	if(foundLineStart){
		line = StringUtils.replaceChars(line, "\u001b", "") ;
		if(line.contains("[m[")) line = this.replaceOnce(line,"[m[", "[");//string replace method uses arrays
		line = this.replaceOnce(line,LineStartMatcher.group(0), "");
	}
	else if(line.contains("\u001b[m")){
		ErrorCounter++;
		line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date()) + " | UNKNOWN-PRODUCER | ERROR | UNKOWN-METHOD(UNKOWN-CLASS.java:-1) | Synthetic Log by IBM Log Analysis-found unformatted log line: "+line;
	}
	if (lastPosition < 0)
		return "";

	return line;
}
/**
 * @param text
 * @param searchString
 * @param replacement
 * @return
 */
private String replaceOnce(String text, String searchString, String replacement) {
	return this.replace(text, searchString, replacement, 1);
}
/**
 * @param text
 * @param searchString
 * @param replacement
 * @param max
 * @return
 */
private String replace(String text, String searchString, String replacement, int max) {
    if (text.isEmpty() || searchString.isEmpty() || replacement == null || max == 0) {
        return text;
    }
    int start = 0;
    int end = text.indexOf(searchString, start);
    if (end == -1) {
        return text;
    }
    int replLength = searchString.length();
    int increase = replacement.length() - replLength;
    increase = (increase < 0 ? 0 : increase);
    increase *= (max < 0 ? 16 : (max > 64 ? 64 : max));
    List<String> buffer = new ArrayList<>();
    while (end != -1) {
        buffer.add(this.substring(text,start, end));
        buffer.add(replacement);
        start = end + replLength;
        
        if (--max == 0) {
            break;
        }
        end = text.indexOf(searchString, start);
    }
	buffer.add(this.substring(text,start,text.length()));
    return (buffer.get(0).toString()+buffer.get(1).toString()+buffer.get(2).toString());
}
/**
 * @param line
 * @return
 */
private String replaceUnwantedPatterns(String line){
	AsteriskMatcher = AsteriskPattern.matcher(this.substring(line,0, 54));
	boolean foundMatch = AsteriskMatcher.find();
	if(foundMatch && (AsteriskMatcher.start() < 25)) {
		line = this.replaceOnce(line, AsteriskMatcher.group(0), " | ") ;
	}
	else if(foundMatch && (AsteriskMatcher.start()) > 45 && (AsteriskMatcher.start()) < 50){
		ErrorCounter++;
		line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date()) + " | UNKNOWN-PRODUCER | ERROR | UNKOWN-METHOD(UNKOWN-CLASS.java:-1) | Synthetic Log by IBM Log Analysis-found unformatted log line: "+line;
	}
	return line;
}


}
