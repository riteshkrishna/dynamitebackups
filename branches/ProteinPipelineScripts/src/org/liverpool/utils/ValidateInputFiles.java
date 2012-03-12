package org.liverpool.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

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
		log.info("Validation finished");
		
		if(matchFound)
			log.info("Valid input");
		else log.error("Invalid input");
		
		return matchFound;
	}
	
	/**
	 * The test method
	 * @param args
	 */
	public static void main(String[] args) {
		
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		ValidateInputFiles vif = new ValidateInputFiles();
		
		ReadConfigurationFiles rc = new ReadConfigurationFiles();
		File keywordFile = new File("resources/omssaKeywords.txt");		
		File inputCsvFile = new File("inputFiles/omssa_inputFile.txt");
		String delimiter = ",";

		try{
			Set <String> keywords = rc.readKeywordDefinitionFile(keywordFile);
			HashMap <String, String> ip = rc.readInputCsvFile(inputCsvFile,delimiter);
			Set <String> inputKeys = ip.keySet();
			
			log.info("Input Keys :: " + inputKeys.toArray(new String[0]).length);
			log.info("Allowed keywords :: " + keywords.toArray(new String[0]).length);
			
			vif.validateContentOfInputFileAgainstAllowedKeywords(keywords, inputKeys);
			
		}catch(Exception ex){
			ex.printStackTrace();
		}

		
	}

}
