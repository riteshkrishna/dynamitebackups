package org.liverpool.parsers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.utils.InputCleaning;
import org.liverpool.utils.ReadConfigurationFiles;
import org.liverpool.utils.ReadUmodTable;
import org.liverpool.utils.ResolveOmssaModificationIdentifiers;
import org.liverpool.pipeline.Constants;

public class ConstructParamFileForMzIdentMLParser {

	static Logger log = Logger.getLogger(ConstructParamFileForMzIdentMLParser.class);
	
	File searchEngineInputFile;
	String seDelimiter;
	File parserConfigurationInputFile;
	String parserDelimiter;
	File paramKeywordFile;
	File umodFile;
	String umodFileDelimiter;
	String omssaIdentifierInHeaderInUmodFile;
	File enzymeFile;
	String enzymeFileDelimiter;
	
	HashMap <String, String> searchInputContent;
	HashMap <String, String> parserInputContent;	
	HashMap <String, String> enzymeFileContent;
	Set <String> paramKeywords;					
	
	
	HashMap<String, String> paramKeyValueHash;
	
	/**
	 * 
	 * @param searchEngineInput 			- The input file having the specifications for search engine
	 * @param searchFileDelimiter
	 * @param parserConfigurationInput		- The input file with parser related settings
	 * @param parserFiledelimiter
	 * @param umodFile 
	 * @param umodFileDelimiter
	 * @param enzymeFile
	 * @param enzymeFileDelimiter
	 * @param omssaIdentifierInHeaderInUmodFile
	 * @paramKeywordFile                    - The keyword file for Param file creation.
	 */
	public ConstructParamFileForMzIdentMLParser(File searchEngineInput,String searchFileDelimiter, 
												File parserConfigurationInput, String parserFileDelimiter,
												File umodFile, String umodFileDelimiter,
												String omssaIdentifierInHeaderInUmodFile,
												File enzymeFile,
												String enzymeFileDelimiter,
												File paramKeywordFile) {
		try{
			searchEngineInputFile = searchEngineInput;
			parserConfigurationInputFile = parserConfigurationInput;
			seDelimiter = searchFileDelimiter;
			parserDelimiter = parserFileDelimiter;
			this.umodFile =  umodFile;
			this.umodFileDelimiter = umodFileDelimiter;
			this.omssaIdentifierInHeaderInUmodFile = omssaIdentifierInHeaderInUmodFile;
			this.enzymeFile = enzymeFile;
			this.enzymeFileDelimiter = enzymeFileDelimiter;
			
			this.paramKeywordFile = paramKeywordFile;
			
			ReadConfigurationFiles rc = new ReadConfigurationFiles();
			searchInputContent = rc.readInputCsvFile(searchEngineInputFile, seDelimiter); 
			parserInputContent = rc.readInputCsvFile(parserConfigurationInputFile, parserDelimiter);
			paramKeywords = rc.readKeywordDefinitionFile(this.paramKeywordFile);	
			enzymeFileContent = rc.readInputCsvFile(this.enzymeFile, this.enzymeFileDelimiter);
			
			if(paramKeywords.isEmpty() || searchInputContent.isEmpty() || parserInputContent.isEmpty())
			{	
				log.fatal("The input parameters needed for param file could not be read properly.");
				throw new Exception();
			}
			
			// Todo : We can check if all the keys in paramKeywords are present in searchInputContent and
			// parserInputContent or not. Also, that the KeywordMap class has the same keys/values.
			//............................
			
			// Create the empty storage for the key-value pairs to be filled
			paramKeyValueHash = new HashMap<String, String>();
			
		}catch(Exception e){
			log.fatal("Problem creating ConstructParamFileForMzIdentMLParser Object");
			e.printStackTrace();
		}
	}
	

	
	/**
	 * The input file will have content in form of -te 1.5, -mf 3,110 etc. We 
	 * need to remove the string -te, -mf etc from the search engine input 
	 * content. 
	 */
	/*
	void cleanTheSearchEngineInputFromExtraFlags(){
		Iterator<String> keys = this.searchInputContent.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			String [] value = this.searchInputContent.get(key).split("\\s"); // split the value for space
			if(value.length > 1)
				this.searchInputContent.put(key, value[1]);
		}
	}
	
	*/
	/**
	 * Fill the Hash with values needed to create the Param File.
	 * @return
	 */
	public HashMap<String,String> fillKeywordParametersForParamFile(){
		try{	
			
			InputCleaning ic = new InputCleaning();
			this.searchInputContent = ic.cleanTheSearchEngineInputFromExtraFlags(this.searchInputContent);
			
			//cleanTheSearchEngineInputFromExtraFlags(); 							// clean the search engine input format
			
			ResolveOmssaModificationIdentifiers rom = new ResolveOmssaModificationIdentifiers(this.searchEngineInputFile, this.seDelimiter,
																	this.umodFile,this.umodFileDelimiter,
																	this.omssaIdentifierInHeaderInUmodFile);
			
			int [][]modArray = rom.omssaModificationsFromSearchInput();
			String lineDelimiter = "\n";
			String modificationString = rom.resolveOmssaModificationNumbers(modArray,lineDelimiter);
			
			// Read the names of all required fields for creating a param file
			KeywordMap paramkeyMap = new KeywordMap();
			Iterator<String> keysForParamFile = paramkeyMap.paramInputToSearchEngineInputMap.keySet().iterator();
		
			// Find values for all the keys needed for param file
			while(keysForParamFile.hasNext()){
				String key = keysForParamFile.next().trim();
				String value = paramkeyMap.paramInputToSearchEngineInputMap.get(key).trim();
			
				String resolvedValue;
				if(this.searchInputContent.containsKey(value)){
					if(value.contains(Constants.SUBSTRING_TO_IDENTIFY_ENZYME)){
						resolvedValue = this.enzymeFileContent.get(this.searchInputContent.get(value).trim());
					}else 
						resolvedValue = this.searchInputContent.get(value).trim();
				}else if(this.parserInputContent.containsKey(value)){
					resolvedValue = this.parserInputContent.get(value).trim();
				}else if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_MOD_IN_SEARCHINPUT)){    
					resolvedValue = modificationString; // because the Map in keywordMap has "modifications" key
				}else {
					log.fatal("No resolution found for the value =" + value +" for key =" + key + " in the keywordMap structure");
					throw new Exception();
				}
				
				this.paramKeyValueHash.put(key, resolvedValue);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return this.paramKeyValueHash;	
	}

