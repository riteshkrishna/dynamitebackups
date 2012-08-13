package org.liverpool.utils;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

public class CreateSummaryOfTheCompleteDataset {
	
	static Logger log = Logger.getLogger(CreateSummaryOfTheCompleteDataset.class);
	
	// We want to process the files with Summary_xxx.txt files only
	String FINAL_OUTPUT_FILE_IDENTIFIER = "Summary_";	

	public class ProteinNameAndByteLocation{
		public String protAccn;
		public long startByte;
		public long endByte;
		
		public ProteinNameAndByteLocation(String prot, long start, long end){
			protAccn = prot;
			startByte = start;
			endByte = end;
		}
	};

	
	HashMap<File, ArrayList<ProteinNameAndByteLocation>> indexer; 
	ArrayList<File> summaryFiles;
	String motherResultDirWithSubDir;
	String outputFile;
	HashMap<String,ArrayList<File>> proteinsAndrespectiveFiles;
	
	/**
	 * 
	 * @param resultDir
	 */
	public CreateSummaryOfTheCompleteDataset(String resultDir,String outputFile){
		this.motherResultDirWithSubDir = resultDir;
		summaryFiles = new ArrayList<File>();
		indexer = new HashMap<File, ArrayList<ProteinNameAndByteLocation>>();
		proteinsAndrespectiveFiles = new  HashMap<String,ArrayList<File>>();
		this.outputFile = outputFile;
	}
	
	/**
	 * The main class to perform the task
	 * @param outputFile
	 * @throws Exception
	 */
	public void summarizeTheWholeDatasetResults() throws Exception{
		// Get the path names of all the files
		summaryFiles = getPathOfSummaryfilesFromEachSubDir(motherResultDirWithSubDir);
		
		// Create the indexer
		for(int i = 0; i < summaryFiles.size(); i++){
			File filepath = summaryFiles.get(i);
			ArrayList<ProteinNameAndByteLocation> idx = makeProteinIndex(filepath);
			indexer.put(filepath, idx);
		}
		
		writeTheSummaryFile(outputFile);
		
	}
	
	
	/**
	 * Get the path names of all the summary files residing within output 
	 * subdirectories located under the main output directory
	 * @param mainDirectory - the main output directory having subdirectories containing
	 * 						 output for each mgf file. 
	 * @return - The path of all the summary files
	 */
	ArrayList<File> getPathOfSummaryfilesFromEachSubDir(String mainDirectory) throws Exception{
		
		File dir = new File(mainDirectory);
		// This filter only returns directories
		FileFilter fileFilter = new FileFilter() {
		    public boolean accept(File file) {
		        return file.isDirectory();
		    }
		};
		
		File[] files = dir.listFiles(fileFilter);
		
		ArrayList<File> summaryFilePaths = new ArrayList<File>();
		
		for(int i = 0; i< files.length; i++){
			// This filter only returns files with names FinalOutput_xxx.txt
			FileFilter finalOutputFileFilter = new FileFilter() {
			    public boolean accept(File file) {
			        return file.getName().contains(FINAL_OUTPUT_FILE_IDENTIFIER);
			    }
			};
			File [] listInThisDir = files[i].listFiles(finalOutputFileFilter);
			
			for(int j = 0 ; j < listInThisDir.length; j++)
				summaryFilePaths.add(listInThisDir[j]);
		}
		
		return summaryFilePaths;
	} 
	
