package org.liverpool.pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.multipleSearch.CallMultipleSearchMethod;
import org.liverpool.parsers.ConstructMzIdentMLParserCommand;
import org.liverpool.parsers.ConstructParamFileForMzIdentMLParser;
import org.liverpool.utils.FileLookup;
import org.liverpool.utils.ReadConfigurationFiles;



public class Pipeline {

	static Logger log = Logger.getLogger(Pipeline.class);
	
	File templateFile_omssa;
	File templateFile_tandem;
	File allowedKeyords;
	File umodFile;
	String umodFileDelimiter;
	String omssaIdentifierInHeaderInUmodFile;
	File enzymeFile;
	String enzymeFileDelimiter;
	
	File templateFile_omssa_param; 
	File templateFile_tandem_param;
	File paramKeywordFile;
	File externalSetupFile;
	String externalSetupFileDelimiter; 		
	
	static String randomNumberIdentifier;

	/**
	 * Set all the path of all required template/configuration files here. These files don't change
	 * during the run.
	 */
	public Pipeline(){
		
		try{
			
			this.templateFile_omssa  					= new File("templates/omssa_template.txt");
			this.templateFile_tandem 					= new File("templates/tandem_template.txt");
			this.templateFile_omssa_param 				= new File("templates/omssa_paramfile_template.txt");
			this.templateFile_tandem_param 				= new File("templates/tandem_paramfile_template.txt");
			this.allowedKeyords      					= new File("resources/omssaKeywords.txt");
			this.paramKeywordFile    					= new File("resources/paramKeywords.txt");
			this.umodFile			 					= new File("resources/UMOD_TABLE.csv");
			this.umodFileDelimiter   					= ",";
			this.omssaIdentifierInHeaderInUmodFile 		= "Omssa_ID";
			this.enzymeFile			 					= new File("resources/enzymeList.csv");
			this.enzymeFileDelimiter 					= "=";
			this.externalSetupFile                      = new File("resources/externalSetup.conf");
			this.externalSetupFileDelimiter				= "=";
			
			int Min = 1,Max = 1000;
			int rand = Min + (int)(Math.random() * ((Max - Min) + 1));
			randomNumberIdentifier = Integer.toString(rand);
			
			
		}catch(Exception e){
			log.fatal("Problem in opening template/configuration files. Check if all the required" +
					" files are present in templates/ and resources/ directory");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		Pipeline pipeline = new Pipeline();
		
		String dirToStoreTempFile 					= "/Users/riteshk/Ritesh_Work/TestSpace/pipeline_test";
		File input 									= new File("inputFiles/omssa_inputFile.txt");
		String inputDelimiter 						= "=";
		File parserConfigurationInput				= new File("inputFiles/mzIdentMLParser_inputFile.txt");
		String parserFileDelimiter					= "=";
		
		/******************************************************************/
		// Construct omssa command - store file
		/******************************************************************/
		// Choose the template
		File template = new File("");
		
		String searchEngineName = Constants.OMSSA_ID;
		if(searchEngineName.equals(Constants.OMSSA_ID))
			 template = pipeline.templateFile_omssa;
		
		ConstructSearchCommand cs = new ConstructSearchCommand(input,template,pipeline.allowedKeyords,
																inputDelimiter,
																pipeline.umodFile, pipeline.umodFileDelimiter, 
																pipeline.omssaIdentifierInHeaderInUmodFile,
																pipeline.enzymeFile, pipeline.enzymeFileDelimiter);

		String command_omssa = cs.accpetInputAndCreateCommand(searchEngineName);
		String outputFileProduced_omssa = cs.getNameOfOutputFile();
		
		/******************************************************************/
		// Construct tandem command - store file
		/******************************************************************/
		searchEngineName = Constants.TANDEM_ID;
		if(searchEngineName.equals(Constants.TANDEM_ID))
			 template = pipeline.templateFile_tandem;
		cs 					= new ConstructSearchCommand(input,template,pipeline.allowedKeyords,
														inputDelimiter,
														pipeline.umodFile, pipeline.umodFileDelimiter, 
														pipeline.omssaIdentifierInHeaderInUmodFile,
														pipeline.enzymeFile, pipeline.enzymeFileDelimiter);

		String command_tandem = cs.accpetInputAndCreateCommand(searchEngineName);
		
		// NOTE - X!Tandem has weired naming scheme for the output file, so this string will only help
		// in identifying the actual file, rather than representing the actual file itself. This is resolved
		// below, after the search engine has finished.
		String outputFileProduced_tandem = cs.getNameOfOutputFile();
		//outputFileProduced_tandem = outputFileProduced_tandem + "_" + randomNumberIdentifier;
		
		/******************************************************************/
		// Execute omssa command
		/******************************************************************/
		String omssa_exec  = null;
		String tandem_exec = null;
		
		try{
			ReadConfigurationFiles rc = new ReadConfigurationFiles();
			HashMap <String, String> inputs = rc.readInputCsvFile(pipeline.externalSetupFile,pipeline.externalSetupFileDelimiter);
			
			if(inputs.containsKey(Constants.OMSSA_EXECUTABLE))
				omssa_exec = inputs.get(Constants.OMSSA_EXECUTABLE);
			if(inputs.containsKey(Constants.TANDEM_EXECUTABLE))
				tandem_exec = inputs.get(Constants.TANDEM_EXECUTABLE);
		}catch(Exception e){
			log.fatal("Unable to find executables for Omsssa and X!Tandem");
			e.printStackTrace();
		}
		
		ExecuteCommands ec = new ExecuteCommands();		
		String runOmssa   = omssa_exec + " " + command_omssa;
		
		log.info("Omssa Command :: " + command_omssa);
		
		ec.execute(runOmssa);
		/******************************************************************/
		// Execute X!Tandem command
		/******************************************************************/
		String inputxml = dirToStoreTempFile + "/input" + "_" + randomNumberIdentifier +".xml";
		try{
			File inputXmlFile = new File(inputxml);
			if(inputXmlFile.exists())
				inputXmlFile.delete();
			inputXmlFile.createNewFile();
			BufferedWriter br = new BufferedWriter(new FileWriter(inputXmlFile));
			br.write(command_tandem);
			br.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
		String runTandem = tandem_exec + " " + inputxml;
		ec.execute(runTandem);
		
		// Retrieve the actual file produced by X!Tandem using the identifier
		FileLookup fl = new FileLookup();
		outputFileProduced_tandem = fl.retrieveTheFileProducedByTandem(dirToStoreTempFile,outputFileProduced_tandem);
		
		log.info("The output file produced by X!Tandem - " + outputFileProduced_tandem);
		/******************************************************************/
		// Construct parser param files
		/******************************************************************/
		// ------ Omssa
		ConstructParamFileForMzIdentMLParser cp = new ConstructParamFileForMzIdentMLParser(
													input, inputDelimiter,									
													parserConfigurationInput,parserFileDelimiter,
													pipeline.umodFile, pipeline.umodFileDelimiter,
													pipeline.omssaIdentifierInHeaderInUmodFile,
													pipeline.enzymeFile,pipeline.enzymeFileDelimiter,
													pipeline.paramKeywordFile);
		
		File paramTemplateFile = pipeline.templateFile_omssa_param; 
		File paramFileToCreate_omssa = new File(dirToStoreTempFile.concat("/exampleParam_omssa" + "_" + randomNumberIdentifier +".csv"));
		cp.createParamFile(paramFileToCreate_omssa, paramTemplateFile);

		// ------ X!Tandem
		paramTemplateFile = pipeline.templateFile_tandem_param; 
		File paramFileToCreate_tandem = new File(dirToStoreTempFile.concat("/exampleParam_tandem" + "_" + randomNumberIdentifier +".csv"));
		cp.createParamFile(paramFileToCreate_tandem, paramTemplateFile);

		/******************************************************************/
		// Execute the parser
		/******************************************************************/

		ConstructMzIdentMLParserCommand cmp = new ConstructMzIdentMLParserCommand(pipeline.externalSetupFile, pipeline.externalSetupFileDelimiter);
		
		String searchEngineKeyword = Constants.OMSSA_KEYWORD;
		String outputMzIdentMLFile_omssa = dirToStoreTempFile + "/Test_Toxo_1D_Slice43_omssa" + "_" + randomNumberIdentifier +".mzid";
		String commandparser = cmp.createMzIdentML(searchEngineKeyword, paramFileToCreate_omssa.getAbsolutePath(), outputFileProduced_omssa, outputMzIdentMLFile_omssa);
		ec = new ExecuteCommands();
		ec.execute(commandparser);
		
		searchEngineKeyword = Constants.TANDEM_KEYWORD;
		String outputMzIdentMLFile_tandem = dirToStoreTempFile + "/Test_Toxo_1D_Slice43_tandem" + "_" + randomNumberIdentifier +".mzid";
		commandparser = cmp.createMzIdentML(searchEngineKeyword, paramFileToCreate_tandem.getAbsolutePath(), outputFileProduced_tandem, outputMzIdentMLFile_tandem);
		ec = new ExecuteCommands();
		ec.execute(commandparser);
		
		/******************************************************************/
		// Call the multiple search engine
		/******************************************************************/
		String mzIdeFile_1 					= outputMzIdentMLFile_omssa; 
		String se_1 						= Constants.OMSSA_ID; 
		String mzIdeFile_2 					= outputMzIdentMLFile_tandem; 
		String se_2 						= Constants.TANDEM_ID;
		String parserInputFile 				= parserConfigurationInput.getAbsolutePath();
		String parserInputDelimiter 		= parserFileDelimiter;
		
		String outputFile =  dirToStoreTempFile + "/FinalOutput" + "_" + randomNumberIdentifier +".txt"; 
		String debugFile =  dirToStoreTempFile + "/FinalOutput_Verbose" + "_" + randomNumberIdentifier +".txt";
		
		log.info(" mzFile 1 : " + mzIdeFile_1 + "mzFile 2 : " + mzIdeFile_2 + "\t ParserInput : " + parserInputFile);
		
		CallMultipleSearchMethod cm = new CallMultipleSearchMethod(mzIdeFile_1, se_1, mzIdeFile_2,se_2, 
																parserInputFile, parserInputDelimiter, 
																outputFile, debugFile);
		cm.launchMultipleSearchEngine();
		
	}

}
