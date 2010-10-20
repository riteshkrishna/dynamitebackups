package org.liverpool.pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.utils.ReadConfigurationFiles;

/**
 * 1. Get the dir with mgf files
 * 2. list the mgf files
 * 3. Get the name of the output dir
 * 4. Fill the inputFileTemplate.txt
 * 
 * @author riteshk
 *
 */
public class RunPipelineForWholeDirectory {
	
	static Logger log = Logger.getLogger(RunPipelineForWholeDirectory.class);
	
	File templateFile;
	File keywordFile;
	HashMap<String,String> keywords;
	String inputTemplate;
	String inputDirWithMgfFiles;
	String outputDirToStoreOutputs;
	String inputDelimiter;
	String parserInputFile;
	String parserDelimiter;

	
	ReadConfigurationFiles rc;
	
	/**
	 * 
	 * @param templateFile
	 * @param inputDirWithMgfFiles
	 * @param outputDirToStoreOutputs
	 */
	public RunPipelineForWholeDirectory(String templateFile, String inputDirWithMgfFiles, String outputDirToStoreOutputs,
										String inputDelimiter,String parserInputFile,String parserDelimiter){
		try{
			
			this.keywordFile = new File("resources/dirBasedPipelineRunKeywords.txt");
			
			this.rc = new ReadConfigurationFiles();
			
			this.templateFile = new File(templateFile);
			this.inputDirWithMgfFiles = inputDirWithMgfFiles;
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
	 * Read the directory containing mgf files and create corresponding text files and output directories
	 */
	public ArrayList<String []> createPipelineCommands(){
		
		ArrayList<String []> commands = new ArrayList<String []>();
		
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
			// Get all the .mgf files in the directory
			File dir = new File(inputDirWithMgfFiles);
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".mgf");
				}
			};
			File [] mgfFiles = dir.listFiles(filter);
		
			for(int i = 0; i < mgfFiles.length; i++){
				File mgf = mgfFiles[i];
				String fullpathname = mgf.getCanonicalPath();
				// remove extension .mgf from the name
				String fileName = mgf.getName();
				if(fileName.contains(".mgf"))
					fileName = fileName.replace(".mgf","");
				String outputPath = this.outputDirToStoreOutputs + "/dir"+fileName;
				
				String filledTemplate = this.inputTemplate.replace(textToReplace_input, fullpathname);
				filledTemplate = filledTemplate.replace(textToReplace_output, outputPath);
				
				// make dir and write the file in that dir
				File dirToMake = new File(outputPath);
				if(!dirToMake.mkdir()){
					log.fatal("Unable to make directory : " + outputPath + "...exiting !");
					System.exit(0);
				}
				String inputFileForSe = outputPath.concat("/input.txt");
				BufferedWriter br = new BufferedWriter(new FileWriter(inputFileForSe));
				br.write(filledTemplate);
				br.close();
				
				String [] runCommand = {outputPath,inputFileForSe,this.inputDelimiter,this.parserInputFile,this.parserDelimiter};
				commands.add(runCommand);
			}
		}catch(Exception e){
			log.fatal("Problem with input diretory with mgf files.");
			e.printStackTrace();
		}
		
		return commands;
	}
	
	/**
	 * the test function
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		/*
		String inputTemplate = "inputFiles/inputFileTemplate.txt";
		String inputMgfDir = "/Users/riteshk/Ritesh_Work/Toxo/Toxo_Test_MSDataset";
		String outputMgfDir = "/Users/riteshk/Ritesh_Work/TestSpace/pipeline_test/tmp";
		
		String inputDelimiter = "=";
		String parserInputFile = "inputFiles/mzIdentMLParser_inputFile.txt";
		String parserDelimiter = "=";
		*/
		if(args.length != 6){
			System.out.println("Arguments needed : inputTemplateFile inputMgfDir outputMgfDir inputDelimiter parserInputFile parserFileDelimiter");
			System.exit(0);
		}
		
		String inputTemplate   = args[0];
		String inputMgfDir     = args[1];
		String outputMgfDir    = args[2];
		String inputDelimiter  = args[3];
		String parserInputFile = args[4];
		String parserDelimiter = args[5];
		
		RunPipelineForWholeDirectory rp = new RunPipelineForWholeDirectory(inputTemplate,inputMgfDir,outputMgfDir,inputDelimiter,parserInputFile,parserDelimiter);
		ArrayList<String[]> comds = rp.createPipelineCommands();
		for(int i = 0; i < comds.size(); i++){
			System.out.println(comds.get(i));
			
			Pipeline pipeline = new Pipeline();
			pipeline.main(comds.get(i));
		}
		
	}	
	
	
}
