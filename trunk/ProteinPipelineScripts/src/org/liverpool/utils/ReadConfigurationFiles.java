package org.liverpool.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ReadConfigurationFiles {
	
	static Logger log = Logger.getLogger(ReadConfigurationFiles.class);
	
	/**
	 * Read the input CSV file where each line has a keyword followed by the value associated. The key and value pair
	 * will be used to form the executable command string. The key-value pairs are returned in a Hash.
	 *  
	 * @param inputFile
	 * @param withinLineDelimiter
	 * @return
	 * @throws FileNotFoundException
	 */
	public HashMap <String, String> readInputCsvFile(File inputFile,String withinLineDelimiter) throws FileNotFoundException{
	
		Scanner scanner = new Scanner(inputFile);
		HashMap <String, String> inputs = new HashMap <String, String> ();
		
		try{
			while(scanner.hasNextLine() ){
				HashMap <String, String> lineContent = processEachLine(scanner.nextLine(),withinLineDelimiter);
				String key = lineContent.keySet().iterator().next();
				String value = lineContent.get(key);
				inputs.put(key, value);
				log.info("Read :: " + key + " = " + value);
			}
		}
		finally{
			scanner.close();
		}
		
		return inputs;
	}
	
	/**
	 * Process each line according to the delimiter mentioned. Return the results in for a Hash.
	 * 
	 * @param line
	 * @param delimiter
	 * @return
	 */
	HashMap <String, String> processEachLine(String line, String delimiter) {
		
		HashMap<String, String> keyValue = new HashMap <String, String> ();
		Scanner scanner = new Scanner(line);
		scanner.useDelimiter(delimiter);
		try{
			if(scanner.hasNext()){
				keyValue.put(scanner.next(), scanner.next());
			}else{
				log.error("Empty or invalid line : Unable to process");
			}
		}
		finally{
			scanner.close();	
		}
		return keyValue;
	}
	
	/**
	 * Read the template of the executable command string and return.
	 * 
	 * @param templateFile
	 * @return
	 */

	public String readTemplateCommandFile(File templateFile) {
		StringBuilder contents = new StringBuilder();
		
		try {
		      BufferedReader input =  new BufferedReader(new FileReader(templateFile));
		      try {
		        String line = null; 
		        while (( line = input.readLine()) != null){
		          contents.append(line);
		          // Just to make sure that we are not missing the newline character which readLine ignores.
		          contents.append(System.getProperty("line.separator")); 
		        }
		      }
		      finally {
		        input.close();
		      }
		    }
		    catch (IOException ex){
		      ex.printStackTrace();
		    }
		    
		    log.info("Finished reading the Template command File");
		    return contents.toString();
	}
	
	/**
	 * Read the keyword definition file and return it's content in a set. Only this set of keywords are allowed
	 * to exist in the input file. The keys received in input file can be matched with the keywords retrieved by
	 * this function to check for the validity of the input file.
	 * 
	 * @param keywordFile
	 */
	public Set <String> readKeywordDefinitionFile(File keywordFile) throws FileNotFoundException{
		
		Scanner scanner = new Scanner(keywordFile);
		HashMap <String, String> keywords = new HashMap <String, String> ();
		
		try{
			while(scanner.hasNextLine() ){
				keywords.put(scanner.nextLine(), " ");
			}
		}
		finally{
			scanner.close();
		}
		
		log.info("Finished reading the definition file");
		
		Set <String> keywordsFound = keywords.keySet();
		return keywordsFound;		
	}
	
	
	/**
	 * The test method
	 * @param args
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
	}

}
