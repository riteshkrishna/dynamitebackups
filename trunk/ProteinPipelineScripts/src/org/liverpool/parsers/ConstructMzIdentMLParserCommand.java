package org.liverpool.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.pipeline.ExecuteCommands;
import org.liverpool.utils.ReadConfigurationFiles;
import org.liverpool.pipeline.Constants;;

public class ConstructMzIdentMLParserCommand {
	
	static Logger log = Logger.getLogger(ConstructMzIdentMLParserCommand.class);
	
	/*
	// The following strings must match in externalSetup.conf file. The values opposite these keywords
	// decide which perl file to call for a given search engine
	static String omssa_keyword = "omssa_parserMzIdentML";
	static String tandem_keyword = "tandem_parserMzIdentML";
	*/
	
	File configFileWithPathsForExternalCalls;
	String omssa_perlFile;
	String tandem_perlFile;
	
	/**
	 * Read the file with information about external Perl scripts to call. 
	 * @param pathOfConfigFile
	 * @param delimiter
	 */
	public void getTheExternalProgramInfo(String pathOfConfigFile,String delimiter) {
		
		try{
			configFileWithPathsForExternalCalls = new File(pathOfConfigFile);
			ReadConfigurationFiles rc = new ReadConfigurationFiles();
			HashMap <String, String> inputs = rc.readInputCsvFile(configFileWithPathsForExternalCalls,delimiter);
			
			if(inputs.containsKey(Constants.OMSSA_KEYWORD)){
				omssa_perlFile = inputs.get(Constants.OMSSA_KEYWORD);
			}else {
				log.fatal("Perl Command for Omssa2mzIdentML not found. Check externalSetup.conf");
				throw new Exception();
			}
			if(inputs.containsKey(Constants.TANDEM_KEYWORD)){
				tandem_perlFile = inputs.get(Constants.TANDEM_KEYWORD);
			}else{
				log.fatal("Perl Command for Tandem2mzIdentML not found. Check externalSetup.conf");
				throw new Exception();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates the perl call string for conversion of csv/dat file to mzIdentML format
	 * 
	 * @param searchEngineKeyword - "omssa_keyword" or "tandem_keyword"
	 * @param paramFile           - The csv param file
	 * @param inputFile           - csv file for Omssa, dat file for X!Tandem
	 * @param outputMzIdentMLFile - The output file
	 * @return                    - The executable command string
	 */
	public String createMzIdentML(String searchEngineKeyword, String paramFile,String inputFile, String outputMzIdentMLFile){
		String command = new String();
		
		if(searchEngineKeyword.equals(Constants.OMSSA_KEYWORD)){
			command = "perl " + omssa_perlFile + " " + inputFile + " " + " " + paramFile + " " + outputMzIdentMLFile;
		}else if(searchEngineKeyword.equals(Constants.TANDEM_KEYWORD)){
			command = "perl " + tandem_perlFile + " " + inputFile + " " + " " + paramFile + " " + outputMzIdentMLFile;
		}else{
			log.fatal("Search Engine Identifier not recognized.");
		}
		
		log.info("Parser command created : " + command);
		return command;
	}
	
	
	
	/**
	 * Note - The allowed search engine keywords are - "omssa_parserMzIdentML" and "tandem_parserMzIdentML". 
	 * The test function
	 * @param args
	 * 
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		String pathOfConfigFile = "resources/externalSetup.conf";
		String delimiter 		= "=";
		
		ConstructMzIdentMLParserCommand cmp = new ConstructMzIdentMLParserCommand();
		cmp.getTheExternalProgramInfo(pathOfConfigFile, delimiter);
		
		// For csv2mzIdentML
		String searchEngineKeyword = "omssa_parserMzIdentML"; 
		//String paramFile = "PerlParsers/examples/toxo/toxo_omssa_params.csv";
		String paramFile = "logs/exampleParam_omssa.csv";
		String inputFile = "PerlParsers/examples/toxo/Toxo_1D_Slice43_omssa.csv";
		String outputMzIdentMLFile = "logs/Test_Toxo_1D_Slice43_omssa_RK.mzid";
		String commandString = cmp.createMzIdentML(searchEngineKeyword, paramFile, inputFile, outputMzIdentMLFile);
		
		ExecuteCommands ec = new ExecuteCommands();
		ec.execute(commandString);
		
		// For Tandem2mzIdentML
		searchEngineKeyword = "tandem_parserMzIdentML"; 
		//paramFile = "PerlParsers/examples/toxo/toxo_tandem_params.csv";
		paramFile = "logs/exampleParam_tandem.csv";
		inputFile = "PerlParsers/examples/toxo/Toxo_1D_Slice43_tandem.dat";
		outputMzIdentMLFile = "logs/Test_Toxo_1D_Slice43_tandem_RK.mzid";
		commandString = cmp.createMzIdentML(searchEngineKeyword, paramFile, inputFile, outputMzIdentMLFile);
		
		ec.execute(commandString);
		
	}

}
