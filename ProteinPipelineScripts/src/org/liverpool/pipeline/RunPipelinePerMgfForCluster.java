package org.liverpool.pipeline;

/**
 * Similar class like RunPipelineForWholeDirecory.java, but runs for only 1 Mgf File. The idea is
 * this class can be used in case we have to give one-by-one file as input to the pipeline. Perhaps
 * this will also help in running the task on a distributed platform where a number of files can
 * be individually run using this class.
 *  
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.utils.ReadConfigurationFiles;


public class RunPipelinePerMgfForCluster {
	
	static Logger log = Logger.getLogger(RunPipelinePerMgfForCluster.class);
	
	File templateFile;
	File keywordFile;
	HashMap<String,String> keywords;
	String inputTemplate;
	String inputMgfFile;
	String outputDirToStoreOutputs;
	String inputDelimiter;
	String parserInputFile;
	String parserDelimiter;

	
	ReadConfigurationFiles rc;
	
	public RunPipelinePerMgfForCluster(String templateFile, String inputMgfFile, String outputDirToStoreOutputs,
										String inputDelimiter,String parserInputFile,String parserDelimiter){
		try{
			
			this.keywordFile = new File("resources/dirBasedPipelineRunKeywords.txt");
			
			this.rc = new ReadConfigurationFiles();
			
			this.templateFile = new File(templateFile);
			this.inputMgfFile = inputMgfFile;
			this.outputDirToStoreOutputs = outputDirToStoreOutputs;
			this.inputDelimiter = inputDelimiter;
			this.parserInputFile = parserInputFile;
			this.parserDelimiter = parserDelimiter;
			
			this.inputTemplate = null;
			this.keywords = new HashMap<String,String>();
			
		}catch(NullPointerException ne){
			log.fatal("Problem opening the input template file");
			System.exit(0);
		}
	}
	
	/**
	 * Read the template file content into internal String, and validate it against the keywords.
	 * If all is well, then return success
	 */
	boolean validateAndcreateTemplateString(){
		try{
			// Get the input template content
			this.inputTemplate = rc.readTemplateCommandFile(templateFile);
			// Read the template keywords needed for sure in the input
			Iterator <String> keywords = rc.readKeywordDefinitionFile(keywordFile).iterator();
			while(keywords.hasNext()){
				String key = keywords.next();
				this.keywords.put(key, "");
				if(!inputTemplate.contains(key)){
					log.fatal("Problem with input file. The key -  " + key +" not found");
					System.exit(0);
				}
			}
		}catch(FileNotFoundException e){
			log.fatal("File not found. Check template and keyword file");
		}	
		return true;
	}
	
	/**
	 * Create corresponding template text files and output directory
	 */
	public String [] createPipelineCommands(){
		
		String [] runCommand = new String[5];
		
		// Read the template content and validate
		if( !validateAndcreateTemplateString()){
			log.fatal("Validation of input file failed");
			System.exit(0);
		}
		
		String inputKeyToReplace = null;
		String outputKeyToReplace = null;		
		Iterator<String> allKeys = this.keywords.keySet().iterator();
		while(allKeys.hasNext()){
			String key = allKeys.next();
			if(key.contains("input"))
				inputKeyToReplace = key;
			if(key.contains("output"))
				outputKeyToReplace = key;
		}
		String textToReplace_input  = "{{ "+ inputKeyToReplace  +" }}";
		String textToReplace_output = "{{ "+ outputKeyToReplace +" }}";
		
		try{
			
			File mgf = new File(this.inputMgfFile);
		
			String fullpathname = mgf.getCanonicalPath();
			// remove extension .mgf from the name
			String fileName = mgf.getName();
			if(fileName.contains(".mgf"))
				fileName = fileName.replace(".mgf","");
			else{
				// Signal error that no MGF file provided..
				String errMsg = "The provided file is not an MGF file. Please check the file and ensure it has an .mgf extension.\n" +
				"Existing ProteoAnnotator";
				log.fatal(errMsg);
				System.out.println(errMsg);
				System.exit(0);
			}
			
			String outputPath = this.outputDirToStoreOutputs +  File.separator + "dir"+fileName;
			
			String filledTemplate = this.inputTemplate.replace(textToReplace_input, fullpathname);
			filledTemplate = filledTemplate.replace(textToReplace_output, outputPath);
			
			// make dir and write the file in that dir
			File dirToMake = new File(outputPath);
			if(!dirToMake.mkdir()){
				log.fatal("Unable to make directory : " + outputPath + "...exiting !");
				System.exit(0);
			}
			String inputFileForSe = outputPath.concat( File.separator + "input.txt");
			BufferedWriter br = new BufferedWriter(new FileWriter(inputFileForSe));
			br.write(filledTemplate);
			br.close();
			
			//runCommand = {outputPath,inputFileForSe,this.inputDelimiter,this.parserInputFile,this.parserDelimiter};
			runCommand[0] = outputPath;
			runCommand[1] = inputFileForSe;
			runCommand[2] = this.inputDelimiter;
			runCommand[3] = this.parserInputFile;
			runCommand[4] = this.parserDelimiter;

		}catch(Exception e){
			log.fatal("Problem with input diretory with mgf files.");
			e.printStackTrace();
		}
		
		return runCommand;
	}
	
	/**
	 * the test function
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		if(args.length != 4){
			System.out.println("Arguments needed : inputTemplateFile parserInputFile inputMgfFile outputMgfDir parserInputFile");
			System.exit(0);
		}
		
		String inputTemplate    = args[0];
		String parserInputFile  = args[1];
		String inputMgfFile     = args[2];
		String outputMgfDir     = args[3];
		String inputDelimiter   = "=";
		String parserDelimiter  = "=";
		
		// Create output directory if required, and do the necessary checks
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return !name.startsWith(".");
		    }
		};
		File dir = new File(outputMgfDir);
		String[] children = dir.list();
		
		if(children != null){
			children = dir.list(filter);
			if(children.length >= 1){
					String errMsg = "\n\n ** Error \n\n" +
							"The provided directory is not empty. Please empty the directory or provide a new directory.\n" +
					"Existing ProteoAnnotator\n";
					log.fatal(errMsg);
					System.out.println(errMsg);
					System.exit(0);
			}
		}else{
			boolean create = dir.mkdir();
			if(!create){
				String errMsg = "Unable to create output directory.\n" +
				"Existing ProteoAnnotator";
				log.fatal(errMsg);
				System.out.println(errMsg);
				System.exit(0);
			}
		}
		
		RunPipelinePerMgfForCluster rp = new RunPipelinePerMgfForCluster(inputTemplate,inputMgfFile,outputMgfDir,inputDelimiter,parserInputFile,parserDelimiter);
		String [] comd = rp.createPipelineCommands();
		
		try{	
			Pipeline pipeline = new Pipeline();
			pipeline.main(comd);
		}catch(Exception e){
			log.fatal("Pipeline failed...");
			log.fatal(e.getMessage());
		}
	}	
	
	
}

