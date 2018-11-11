/**
 * 
 */
package com.ibm.la.insightpack.ABCClientApp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.la.insightpack.ABCClientLogAnnotator.ABCClientLogAnnotator;
import com.ibm.la.insightpack.ABCClientLogSplitter.ABCClientLogSplitter;

/**
 * @author ahmedasaleh
 *
 */
public class Application {

	/**
	 * @param args
	 */
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Path p1 = Paths.get("logSamples/sample_test1.log");
//		Path p1 = Paths.get("logSamples/ABCClientLogs.log");
		ABCClientLogSplitter splitter = new ABCClientLogSplitter();
		ABCClientLogAnnotator annotator = new ABCClientLogAnnotator();
		ArrayList<JSONObject> splitLogRecords = null;
		
		String text = null;
		try {
			text = readFile(p1.toString(),StandardCharsets.UTF_8);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			splitLogRecords = splitter.split(prepareJSONObject(text));
			//Annotator part
			int recordNumber = 0;
			for (; recordNumber < splitLogRecords.size(); recordNumber++) {
				JSONObject output = annotator.annotate((JSONObject) splitLogRecords.get(recordNumber));
			}		
		} catch (Exception e) {
			e.printStackTrace();			
		}
		
	}
	
	/**
	 * @param text : a text contains the whole log file
	 * @return JSONObject : with the format 
	 * 						{
							   "content":{"text":"LOG_TEXT","metadata":{"timestamp":"TIME_INSTANCE"}},
							   "timestampFormats":["yyyy-MM-dd HH:mm:ss,SSS"],
							   "datasource":"ABCClientAppLog"
							}
	 */
	static JSONObject prepareJSONObject(String text){
		JSONObject content = new JSONObject();
		JSONArray timestampFormats = new JSONArray();
		JSONObject object = new JSONObject();
		JSONObject metadata = new JSONObject();

		content.put("text", text);//contains single name "text" with the whole log as a value, {"text":"LOG_TEXT"}
		timestampFormats.add("yyyy-MM-dd HH:mm:ss,SSS");//use log file time stamp
		object.put("timestampFormats", timestampFormats);//provides time stamp format for unity server, {"timestampFormats":["yyyy-MM-dd HH:mm:ss,SSS"]}
		metadata.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date()));//provides current real time stamp for unity server, {"timestamp":"2018-11-01 02:03:38,332"}
		content.put("metadata", metadata);//{"text":"LOG_TEXT","metadata":{"timestamp":"2018-11-01 02:03:38,332"}}
		object.put("content", content);//{"content":{"text":"LOG_TEXT","metadata":{"timestamp":"2018-11-01 02:03:38,332"}},"timestampFormats":["yyyy-MM-dd HH:mm:ss,SSS"]}
		object.put("datasource", "ABCClientAppLog");////{"content":{"text":"LOG_TEXT","metadata":{"timestamp":"2018-11-01 02:03:38,332"}},"timestampFormats":["yyyy-MM-dd HH:mm:ss,SSS"],"datasource":"ABCClientAppLog"}
		return object;
	}
	/**
	 * @param path
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	static String readFile(String path, Charset encoding) throws IOException 
	{
	  byte[] encoded = Files.readAllBytes(Paths.get(path));
	  return new String(encoded, encoding);
	}
	

}
