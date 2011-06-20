package org.liverpool.gff;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

public class ReadProteinRecordFromSummaryFile {
	

	/***********************   Do Not Edit *************************/
	/** Indicates the column numbers for the following fields in the Summary file**/
	final int AccessionColumn = 0;
	final int startColumn     = 7;
	final int endcolumn       = 8;
	/**************************************************************/
	
	ArrayList<ProteinResults> proteinHits;
	String summaryFile;
	String deocyIdentifier;
	
	/**
	 * 
	 * @param summaryFile
	 * @param deocyIdentifier
	 */
	ReadProteinRecordFromSummaryFile(String summaryFile, String deocyIdentifier){
		proteinHits = new ArrayList<ProteinResults>();
		this.summaryFile = summaryFile;
		this.deocyIdentifier = deocyIdentifier;
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public ArrayList<ProteinResults> readProteinResultsFromFile() throws Exception{
			
		Scanner scanner = new Scanner(new FileReader(new File(summaryFile)));
		
		try{
			while(scanner.hasNextLine()){
				processLine(scanner.nextLine());
			}
		}finally{
			scanner.close();
		}
		
		return proteinHits;
	}
	
	/**
	 * 
	 */
	void processLine(String line) throws Exception{
		
		if(line.isEmpty())
			return;
		
	    boolean decoy = false;
		String accession;
		long start;
		long end;
		
		String [] fields = line.split("\t",-1); // This will force split to continue for blank spaces as well
	    
		if( (fields[AccessionColumn] == null) || (fields[startColumn] == null) || (fields[endcolumn] == null) ){
			Exception e = new Exception(" Emptry field (Accession = " + fields[AccessionColumn] 
							+ ", Start = " + fields[startColumn]+ " , end = " + fields[endcolumn] + ") in the summary file");
			throw e;
		}
		
		accession = fields[AccessionColumn];
		if(accession.contains(this.deocyIdentifier))
			decoy = true;
		start = Long.parseLong(fields[startColumn]);
		end   = Long.parseLong(fields[endcolumn]);
		
		// Handle the empty accessions by referring to the previous ones in the summary file
		if (accession.trim().isEmpty())
			accession = proteinHits.get(proteinHits.size() - 1).getAccession();
		
		ProteinResults pr = new ProteinResults(accession, decoy, start, end);
		
		
			
		proteinHits.add(pr);
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String []args) {
		
		String summaryFile     		   = args[0];
		String decoyIdentifier 		   = args[1];
		ReadProteinRecordFromSummaryFile rip = new ReadProteinRecordFromSummaryFile(summaryFile,decoyIdentifier);
		
		try{
			ArrayList<ProteinResults> prList = rip.readProteinResultsFromFile();
			
			for(int i = 0 ; i < prList.size(); i++){
				System.out.println(prList.get(i).getAccession() +"\t" +  prList.get(i).getDecoyOrNot() +"\t" + 
						prList.get(i).getstart() +"\t" + prList.get(i).getEnd() +"\t"); 
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
