package org.liverpool.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.utils.ReadConfigurationFiles;


public class ConstructParamFileForMzIdentMLParser {

	static Logger log = Logger.getLogger(ReadConfigurationFiles.class);
	
	File searchEngineInputFile;
	String seDelimiter;
	File parserConfigurationInputFile;
	String paramDelimiter;
	
	HashMap <String, String> searchInputContent;
	HashMap <String, String> paramInputContent;	
						
	/**
	 * 
	 * @param searchEngineInput 			- The input file having the specifications for search engine
	 * @param searchFileDelimiter
	 * @param parserConfigurationInput		- The input file with parser related settings
	 * @param paramFiledelimiter
	 * @throws FileNotFoundException
	 */
	public ConstructParamFileForMzIdentMLParser(String searchEngineInput,String searchFileDelimiter, String parserConfigurationInput, String paramFiledelimiter) {
		try{
			searchEngineInputFile = new File(searchEngineInput);
			parserConfigurationInputFile = new File(parserConfigurationInput);
			seDelimiter = searchFileDelimiter;
			paramDelimiter = paramFiledelimiter;
			
			ReadConfigurationFiles rc = new ReadConfigurationFiles();
			searchInputContent = rc.readInputCsvFile(searchEngineInputFile, seDelimiter);
			paramInputContent = rc.readInputCsvFile(parserConfigurationInputFile, paramDelimiter);
		}catch(Exception e){
			log.fatal("Problem creating ConstructParamFileForMzIdentMLParser Object");
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * The test method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);

		
	}

}
