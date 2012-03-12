package org.liverpool.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.pipeline.Constants;

public class ResolveOmssaModificationIdentifiers {

	static Logger log = Logger.getLogger(ResolveOmssaModificationIdentifiers.class);
	
	File searchEngineInputFile;
	String seDelimiter;
	HashMap <String, String> searchInputContent;
	
	File umodFile;
	String umodFileDelimiter;
	String omssaIdentifierInHeaderInUmodFile;

	/**
	 * 
	 * @param searchEngineInputFile
	 * @param searchInputDelimiter
	 * @param umodFile
	 * @param umodFileDelimiter
	 * @param omssaIdentifierInHeaderInUmodFile
	 */
	
	public ResolveOmssaModificationIdentifiers(File searchEngineInputFile,String searchInputDelimiter,
											   File umodFile,String umodFileDelimiter,String omssaIdentifierInHeaderInUmodFile){
		this.searchEngineInputFile = searchEngineInputFile;
		this.seDelimiter = searchInputDelimiter;
		this.umodFile = umodFile;
		this.umodFileDelimiter = umodFileDelimiter;
		this.omssaIdentifierInHeaderInUmodFile = omssaIdentifierInHeaderInUmodFile;
		
		searchInputContent = new HashMap <String, String> ();		
	}
	
	
	/**
	 * In Omssa, the modification are - fixed_mod_id= -mf 3,110 + variable_mod_id= -mv 1 etc. The list needs to be
	 * resolved using the umod specifications. Scan the input hash keys -look out for "mod" string in the keys and 
	 * parse them.
	 * 
	 * @return A 2-d array - the first index contains Omssa Ids, and the second index 0 (for variable mode ) or 1 (for fixed mod)
	 */
	public int [][] omssaModificationsFromSearchInput(){
		
		try{
			ReadConfigurationFiles rc = new ReadConfigurationFiles();
			searchInputContent = rc.readInputCsvFile(this.searchEngineInputFile,this.seDelimiter); 
			log.info("Read the search input content to resolve omssa ids to umod ids");
		}catch(Exception e){
			log.fatal("Problem reading input file while resolving omssa ids to umod ids.");
			e.printStackTrace();
		}
		
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
	 * @param lineDelimiter - Used in the output string where each Mod info is separated by the lineDelimiter 
	 * 
	 * @return - The modification string in the format required for the param file for mzIdentML parser
	 */
	public String resolveOmssaModificationNumbers(int [][] omssaIdAndModInfo, String lineDelimiter){
		
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
			
			modificationForParmaFile = modificationForParmaFile + umodToReport + lineDelimiter;  
		}
		
		return modificationForParmaFile;
	}
	

	
	/**
	 * Get the Complete umod info from umod_table 
	 * @return
	 */
	public HashMap<Integer,String> getuModNamesOfTheModifications(){
		
		int [][] omssaIdAndModInfo = omssaModificationsFromSearchInput();

		ArrayList<Integer> omssaIdsToFind =new ArrayList<Integer>();
		for(int i = 0 ; i < omssaIdAndModInfo.length; i++){
			omssaIdsToFind.add(omssaIdAndModInfo[i][0]);
		}

		ReadUmodTable rut = new ReadUmodTable(this.umodFile,this.umodFileDelimiter);
		HashMap<Integer,String> info = rut.getInformationForGivenOmssaIdentifiers(this.omssaIdentifierInHeaderInUmodFile,omssaIdsToFind);

		return info;
	}

	
	/**
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);

		String searchEngineInput 					= "inputFiles/omssa_inputFile.txt";
		String searchFileDelimiter 					= "="; 
		String umodFile								= "resources/UMOD_TABLE.csv"; 
		String umodFileDelimiter					= ",";	
		String omssaIdentifierInHeaderInUmodFile	= "Omssa_ID";	
		
		ResolveOmssaModificationIdentifiers rom = new ResolveOmssaModificationIdentifiers(new File(searchEngineInput), searchFileDelimiter, 
														new File(umodFile), umodFileDelimiter, omssaIdentifierInHeaderInUmodFile);
		// Use -1 
		HashMap<Integer,String> info = rom.getuModNamesOfTheModifications();
		log.info(info.toString());
		
		// Use - 2
		String lineDelimiter = "\n";
		int[][] ids = rom.omssaModificationsFromSearchInput();
		String formattedInfo = rom.resolveOmssaModificationNumbers(ids,lineDelimiter);
		log.info(formattedInfo);
		
	}

}
