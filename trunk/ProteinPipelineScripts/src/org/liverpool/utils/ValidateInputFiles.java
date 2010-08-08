package org.liverpool.utils;

import java.util.Set;

import org.apache.log4j.Logger;

public class ValidateInputFiles {

	static Logger log = Logger.getLogger(ValidateInputFiles.class);
	
	/**
	 * Validates whether the keywords described in the input file are allowed keywords or not.
	 * 
	 * @param allowedKeywords - The keywords read from the definition file
	 * @param inputKeys - The keywords read from the input file
	 * @return true, if all the elements of inptKeys are found in allowedKeywords, otherwise false.
	 */

	public boolean validateContentOfInputFileAgainstAllowedKeywords(Set <String> allowedKeywords, Set <String> inputKeys){
	    
		boolean matchFound = false;
		try {
			matchFound = allowedKeywords.containsAll(inputKeys);
		}catch (Exception ex) {
			log.error("Exception occurred in set matching :: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		log.error("Validation finished");
		return matchFound;
	}
	
	/**
	 * The test method
	 * @param args
	 */
	public static void main(String[] args) {
		
	}

}
