package org.liverpool.utils;

import java.io.*;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.parsers.ConstructParamFileForMzIdentMLParser;

/*
 * Convert to omssa like csv file
 * Create the template file
 * Run all the jars with the parameters
 */
public class PerformPostProcessingUsingMzLib {
	
	static Logger log = Logger.getLogger(PerformPostProcessingUsingMzLib.class);
	
	/**
	 * 
	 * @param pipelineSummaryFile
	 * @param omssaLikeOutputFile
	 * @param csvThreshold
	 * @param decoyString
	 */
	public int createOmssaLikeCSV(String pipelineSummaryFile,String omssaLikeOutputFile,double csvThreshold,String decoyString){
		String delimiter = "\t";
		ConvertWholeSummaryFileToOmssaCSVFormat cv = new ConvertWholeSummaryFileToOmssaCSVFormat(pipelineSummaryFile, csvThreshold, delimiter, decoyString, omssaLikeOutputFile);
		cv.createOmssaLikeFile();
		
		//log.info("Post-processing - csv summary file created.");
		return 1;
	}
	
	/**
	 * 
	 * @param searchEngineInput
	 * @param parserConfigurationInput
	 * @param paramFileToCreate
	 */
	public HashMap<String,String> createPostProcessingParamFile(File searchEngineInput,File parserConfigurationInput,File paramFileToCreate){

		String searchFileDelimiter 					= "="; 
		String parserFileDelimiter					= "=";
		File umodFile								= new File("resources/UMOD_TABLE.csv"); 
		String umodFileDelimiter					= ",";	
		String omssaIdentifierInHeaderInUmodFile	= "Omssa_ID";	
		File paramKeywordFile 						= new File("resources/paramKeywords.txt");
		File enzymeFile								= new File("resources/enzymeList.csv"); 
		String enzymeFileDelimiter					= "=";
		File paramTemplateFile = new File("templates/mzprocessing_paramfile_template.txt");
		
		ConstructParamFileForMzIdentMLParser cp = new ConstructParamFileForMzIdentMLParser(searchEngineInput,searchFileDelimiter, 
																			parserConfigurationInput,parserFileDelimiter,
																			umodFile, umodFileDelimiter,
																			omssaIdentifierInHeaderInUmodFile,
																			enzymeFile,enzymeFileDelimiter,
																			paramKeywordFile);
		HashMap<String,String> resolvedValuesForParam = cp.fillKeywordParametersForParamFile(); 
		cp.createParamFile(paramFileToCreate, paramTemplateFile);
		
		//log.info("Post-processing : mzLib parameter file created.");
		
		return resolvedValuesForParam;
	}
	