	/**
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
	 * @param paramFileToCreate
	 * @param templateCommandFile
	 */
	public void createParamFile(File paramFileToCreate,File templateCommandFile){
		
		try{
			FileWriter fstream = new FileWriter(paramFileToCreate);
			BufferedWriter out = new BufferedWriter(fstream);
		
			ReadConfigurationFiles rc = new ReadConfigurationFiles();
			String paramFileTemplate = rc.readTemplateCommandFile(templateCommandFile);
			
			fillKeywordParametersForParamFile();
		
			String filledTemplate = fillTheCommandTemplate(this.paramKeyValueHash, paramFileTemplate);
			out.write(filledTemplate);
			
			out.close();
			fstream.close();
		}catch(IOException e){
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

		File searchEngineInput 						= new File("inputFiles/omssa_inputFile.txt");
		String searchFileDelimiter 					= "="; 
		File parserConfigurationInput				= new File("inputFiles/mzIdentMLParser_inputFile.txt");
		String parserFileDelimiter					= "=";
		File umodFile								= new File("resources/UMOD_TABLE.csv"); 
		String umodFileDelimiter					= ",";	
		String omssaIdentifierInHeaderInUmodFile	= "Omssa_ID";	
		File paramKeywordFile 						= new File("resources/paramKeywords.txt");
		File enzymeFile								= new File("resources/enzymeList.csv"); 
		String enzymeFileDelimiter					= "=";
		
		ConstructParamFileForMzIdentMLParser cp = new ConstructParamFileForMzIdentMLParser(searchEngineInput,searchFileDelimiter, 
																			parserConfigurationInput,parserFileDelimiter,
																			umodFile, umodFileDelimiter,
																			omssaIdentifierInHeaderInUmodFile,
																			enzymeFile,enzymeFileDelimiter,
																			paramKeywordFile);
		HashMap<String,String> resolvedValuesForParam = cp.fillKeywordParametersForParamFile(); 
		
		System.out.println(resolvedValuesForParam.toString());
		
		File paramFileToCreate = new File("logs/exampleParam_omssa.csv");
		File paramTemplateFile = new File("templates/omssa_paramfile_template.txt");
		cp.createParamFile(paramFileToCreate, paramTemplateFile);
		
		paramFileToCreate = new File("logs/exampleParam_tandem.csv");
		paramTemplateFile = new File("templates/tandem_paramfile_template.txt");
		cp.createParamFile(paramFileToCreate, paramTemplateFile);
	}

}
