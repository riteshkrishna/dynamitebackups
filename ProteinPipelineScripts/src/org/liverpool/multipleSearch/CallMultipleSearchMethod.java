package org.liverpool.multipleSearch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.utils.ReadConfigurationFiles;
import org.liverpool.mzIdentML.CombineSearchEngines;
import org.liverpool.pipeline.Constants;
import org.liverpool.pipeline.ExecuteCommands;

/**
 * 
 * 
 * @author riteshk
 *
 */
public class CallMultipleSearchMethod {

	static Logger log = Logger.getLogger(ReadConfigurationFiles.class);
	
	String mzIdentMLFile_1;
	String mzIdentMLFile_2;
	String searchEngine_1;
	String searchEngine_2;
	String parserInputFile;
	String parserFileDelimiter;
	String outputFile;
	String debugFile;
	
	HashMap <String, String> parserInputContent;
	int rank_threshold;
	int decoy_ratio;
	
	public CallMultipleSearchMethod(String searchEngineFile_1, String searchEngine_1,
									String searchEngineFile_2, String searchEngine_2,
									String parserInputFile,String parserFileDelimiter,
									String outputFile,
									String debugFile){
		
		this.mzIdentMLFile_1 = searchEngineFile_1;
		this.mzIdentMLFile_2 = searchEngineFile_2;
		this.searchEngine_1 = searchEngine_1;
		this.searchEngine_2 = searchEngine_2;
		this.parserInputFile = parserInputFile;
		this.parserFileDelimiter = parserFileDelimiter;
		this.outputFile = outputFile;
		this.debugFile = debugFile;
		
		parserInputContent = new HashMap <String, String>();
		rank_threshold = 0;
		decoy_ratio    = 0;
				
	}
	
	/**
	 * 
	 */
	void extractRankAndDecoyInformationFromParserInputFile(){
		
		log.info("The Parser input file is : " + parserInputFile);
		
		try{
			ReadConfigurationFiles rc = new ReadConfigurationFiles();
			parserInputContent = rc.readInputCsvFile(new File(parserInputFile),parserFileDelimiter);
			if (parserInputFile == null){
				log.fatal("Unable to read the contents of Parser input file");
				throw new Exception();
			}
			
			// Check if the decoy_ratio is present in the input
			if(parserInputContent.get(Constants.DECOY_RATIO) != null)
				this.decoy_ratio = Integer.parseInt(parserInputContent.get(Constants.DECOY_RATIO));
			else {
				log.fatal("No value found for decoy ratio in the parser input file");
				throw new Exception();
			}
			
			//Check if rank_threshold are present in the input
			if(parserInputContent.get(Constants.RANK_THRESHOLD) != null)
				this.rank_threshold = Integer.parseInt(parserInputContent.get(Constants.RANK_THRESHOLD));
			else {
				log.fatal("No value found for rank threshold in the parser input file");
				throw new Exception();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * An example input --
	 * 
	 * exampleMzIDFiles/Toxo_1D_Slice43_omssa.mzid omssa 
	 * exampleMzIDFiles/Toxo_1D_Slice43_tandem.mzid X!Tandem 
	 * 2 3 
	 * output/test_sept12.txt 
	 * output/test_sept12_debug.txt
	 */
	public void launchMultipleSearchEngine(){
		
		extractRankAndDecoyInformationFromParserInputFile();
		
		String [] args = new String[8];
		args[0] = this.mzIdentMLFile_1;
		args[1] = this.searchEngine_1;
		args[2] = this.mzIdentMLFile_2;
		args[3] = this.searchEngine_2;
		args[4] = Integer.toString(rank_threshold);
		args[5] = Integer.toString(decoy_ratio);
		args[6] = this.outputFile;
		args[7] = this.debugFile;
		
		try{
			String [] searchEngineNames = {args[1],args[3]};
			CombineSearchEngines cs = new CombineSearchEngines(searchEngineNames);
			cs.runTheMultipleSearchPipeline(args);
		}catch(Exception e){
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

		String mzIdeFile_1 = "logs/Test_Toxo_1D_Slice43_omssa_RK.mzid"; 
		String se_1 = "omssa"; 
		String mzIdeFile_2 = "logs/Test_Toxo_1D_Slice43_tandem_RK.mzid"; 
		String se_2 = "X!Tandem"; 
		String parserInputFile = "inputFiles/mzIdentMLParser_inputFile.txt";
		String parserInputDelimiter = "=";
		String outputFile = "logs/test_sept13.txt"; 
		String debugFile = "logs/test_sept13_debug.txt";
		
		CallMultipleSearchMethod cm = new CallMultipleSearchMethod(mzIdeFile_1, se_1, mzIdeFile_2,se_2, 
																parserInputFile, parserInputDelimiter, 
																outputFile, debugFile);
		cm.launchMultipleSearchEngine();
	}

}
