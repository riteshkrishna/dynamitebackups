package org.liverpool.pipeline;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.utils.ReadConfigurationFiles;
import org.liverpool.utils.ValidateInputFiles;

public class ConstructSearchCommand {

	static Logger log = Logger.getLogger(ConstructSearchCommand.class);
	
	ReadConfigurationFiles rc;
	ValidateInputFiles vif;
	
	File inputFile;
	File templateFile;
	File allowedKeywordFile;
	String inputFileDelimiter;
	
	ConstructSearchCommand(File input,File template,File allowedKeyword,String inputDelimiter)
	{
		rc = new ReadConfigurationFiles();
		vif = new ValidateInputFiles();
		 
		inputFile = input;
		templateFile = template;
		allowedKeywordFile = allowedKeyword;
		inputFileDelimiter = inputDelimiter;
	}
	
	
	HashMap <String, String> readInputFile(){
		HashMap <String, String> inputs = new HashMap <String, String> ();
		try{
			inputs = rc.readInputCsvFile(inputFile, inputFileDelimiter);
		}catch(Exception ex){
			log.fatal("Problem with Input File");
			ex.printStackTrace();
		}
		return inputs;
	}
	
	
	String readTemplateCommandFile(){
		String command = new String();
		try{
			command = rc.readTemplateCommandFile(templateFile);
		}catch(Exception ex){
			log.fatal("Problem with Template File");
			ex.printStackTrace();
		}
		return command;
	}
	
	
	Set <String> readAllowedKeywordFile() throws Exception{
		Set <String> keywords = rc.readKeywordDefinitionFile(allowedKeywordFile);
		return keywords;
	}
	
	/**
	 * In the template file, the text to be replaces is indicated like - {{ text }}. So, we need to search for
	 * these texts surrounded by double curly brackets and replace them by appropriate values read from the input file.
	 *  
	 * @param inputHash
	 * @param templateCommand
	 * @return
	 */
	String fillTheCommandTemplate(HashMap <String, String> inputHash, String templateCommand){
		String command = new String(templateCommand);
		Iterator <String> inputkeys = inputHash.keySet().iterator();
		while(inputkeys.hasNext()){
			String key = inputkeys.next();
			String textToReplace = "{{ " + key.trim() + " }}";
			command = command.replace(textToReplace,inputHash.get(key));
		}
		return command;
	}
	
	/**
	 * 
	 * @return
	 */
	public String accpetInputAndCreateCommand() {

		String command = new String();
		try{
			HashMap <String, String> inputHash = readInputFile();
			Set <String> allowedKeywords = readAllowedKeywordFile();
			String templateCommand = readTemplateCommandFile();
			
			if(vif.validateContentOfInputFileAgainstAllowedKeywords(allowedKeywords, inputHash.keySet())){
				command = fillTheCommandTemplate(inputHash,templateCommand);
				log.info("Created Command :: "+ command);
			}
			else{
				log.error("Invalid input keywords found in the input file");
			}
			
		}catch(Exception ex){
			log.fatal("Failed to create command.");
			ex.printStackTrace();
		}
		return command;
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		File input = new File("inputFiles/omssa_inputFile.txt");
		File template = new File("templates/omssa_template.txt");
		File allowedKeyword = new File("resources/omssaKeywords.txt");
		String inputDelimiter = ",";
		
		ConstructSearchCommand cs = new ConstructSearchCommand(input,template,allowedKeyword,inputDelimiter);
		String command = cs.accpetInputAndCreateCommand();
		
	}

}
