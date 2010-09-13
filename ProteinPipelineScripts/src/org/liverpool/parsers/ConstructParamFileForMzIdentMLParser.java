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
import org.liverpool.utils.ReadConfigurationFiles;
import org.liverpool.utils.ReadUmodTable;
import org.liverpool.pipeline.Constants;

public class ConstructParamFileForMzIdentMLParser {

	static Logger log = Logger.getLogger(ReadConfigurationFiles.class);
	
	File searchEngineInputFile;
	String seDelimiter;
	File parserConfigurationInputFile;
	String parserDelimiter;
	File paramKeywordFile;
	String umodFile;
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
	public ConstructParamFileForMzIdentMLParser(String searchEngineInput,String searchFileDelimiter, 
												String parserConfigurationInput, String parserFileDelimiter,
												String umodFile, String umodFileDelimiter,
												String omssaIdentifierInHeaderInUmodFile,
												String enzymeFile,
												String enzymeFileDelimiter,
												String paramKeywordFile) {
		try{
			searchEngineInputFile = new File(searchEngineInput);
			parserConfigurationInputFile = new File(parserConfigurationInput);
			seDelimiter = searchFileDelimiter;
			parserDelimiter = parserFileDelimiter;
			this.umodFile =  umodFile;
			this.umodFileDelimiter = umodFileDelimiter;
			this.omssaIdentifierInHeaderInUmodFile = omssaIdentifierInHeaderInUmodFile;
			this.enzymeFile = new File(enzymeFile);
			this.enzymeFileDelimiter = enzymeFileDelimiter;
			
			this.paramKeywordFile = new File(paramKeywordFile);
			
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
	 * In Omssa, the modification are - fixed_mod_id= -mf 3,110 + variable_mod_id= -mv 1 etc. The list needs to be
	 * resolved using the umod specifications. Scan the input hash keys -look out for "mod" string in the keys and 
	 * parse them.
	 * 
	 * @return A 2-d array - the first index contains Omssa Ids, and the second index 0 (for variable mode ) or 1 (for fixed mod)
	 */
	int [][] resolveModificationsFromSearchInputToUmodId(){
		
		ArrayList<Integer> omssaIdsToFind = new ArrayList<Integer>();
		ArrayList<Integer> fixedModOrNot =  new ArrayList<Integer>();
		
		try{
						
			Iterator<String> keys = searchInputContent.keySet().iterator();
			while(keys.hasNext()){
				String key = keys.next(); 
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_MOD_IN_SEARCHINPUT)){
					String userInputForMod = searchInputContent.get(key).trim();
					userInputForMod = userInputForMod.replaceAll("[^a-zA-Z0-9]", "##"); // replace all the non-alphanumeric characters by ##
					String [] tokens = userInputForMod.split("##");
					for(int i = 0 ; i < tokens.length ;i++){
						int numericVal = -1;
						try{
							numericVal = Integer.parseInt(tokens[i]); // if a non-numeric string is found, it will throw the exception
						}catch(NumberFormatException ne){
							continue;
						}
						omssaIdsToFind.add(numericVal);          
						if(key.contains(Constants.SUBSTRING_FOR_FIXED_MOD_IN_SEARCHINPUT))
							fixedModOrNot.add(1);
						else fixedModOrNot.add(0);
						
						log.info("modification number extracted from search engine input :: "+  numericVal + 
									" :: Fixed = " + key.contains(Constants.SUBSTRING_FOR_FIXED_MOD_IN_SEARCHINPUT));
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		int[][] omssaModIdAndFixedOrVariableInformation = new int[omssaIdsToFind.size()][2];
		
		for(int i = 0 ; i < omssaIdsToFind.size(); i++){
			omssaModIdAndFixedOrVariableInformation[i][0] = omssaIdsToFind.get(i); 
			omssaModIdAndFixedOrVariableInformation[i][1] =	fixedModOrNot.get(i);
		}
		
		return omssaModIdAndFixedOrVariableInformation.clone();
	}
	
	/**
	 * 
	 * @param omssaIdAndModInfo - A 2-d Array, the [0] indices have omssa Ids, and [1] indices have 1(for fixed mod)
	 * 							  or 0 (for variable mod) 
	 * @return - The modification string in the format required for the param file for mzIdentML parser
	 */
	String resolveOmssaModificationNumbers(int [][] omssaIdAndModInfo){
		
		String modificationForParmaFile = new String();
		
		ArrayList<Integer> omssaIdsToFind =new ArrayList<Integer>();
		for(int i = 0 ; i < omssaIdAndModInfo.length; i++){
			omssaIdsToFind.add(omssaIdAndModInfo[i][0]);
		}
		
		ReadUmodTable rut = new ReadUmodTable(this.umodFile,this.umodFileDelimiter);
		HashMap<Integer,String> info = rut.getInformationForGivenOmssaIdentifiers(this.omssaIdentifierInHeaderInUmodFile,omssaIdsToFind);
		
		for(int i = 0; i < omssaIdAndModInfo.length; i++){
			String umodInfo = info.get(omssaIdAndModInfo[i][0]);
			String fixed;
			if(omssaIdAndModInfo[i][1] == 1)
				fixed = "TRUE";
			else fixed = "FALSE";
			
			String [] umodFields = umodInfo.split(this.umodFileDelimiter);
			
			String umodTitle = umodFields[0];
			String umodName =  umodFields[1];
			String umodRecord = umodFields[2];
			String umodSite = umodFields[3];
			String fixedOrVar = fixed;
			String massDelta =  umodFields[4];
			
			String umodToReport = umodTitle + "," + umodName + "," + umodRecord + "," + umodSite + "," 
								  + fixedOrVar + "," + massDelta;
			
			modificationForParmaFile = modificationForParmaFile + umodToReport + "\n";  
		}
		
		return modificationForParmaFile;
	}
	
	/**
	 * The input file will have content in form of -te 1.5, -mf 3,110 etc. We 
	 * need to remove the string -te, -mf etc from the search engine input 
	 * content. 
	 */
	void cleanTheSearchEngineInputFromExtraFlags(){
		Iterator<String> keys = this.searchInputContent.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			String [] value = this.searchInputContent.get(key).split("\\s"); // split the value for space
			if(value.length > 1)
				this.searchInputContent.put(key, value[1]);
		}
	}
	
	/**
	 * Fill the Hash with values needed to create the Param File.
	 * @return
	 */
	public HashMap<String,String> fillKeywordParametersForParamFile(){
		try{	
			
			cleanTheSearchEngineInputFromExtraFlags(); 							// clean the search engine input format
			int [][] modArray = resolveModificationsFromSearchInputToUmodId(); 	// Resolve the modification string
			String modificationString = resolveOmssaModificationNumbers(modArray);
			
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

		String searchEngineInput 					= "inputFiles/omssa_inputFile.txt";
		String searchFileDelimiter 					= "="; 
		String parserConfigurationInput				= "inputFiles/mzIdentMLParser_inputFile.txt";
		String parserFileDelimiter					= "=";
		String umodFile								= "resources/UMOD_TABLE.csv"; 
		String umodFileDelimiter					= ",";	
		String omssaIdentifierInHeaderInUmodFile	= "Omssa_ID";	
		String paramKeywordFile 					= "resources/paramKeywords.txt";
		String enzymeFile							= "resources/enzymeList.csv"; 
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
