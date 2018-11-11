/**
 * 
 */
package com.ibm.la.insightpack.ABCClientLogAnnotator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import com.ibm.json.java.JSONObject;
import com.ibm.tivoli.unity.splitterannotator.annotator.IJavaAnnotator;

/**
 * @author ahmedasaleh
 *
 */
public class ABCClientLogAnnotator implements IJavaAnnotator {
	// Define the fields to be extracted
	private static final String DATE = "Date";
	private static final String TIME = "Time";
	private static final String SEVERITY = "Severity";
	private static final String PRODUCER = "Producer";
	private static final String METHOD = "JavaMethod";
	private static final String CLASS = "JavaClass";
	private static final String lINENUMBER = "LineNumber";
	private static final String MESSAGE = "Message";

	/* (non-Javadoc)
	 * @see com.ibm.tivoli.unity.splitterannotator.annotator.IJavaAnnotator#annotate(com.ibm.json.java.JSONObject)
	 */
	public JSONObject annotate(JSONObject input) throws Exception {
		JSONObject Wrapperannotations = new JSONObject();

		// read input text
		JSONObject content = (JSONObject) input.get("content");
		String text = (String) content.get("text");
		// start processing
		JSONObject annotations = new JSONObject();
		if (text == null || isEmpty(text)){
			return annotations;		
		}
		if (!text.endsWith("\n")){
			text = text + "\n";			
		}
		StringTokenizer lineSplitted = new StringTokenizer(text, "|");
		if(lineSplitted.countTokens() == 5){
		//0
		String DateTime = lineSplitted.nextToken().trim();
		StringTokenizer dateTimeTokenizer = new StringTokenizer(DateTime, " ");
		annotations.put(DATE, dateTimeTokenizer.nextToken());
		annotations.put(TIME, dateTimeTokenizer.nextToken());
		//1
		annotations.put(PRODUCER, lineSplitted.nextToken().trim());
		//2
		String Severity = lineSplitted.nextToken().trim();
		if(Severity.isEmpty()){
			annotations.put(SEVERITY, "UNKNOWN");
		}
		else{
			annotations.put(SEVERITY,Severity);
		}
		//3
		String Message1 = lineSplitted.nextToken().trim();
		StringTokenizer Message1Splitted = new StringTokenizer(Message1,"\\(");
		annotations.put(METHOD,Message1Splitted.nextToken());
		String temp = Message1Splitted.nextToken();
		String ClassAndNumber = temp.replaceFirst("\\)", "");
		StringTokenizer ClassAndNumberSplitted = new StringTokenizer(ClassAndNumber,":");
		annotations.put(CLASS,ClassAndNumberSplitted.nextToken());
		annotations.put(lINENUMBER,ClassAndNumberSplitted.nextToken());
		//4
		annotations.put(MESSAGE, lineSplitted.nextToken().trim());
		}
		else{
			annotations.put(DATE, new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
			annotations.put(TIME, new SimpleDateFormat("HH:mm:ss,SSS").format(new Date()));
			annotations.put(PRODUCER, "UNKNOWN-PRODUCER");
			annotations.put(SEVERITY, "ERROR");
			annotations.put(METHOD,"UNKOWN-METHOD");
			annotations.put(CLASS,"UNKOWN-CLASS.java");
			annotations.put(lINENUMBER,"-1");
			annotations.put(MESSAGE, "Synthetic Log by IBM Log Analysis-found unformatted log line:"+text);
			
		}

		Wrapperannotations.put("annotations", annotations);
		Wrapperannotations.put("content", content);
		return Wrapperannotations;
	}
	/**
	 * @param text
	 * @return
	 */
	private boolean isEmpty(String text) {
		return text == null || text.isEmpty();
	}
	
}