	public void flushStream(InputStream stdin, InputStream stderr){
		try{
		InputStreamReader isr_in = new InputStreamReader(stdin);
        InputStreamReader isr_err = new InputStreamReader(stderr);
        BufferedReader br_1 = new BufferedReader(isr_in);
        BufferedReader br_2 = new BufferedReader(isr_err);
        String line=null;
        while ( (line = br_1.readLine()) != null)
            System.out.println(line);
        while ( (line = br_2.readLine()) != null)
            System.out.println(line);
		}catch(Exception e){
			
		}
	}
	/**
	 * 
	 * @param proteoInput_search
	 * @param proteoInput_database
	 * @param pipelineSummaryFile
	 * @param omssaLikeOutputFile
	 * @param csvThreshold
	 * @param fdrThreshold
	 * @param summarymzIdentFile
	 */
	public void performPostProcessing(String proteoInput_search,String proteoInput_database,String pipelineSummaryFile,
									 String omssaLikeOutputFile,double csvThreshold, double fdrThreshold,String summarymzIdentFile){
		
		try{
			
		    // Create param file
			File searchEngineInput = new File(proteoInput_search);
			File parserConfigurationInput = new File(proteoInput_database);
			
			File temp_param_file = File.createTempFile("mzlib-temp", ".csv");
			//temp_param_file.deleteOnExit();
			
			HashMap<String,String> inputKeys = createPostProcessingParamFile(searchEngineInput,parserConfigurationInput,temp_param_file);
			
			// Extract the database file name
			String database_file = null; 
			if(inputKeys.containsKey("database_file"))
				database_file = inputKeys.get("database_file");
			else database_file = null;
			
			String decoyString = null;
			if(inputKeys.containsKey("decoy_regex"))
				decoyString = inputKeys.get("decoy_regex");
			else decoyString = null;
			
			if((database_file == null) || (decoyString == null)){
				String errMsg = "\n Error - Search Database file of the decoy identifier not found in the input parameter files. " +
						"Check the 'database_file' and 'decoy_regex' fields in the input files, or " +
						"provide the correct input files.\n" +
						"Exiting now...";
				log.fatal(errMsg);
				System.exit(0);
			}
			
			// Create Omssa like file
			int omssa_done = createOmssaLikeCSV(pipelineSummaryFile,omssaLikeOutputFile,csvThreshold,decoyString);
		
			if(omssa_done != 1){
				String errorMessage = "Post-processing : Unbale to convert the Whole Summary file to csv format ";
				System.out.println(errorMessage);
				//log.fatal(errorMessage);
				System.exit(0);
			}
		
			
			// create commands
			//java -jar -Xmx1024m mzidentml-lib.jar  Csv2mzid Toxo1DSFIF.csv Toxo1DSFIF.mzid -paramsFile toxo_proteoannotator_params.csv -cvAccessionForPSMOrdering "MS:1001874" -decoyRegex Rev_
			String csv2mzid_command = "java -jar -Xmx1024m lib/mzidentml-lib.jar  Csv2mzid " +  omssaLikeOutputFile + " " + summarymzIdentFile +" -paramsFile " + temp_param_file.getAbsolutePath() + " -cvAccessionForPSMOrdering MS:1001874 -decoyRegex "+ decoyString;
			
			//java -jar -Xmx1024m mzidentml-lib.jar Threshold Toxo1DSFIF.mzid Toxo1DSFIF.mzid -isPSMThreshold true -cvAccForScoreThreshold "MS:1001874" -threshValue 0.01 -scoreLowToHigh true
			String mzlib_threshold_command = "java -jar -Xmx1024m lib/mzidentml-lib.jar Threshold "+ summarymzIdentFile + " " + summarymzIdentFile + " -isPSMThreshold true -cvAccForScoreThreshold MS:1001874 -threshValue "+ fdrThreshold + " -scoreLowToHigh true";
			//String mzlib_threshold_command = "java -jar -Xmx1024m lib/mzidentml-lib.jar Threshold /Users/riteshk/Ritesh_Work/TestSpace/mzId-Testing/Andy-mzIdLibrary/Test/sfif.mzid " + threshold_summarymzIdentFile + " -isPSMThreshold true -cvAccForScoreThreshold MS:1001874 -threshValue  0.01  -scoreLowToHigh true";
			
			//java -jar -Xmx1024m mzidentml-lib.jar ProteoGrouper Toxo1DSFIF.mzid Toxo1DSFIF.mzid  -requireSIIsToPassThreshold true -verboseOutput  false -cvAccForSIIScore "MS:1001874" -logTransScore true -includeOnlyBestScorePerPep false
			String mzlib_proteogroup_command = "java -jar -Xmx1024m lib/mzidentml-lib.jar ProteoGrouper " + summarymzIdentFile + " " + summarymzIdentFile + " -requireSIIsToPassThreshold true -verboseOutput  false -cvAccForSIIScore MS:1001874 -logTransScore true -includeOnlyBestScorePerPep false"; 
			
			//java -jar -Xmx1024m mzidentml-lib.jar empai Toxo1DSFIF.mzid Toxo1DSFIF.mzid  -fastaFile /Users/riteshk/Ritesh_Work/Toxo/ToxoDB/combined_gene_models.fasta -accessionSplitRegex "/ /" 
			//String mzlib_empai_command = "java -jar lib/mzidentml-lib.jar empai "  + summarymzIdentFile + " " + summarymzIdentFile + " -fastaFile " + database_file + " -accessionSplitRegex \"/ /\" "; // Pass as array becasue we need  exactly - "/ /"               
			String [] mzlib_empai_command = new String[11];
			mzlib_empai_command[0] = "java";
			mzlib_empai_command[1] = "-jar";
			mzlib_empai_command[2] = "-Xmx1024m";
			mzlib_empai_command[3] = "lib/mzidentml-lib.jar";
			mzlib_empai_command[4] = "empai";
			mzlib_empai_command[5] = summarymzIdentFile;
			mzlib_empai_command[6] = summarymzIdentFile;
			mzlib_empai_command[7] = "-fastaFile";
			mzlib_empai_command[8] = database_file;
			mzlib_empai_command[9] = "-accessionSplitRegex";
			mzlib_empai_command[10] = "/ /";
			
			//java -jar -Xmx1024m mzidentml-lib.jar MzIdentMLToCSV Toxo1DSFIF.mzid  Toxo1DSFIF_processed.csv -exportType exportProteinGroups
			String  mzlib_mztocsv_command = "java -jar -Xmx1024m lib/mzidentml-lib.jar MzIdentMLToCSV "+ summarymzIdentFile + " " + summarymzIdentFile.concat("_processed.csv") + " -exportType exportProteinGroups";
			
			//java -jar -Xmx1024m mzidentml-lib.jar MzIdentMLToCSV Toxo1DSFIF.mzid  Toxo1DSFIF_processed_PSM.csv -exportType exportPSMs
			String  mzlib_mztocsv_command_1 ="java -jar -Xmx1024m lib/mzidentml-lib.jar MzIdentMLToCSV "+ summarymzIdentFile + " " + summarymzIdentFile.concat("_processed_PSM.csv") + "  -exportType exportPSMs";
			
			//java -jar -Xmx1024m mzidentml-lib.jar MzIdentMLToCSV Toxo1DSFIF.mzid  Toxo1DSFIF_processed_pags.csv -exportType exportRepProteinPerPAGOnly
			String mzlib_mztocsv_command_2 = "java -jar -Xmx1024m lib/mzidentml-lib.jar MzIdentMLToCSV " + summarymzIdentFile + " " + summarymzIdentFile.concat("_processed_pags.csv") + " -exportType exportRepProteinPerPAGOnly";
	
			//System.out.println(mzlib_empai_command);
			
			Runtime rt = Runtime.getRuntime();
			
			Process proc = rt.exec(csv2mzid_command);
			InputStream stdin = proc.getInputStream();
	        InputStream stderr = proc.getErrorStream();
	        flushStream(stdin,stderr);
	        proc.waitFor(); 
	        stdin.close();
	        stderr.close();
	        proc.exitValue();
	        proc.destroy();
	        
            
			System.out.println(mzlib_threshold_command);
			proc = rt.exec(mzlib_threshold_command);
			stdin = proc.getInputStream();
	        stderr = proc.getErrorStream();
	        flushStream(stdin,stderr);
	        proc.waitFor(); 
	        stdin.close();
	        stderr.close();
	        proc.exitValue();
	        proc.destroy();
	        
	        
			System.out.println(mzlib_proteogroup_command);
			proc = rt.exec(mzlib_proteogroup_command);
			stdin = proc.getInputStream();
	        stderr = proc.getErrorStream();
	        flushStream(stdin,stderr);
	        proc.waitFor(); 
	        stdin.close();
	        stderr.close();
	        proc.exitValue();
	        proc.destroy();
	        
			System.out.println(mzlib_empai_command);
			proc = rt.exec(mzlib_empai_command);
			stdin = proc.getInputStream();
	        stderr = proc.getErrorStream();
	        flushStream(stdin,stderr);
	        proc.waitFor(); 
	        stdin.close();
	        stderr.close();
	        proc.exitValue();
	        proc.destroy();
	        
			
			System.out.println(mzlib_mztocsv_command);
			proc = rt.exec(mzlib_mztocsv_command);
			stdin = proc.getInputStream();
	        stderr = proc.getErrorStream();
	        flushStream(stdin,stderr);
	        proc.waitFor(); 
	        stdin.close();
	        stderr.close();
	        proc.exitValue();
	        proc.destroy();
	        
			System.out.println(mzlib_mztocsv_command_1);
			proc = rt.exec(mzlib_mztocsv_command_1);
			stdin = proc.getInputStream();
	        stderr = proc.getErrorStream();
	        flushStream(stdin,stderr);
	        proc.waitFor(); 
	        stdin.close();
	        stderr.close();
	        proc.exitValue();
	        proc.destroy();
	        
			System.out.println(mzlib_mztocsv_command_2);
			proc = rt.exec(mzlib_mztocsv_command_2);
			stdin = proc.getInputStream();
	        stderr = proc.getErrorStream();
	        flushStream(stdin,stderr);
	        proc.waitFor(); 
	        stdin.close();
	        stderr.close();
	        proc.exitValue();
	        proc.destroy();
	        
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String [] args) {
		
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		/*
		String proteoInput_search = "inputFiles/inputFileTemplate.txt";
		String proteoInput_database = "inputFiles/mzIdentMLParser_inputFile.txt";
		String pipelineSummaryFile = "/Users/riteshk/Ritesh_Work/ProteoAnnotator-ToxoResults/SearchResults/WholeSummary_sfif.txt";
		String omssaLikeOutputFile = "/Users/riteshk/Ritesh_Work/ProteoAnnotator-ToxoResults/postprocess/WholeSummary_sfif.csv";
		double csvThreshold = 0.1;
		double fdrThreshold = 0.01;
		String summarymzIdentFile = "/Users/riteshk/Ritesh_Work/ProteoAnnotator-ToxoResults/postprocess/sfif.mzid";
		*/
		try{
			
			if(args.length != 7){
				System.out.println("Arguments: proteoAnnotator-searchInput.txt proteoAnnotator-databaseInput.txt " +
						"proteoAnnotator-SummaryFile.txt output.csv csv_threshold fdr_threshold output_mzidentMLFile.mzid");
				System.exit(0);
			}
			
			String proteoInput_search = args[0];
			String proteoInput_database = args[1];
			String pipelineSummaryFile = args[2];
			String omssaLikeOutputFile = args[3];
			double csvThreshold = Double.parseDouble(args[4]);
			double fdrThreshold = Double.parseDouble(args[5]);
			String summarymzIdentFile = args[6];
			
			PerformPostProcessingUsingMzLib pp = new PerformPostProcessingUsingMzLib();
			pp.performPostProcessing(proteoInput_search, proteoInput_database, pipelineSummaryFile, omssaLikeOutputFile, csvThreshold,fdrThreshold, summarymzIdentFile);
			
		}catch(NumberFormatException e){
			System.out.println("Error : Check the parameters and the threshold used. Not able to process FDR threshold");
			System.exit(0);
		}catch(Exception e){
			log.fatal(e.getMessage());
			System.out.println("Error : Post-processing failed... \n Exiting..");
			System.exit(0);
		}
	}
}