	/**
	 * 
	 * @param inputFile
	 * @return
	 * @throws Exception
	 */
	ArrayList<ProteinNameAndByteLocation> makeProteinIndex(File inputFile) throws Exception{
		
		ArrayList<ProteinNameAndByteLocation> proteinRecords = new ArrayList<ProteinNameAndByteLocation>();
		
		RandomAccessFile randomFile = new RandomAccessFile(inputFile,"r");
				
		String strLine = "";
		String protAccn = "";
		String prevProtAccn = "";
		long endPosition = 0;
		
		long startPosition = randomFile.getFilePointer();
		
		while((strLine = randomFile.readLine()) != null){
		
			String [] records = strLine.split("\t");	
			
			if(records.length == 0)
				continue;
			
			protAccn = records[0].trim();
			
			if(protAccn.length() > 1){
				prevProtAccn = protAccn;
				String newLine = "";
				String strLine_2 = "";
				long fileMarker = -1;
			
				do{
					fileMarker = randomFile.getFilePointer(); // Mark the location before reading another line	
					if((strLine_2 = randomFile.readLine()) != null){
						if(strLine_2.contains("\t")){
							String [] records_next = strLine_2.split("\t");
							if(records_next.length != 0)
								newLine = records_next[0].trim();
							else newLine = "Get out of the loop !!";
							System.out.println(" In the loop : " + newLine);
						}
					}else{
						newLine = "Get out of the loop !!";
					}
				}while(newLine.length() < 1);
							
				// Reset the see location to beginning of the last line read
				if(fileMarker != -1)
					randomFile.seek(fileMarker);
				
				endPosition = randomFile.getFilePointer() - 1;
				
				ProteinNameAndByteLocation p = new ProteinNameAndByteLocation(prevProtAccn, startPosition, endPosition);
        		proteinRecords.add(p);

        		startPosition = endPosition + 1;
        		
        		// Remember which Protein is present in which files 
        		if(proteinsAndrespectiveFiles.containsKey(prevProtAccn)){
        			ArrayList <File> associatedFiles = proteinsAndrespectiveFiles.get(prevProtAccn);
        			associatedFiles.add(inputFile);
        			proteinsAndrespectiveFiles.put(prevProtAccn, associatedFiles);
        		}else{
        			ArrayList <File> associatedFiles = new ArrayList<File>();
        			associatedFiles.add(inputFile);
        			proteinsAndrespectiveFiles.put(prevProtAccn, associatedFiles);
        		}
        		
			}
			
		}
		
		randomFile.close();
		
		return proteinRecords;
		
	}

	
	/**
	 * 
	 * @param outputFile
	 */
	void writeTheSummaryFile(String outputFile) throws Exception{
		
		FileOutputStream fout = new FileOutputStream(new File(outputFile));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(fout)));
		
		Iterator<String> protNames = proteinsAndrespectiveFiles.keySet().iterator();
		
		while(protNames.hasNext()){
		
			String protName = protNames.next();
			ArrayList<File> relatedFiles = proteinsAndrespectiveFiles.get(protName);
			
			for(int i = 0; i < relatedFiles.size(); i++){
				File currentFile = relatedFiles.get(i);
				ArrayList<ProteinNameAndByteLocation> indexForThisFile = indexer.get(currentFile);
				
				// find the index of the protName in the list of objects
				RandomAccessFile randomFile = new RandomAccessFile(currentFile,"r");
				
				boolean found = false;	
				for(int k = 0; (k < indexForThisFile.size()) && (found == false); k++){
				
					ProteinNameAndByteLocation p = indexForThisFile.get(k);
					if (p.protAccn.equals(protName)){
						found  = true;
						
						long start = p.startByte;
						long end = p.endByte;
						
						randomFile.seek(start);
						int buffLength = (int)(end - start);
						
						byte [] buffer = new byte[buffLength];
						randomFile.read(buffer, 0, buffLength);
						
						String content = new String(buffer);
						content = content.trim();
						
						out.write("\n" + content + "\t" + currentFile.getAbsolutePath());
					}
				}
				
				randomFile.close();
			}
		}
		
		out.close();
	}
	
	
	/**
	 * 
	 * @param args[0] = resultDir
	 * @param args[1] = outputFile
	 */
	public static void main(String []args) throws Exception{
		
		if(args.length != 2){	
			System.out.println("Creating Summary files from the complete dataset");
			System.out.println("Usage : [arg0] result_directory [arg1] outputfile_name");
		}
		
		CreateSummaryOfTheCompleteDataset csf = new CreateSummaryOfTheCompleteDataset(args[0],args[1]);
		csf.summarizeTheWholeDatasetResults();
	}
		
}
