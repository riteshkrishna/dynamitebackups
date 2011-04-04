package org.liverpool.utils;

/**
 * Script to process the FinalOutput_xxx.txt files. This program arranges the output produced 
 * by the pipeline in an arranged and grouped manner. The peptide and spectrum matches are grouped
 * according to proteins, and combined score is computed for each protein. 
 * 
 */
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class CreateSummaryFile {

	static Logger log = Logger.getLogger(CreateSummaryFile.class);
	
	/*********************************************************************************/
	// Don't change this - These strings are block markers in the FinalOutput_xxx.txt
	// files produced by pipeline. We will use these strings to identify the blocks
	// we are processing
	String blockIdentifierInTheInputFile_ot = "Sequences in the container : [ot]";
	String blockIdentifierInTheInputFile_o = "Sequences in the container : [o]";
	String blockIdentifierInTheInputFile_t = "Sequences in the container : [t]";
	/*********************************************************************************/
	
	HashMap <String, ArrayList<ArrayList<String>>> allProteinSummary;
	HashMap<String, Double> allProteinScore;
	
	String inputFileName;
	String summaryFileName;
	
	String currentBlockOfProcessing;
	
	/**
	 * 
	 * @param inputFile
	 * @param summaryFile
	 */
	public CreateSummaryFile(String inputFile, String summaryFile){
		this.inputFileName = new String(inputFile);
		this.summaryFileName = new String(summaryFile);
		
		allProteinSummary = new HashMap <String, ArrayList<ArrayList<String>>>();
		currentBlockOfProcessing = new String();
		allProteinScore = new HashMap<String, Double>();
	}
	
	/**
	 * 
	 */
	public void createTheSummaryFile() throws Exception{
		
		Scanner scanner = new Scanner(new FileReader(new File(inputFileName)));
		
		try{
			// Process each line and arrange the records according to each protein
			while(scanner.hasNextLine()){
				processLine(scanner.nextLine());
			}
		}finally{
			scanner.close();
		}

		// Compute single score for each protein
		computeScoreForEachProtein();
		
		writeInTheOutputFile();
	}
	
	/**
	 * 
	 * @param line
	 */
	void processLine(String line){
		line  = line.trim();
		
		if(line.isEmpty())
			return;
		
		if(line.equalsIgnoreCase(blockIdentifierInTheInputFile_ot))
			currentBlockOfProcessing = "ot";
		else if(line.equalsIgnoreCase(blockIdentifierInTheInputFile_o))
			currentBlockOfProcessing = "o";
		else if(line.equalsIgnoreCase(blockIdentifierInTheInputFile_t))
			currentBlockOfProcessing = "t";
		
		
	    String lineNumber = " ";
	    String spectraId  = " ";
	    String peptideSeq = " ";
	    String fdrScore   = " ";
	    String calcMass   = " ";
	    String group      = " ";
	    String protAccn   = " ";
	    String start      = " ";
	    String end        = " ";
	    String simpleFdr  = " ";
	    String mods       = " ";
	    String charge     = " ";
	    String expMass    = " ";
	    
	    String [] fields = line.split("\\,",-1); // This will force split to continue for blank spaces as well
	    if(fields.length == 13){
	    	lineNumber = fields[0];
	    	spectraId  = fields[1];
		    peptideSeq = fields[2];
		    fdrScore   = fields[3];
		    calcMass   = fields[4];
		    group      = fields[5];
		    protAccn   = fields[6];
		    start      = fields[7];
		    end        = fields[8];
		    simpleFdr  = fields[9];
		    mods       = fields[10];
		    charge     = fields[11];
		    expMass    = fields[12];
	    }else
	    	return;
	    
	    // take the prot acc and add in the map...
	    
	    ArrayList<String> pepSpectrumMatch = new ArrayList<String>();
	    pepSpectrumMatch.add(spectraId);
	    pepSpectrumMatch.add(peptideSeq);
	    pepSpectrumMatch.add(fdrScore);    // Note 1  : Index = 2 for FDR Score
	    pepSpectrumMatch.add(calcMass);
	    pepSpectrumMatch.add(group);
	    pepSpectrumMatch.add(start);
	    pepSpectrumMatch.add(end);
	    pepSpectrumMatch.add(simpleFdr);
	    pepSpectrumMatch.add(mods);
	    pepSpectrumMatch.add(charge);
	    pepSpectrumMatch.add(expMass);
	    pepSpectrumMatch.add(currentBlockOfProcessing); // Add the SE combination identifier
	    
	    if(allProteinSummary.containsKey(protAccn)){
	    	ArrayList<ArrayList<String>> records = allProteinSummary.get(protAccn);
	    	records.add(pepSpectrumMatch);
	    	allProteinSummary.put(protAccn, records);
	    }else{
	    	ArrayList<ArrayList<String>> records = new ArrayList<ArrayList<String>>(); 
	    	records.add(pepSpectrumMatch);
	    	allProteinSummary.put(protAccn, records);
	    }
		
	}
	
	/**
	 * Compute a single score for each protein 
	 */
	void computeScoreForEachProtein(){
		
		int fdrIndex = 2; //  Mentioned at "Note 1  : Index = 2 for FDR Score"
		
		Iterator<String> allProt = allProteinSummary.keySet().iterator();
		
		while(allProt.hasNext()){
			String key = allProt.next();
			ArrayList<ArrayList<String>> records = allProteinSummary.get(key);
			
			Double finalFdrScore = 1.0;
			for(int i = 0; i < records.size(); i++)
				finalFdrScore = finalFdrScore * Double.parseDouble(records.get(i).get(fdrIndex));
			
			allProteinScore.put(key, finalFdrScore);
		}
	}
	
	/**
	 * 
	 */
	void writeInTheOutputFile() throws Exception{
		try{
			FileWriter fstream = new FileWriter(summaryFileName);
			BufferedWriter out = new BufferedWriter(fstream);
			
			Iterator<String> allProt = allProteinSummary.keySet().iterator();
			while(allProt.hasNext()){
				String protName = allProt.next();
				Double score = allProteinScore.get(protName);
				
				ArrayList<ArrayList<String>> records = allProteinSummary.get(protName);
				
				out.write("\n" + protName + "\t" + score); 
				for(int i = 0; i < records.size(); i++){
					ArrayList<String> thisRecord = records.get(i);
					for(int j = 0; j < thisRecord.size(); j++){
						out.write("\t" + thisRecord.get(j));
					}
					out.write("\n\t");
				}
			}	
			
			out.close();
		}catch(Exception e){
			log.fatal(summaryFileName + " cann't be created...write failed.");
			throw e;
		}
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		String inputFile = args[0];
		String summaryFile = args[1];
		
		try{
			CreateSummaryFile cs = new CreateSummaryFile(inputFile, summaryFile);
			cs.createTheSummaryFile();	
		}catch(Exception e){
			log.fatal(e.getMessage());
			e.printStackTrace();
		}
		
	}

}
