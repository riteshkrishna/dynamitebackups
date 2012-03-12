package org.liverpool.utils;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class InputCleaning {

	static Logger log = Logger.getLogger(InputCleaning.class);
	
	/**
	 * The input file will have content in form of -te 1.5, -mf 3,110 etc. We 
	 * need to remove the string -te, -mf etc from the search engine input 
	 * content. 
	 */
	public HashMap <String, String> cleanTheSearchEngineInputFromExtraFlags(HashMap <String, String> inputHash){
		
		HashMap <String, String> cleanedHash = new HashMap <String, String>();
		Iterator<String> keys = inputHash.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			String [] value = inputHash.get(key).split("\\s"); // split the value for space
			if(value.length > 1)
				inputHash.put(key, value[1]);
		}
		cleanedHash.putAll(inputHash);
		return cleanedHash;
	}


}
