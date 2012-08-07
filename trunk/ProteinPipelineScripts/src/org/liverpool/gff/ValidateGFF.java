/**
 * The code to process a GFF3 file for creating FASTA and for creating a new GFF3 with mapped peptides added.
 * The code works in two different modes which is listed in the main().
 * 
 * The code is pretty strict in parsing the GFF3 format, as it demands the GFF3 to have certain fields -
 *  - ## FASTA
 *  - the third column in GFF3 denoting type must be equal to string "CDS" denoting coding region
 *  - The ninth column in GFF3 denoting attribute must have an "ID"
 *  - The FASTA sequences in the GFF3 must have the same identifier as the corresponding ID of the CDS 
 *  
 *   The code does the following ignoring and error reporting -
 *   - if no ##FASTA section present in the file - report error and exit
 *   - if any CDS is missing the "ID" in attribute - report error and exit
 *   - if extra fasta sequences present than the ones with GFF3 entry - ignore extra sequences, continue
 *   - if extra CDS present in GFF3 entries, but no corresponding fasta seq - ignore those CDS, continue
 */

package org.liverpool.gff;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


public class ValidateGFF {
	
	HashMap<String, ArrayList<CDS_Information>> cdsRecords;
	String nameOfTempFastaFile;
	String extOfTempFastaFile;
	File temp;
	
	public ValidateGFF(){
		cdsRecords = new HashMap<String, ArrayList<CDS_Information>>();
		nameOfTempFastaFile = "TempFastaFromGFF";
		extOfTempFastaFile = "fasta";
	}
	/**
	 * Processes the GFF file and creates a temp FASTA file containing the sequences from the ##FASTA section
	 * of the GFF  
	 * @param gffFile
	 * @return Name of the temp Fasta file created
	 * @throws Exception
	 */
	public String processGffFileAndCreateFasta(String gffFile) throws Exception{
		
		try{
			BufferedReader in =  new BufferedReader(new FileReader(gffFile));
			String line;
			boolean fastaRegionFlag = false;
			
		    // Create temp file for storing the Fasta Sequences in the GFF file.
			temp = File.createTempFile(nameOfTempFastaFile, extOfTempFastaFile);
		    //temp.deleteOnExit();		   
		    
		    BufferedWriter temp_out = new BufferedWriter(new FileWriter(temp));
			
		    // Variables needed during processing of ##FASTA file
		    String accn = new String();
			String seq = new String();
			int fastaCount = 0;
			
			while((line = in.readLine()) != null){
				
				//System.out.println(line);
				
				if(line.startsWith("##")){
					if(line.equals("##FASTA")){
						fastaRegionFlag = true;
					}
				}
				
				if(fastaRegionFlag == false){
					
					String [] records = line.split("\t",-1);
					if(records.length != 9){
						System.out.println("Skipping... - " + line);
						continue;
					}
					
					String seqId     	= records[0];
					String source 	 	= records[1]; 
					String type 	 	= records[2];
					String start 	 	= records[3];
					String end 		 	= records[4];
					String score 		= records[5];
					String strand 		= records[6];
					String phase 		= records[7];
					String attribute 	= records[8];
					
					if(type.trim().matches("CDS")){
						long startPos;
						long endPos;
						try{
							startPos = Long.parseLong(start);
							endPos = Long.parseLong(end);
							//A simple check...
							//if(endPos < startPos){
							//	System.out.println("End position = " + endPos + " greater than start position " + startPos+ " in Gff file");
							//	throw new Exception();
							//}
						}catch(NumberFormatException e){
							String exMessage = "ProteoAnnotator : Problem in processing the start and end fields of Line - \n" + line + "\n...exiting";
							System.out.println(exMessage);
							throw new Exception(exMessage);
						}
						
						CDS_Information cdsObj = new CDS_Information(seqId, source, startPos, endPos, strand, phase, attribute);
						String cdsId = parseAttributeFieldToExtractId(attribute); 
						
						ArrayList<CDS_Information> cdsColl = cdsRecords.get(cdsId);
						
						if(cdsColl == null){
							cdsColl = new ArrayList<CDS_Information>();
						}
						
						cdsColl.add(cdsObj);
						
						if(cdsId != null)
							this.cdsRecords.put(cdsId, cdsColl);
					}	
					
				}else{
					
					// Now we are in the FASTA section, so we should have all the cds information
					
					if(cdsRecords.size() == 0){
						String exMessage = "No CDS field found.....exiting";
						System.out.println(exMessage);
						throw new Exception(exMessage);
					}
					
					// If all well, then process fasta - skip the line having ##FASTA 
					if(line.equals("##FASTA")){
						continue;
					}
				
					// Write all other lines
					if(line.contains(">")){
						boolean cdsAccnFound = verifyNeededAccnOrNot(accn);
						
						// Write to file only if it is a cds Accession
						if(cdsAccnFound){
							temp_out.write( "\n" + accn + "\n" + seq);
							fastaCount++;
						}
						accn = line;
						seq = "";
					}else{
						seq = seq.concat(line);
					}
					
				}
				
			} // end of while
			
			//System.out.println("Out of while loop");
			
			// Signal error if no ##FASTA is encountered...
			if(fastaRegionFlag == false){
				String exMessage = "No ##FASTA directive encountered.....Quitting";
				System.out.println(exMessage);
				throw new Exception(exMessage);
			}
			
			// Write the last fasta sequence in the file
			if(line == null){
				boolean cdsAccnFound = verifyNeededAccnOrNot(accn);
				if(cdsAccnFound){
					temp_out.write( "\n" + accn + "\n" + seq);
					fastaCount++;
					}
			}			
			
			System.out.println("Total CDS entries found - " + cdsRecords.size() );
			
			in.close();
			temp_out.close();
		
			// return the name of the file with fasta sequences 
			return temp.getAbsolutePath();
			
		}catch(Exception e){
			// All the exceptions caught during making of a FASTA file are reported here and the program exists
			System.out.println("ProteoAnnotator : Exception encountered in the GFF - " + e.getMessage());
			System.exit(0);
			throw e;
		}
		
	}
	
	/**
	 * 
	 * @param attribute
	 * @return
	 */
	String parseAttributeFieldToExtractId (String attribute) throws Exception{
		String id = null;
		
		String [] splitOnColon = attribute.split(";",-1);
		if(splitOnColon.length == 0){
			String [] keyVal = attribute.split("=",-1);
			if(keyVal.length == 0){
				String exMessage = "No ID found for the CDS entry at attribute : " + attribute;
				System.out.println(exMessage);
				throw new Exception(exMessage);
			}else if (!keyVal[0].equals("ID") ){
				String exMessage = "No ID found for the CDS entry at attribute : " + attribute;
				System.out.println(exMessage);
				throw new Exception(exMessage);
			}else{
				id  = new String(keyVal[1]);
			}
		}else{
			boolean found = false;
			for(int i = 0 ; i< splitOnColon.length; i++){
				
				String subAttr = splitOnColon[i];
				String [] keyVal = subAttr.split("=",-1);
				if(keyVal.length == 0){
					System.out.println("No ID found for the CDS entry");
				}else if (!keyVal[0].equals("ID")){
					System.out.println("No ID found for the CDS entry");
				}else{
					id = new String(keyVal[1]);
					found = true;
				}
				if(found)
					break;
			}
			
			if(!found){
				String exMessage = "No ID found for the CDS entry at attribute : " + attribute;
				System.out.println(exMessage);
				throw new Exception(exMessage);
			}
			
		}
		
		return id;
	}
	
    /**
	 * 
	 * @param accnToCheck
	 * @return
	 */
    boolean verifyNeededAccnOrNot(String accnToCheck){
    	boolean found = false;
    	
    	// Remove the ">" from accnToCheck
    	if(accnToCheck.contains(">"))
    		accnToCheck = accnToCheck.replace(">", "").trim(); 
    	
    	found = cdsRecords.containsKey(accnToCheck);
    	
    	return found;
    }
    
    
    /**
     * 
     * @param inputGffFile
     * @param outputGffFile
     * @param proteinHits
     */
    public void writingTheGFFfile(String inputGffFile, String outputGffFile,ArrayList<ProteinResults> proteinHits)
    																		throws Exception{
		try{
			BufferedReader in =  new BufferedReader(new FileReader(inputGffFile));
			String line;
			boolean fastaRegionFlag = false;
			
			FileWriter fstream = new FileWriter(outputGffFile);
			BufferedWriter out = new BufferedWriter(fstream);
			
		    // Variables needed during processing of ##FASTA file
		    String accn = new String();
			String seq = new String();
			int fastaCount = 0;
			
			while((line = in.readLine()) != null){
				
				if(line.startsWith("##")){
					if(line.equals("##FASTA")){
						fastaRegionFlag = true;
					}
				}
				
				if(fastaRegionFlag == false){
					
					String [] records = line.split("\t",-1);
					
					out.write(line + "\n");
					
					if(records.length != 9){
						System.out.println("Skipping... - " + line);
						continue;
					}
					
					String seqId     	= records[0];
					String source 	 	= records[1]; 
					String type 	 	= records[2];
					String start 	 	= records[3];
					String end 		 	= records[4];
					String score 		= records[5];
					String strand 		= records[6];
					String phase 		= records[7];
					String attribute 	= records[8];
					
					if(type.trim().matches("CDS")){
						long startPos;
						long endPos;
						try{
							startPos = Long.parseLong(start);
							endPos = Long.parseLong(end);
							
							//A simple check...
							//if(endPos < startPos){
							//	System.out.println("End position = " + endPos + " greater than start position " + startPos+ " in Gff file");
							//	throw new Exception();
							//}
						}catch(NumberFormatException e){
							String exMessage = "ProteoAnnotator : Problem in processing the start and end fields of Line - \n" + line + "\n...exiting";
							System.out.println(exMessage);
							throw new Exception(exMessage);
						}
						
						CDS_Information cdsObj = new CDS_Information(seqId, source, startPos, endPos, strand, phase, attribute);
						String cdsId = parseAttributeFieldToExtractId(attribute); 
						
						ArrayList<CDS_Information> cdsColl = cdsRecords.get(cdsId);
						
						if(cdsColl == null){
							cdsColl = new ArrayList<CDS_Information>();
						}

						cdsColl.add(cdsObj);
						this.cdsRecords.put(cdsId, cdsColl);
					}	
					
				}
				
				// Come out when ##FASTA encountered
				if(fastaRegionFlag)
					break;
				
			} // end of while
			
			// Exit if no CDS found
			if(cdsRecords.size() == 0){
				String exMessage = "No CDS field found.....exiting";
				System.out.println(exMessage);
				throw new Exception(exMessage);
			}
			
			in.close();
			
    	   // Do the mapping and writing  part here.....
			for(int i = 0; i < proteinHits.size(); i++){
				ProteinResults pr = proteinHits.get(i);
				String mappedGffEntry = mapToGff(pr);
				if(mappedGffEntry != null)
					out.write(mappedGffEntry);
				
			}
			out.close();
			
		}catch(IOException e){
			System.out.println("ProteoAnnotator : IO Excpetion : Unable to create/read file");
			System.exit(0);
			throw e;
		}catch(Exception e){
			System.out.println("ProteoAnnotator : Exception encountered in the GFF - " + e.getMessage());
			System.exit(0);
			throw e;
		}
    }
    
    /*
     * 
     */
    String mapToGff(ProteinResults pr){
    	
    		String gffMapping = new String();
    	
    		// Get data from the protein object
    		String accession = pr.getAccession();
    		boolean decoyOrNot = pr.getDecoyOrNot();
    		long start = pr.getstart();
    		long end = pr.getEnd();
    	
    		// return if no key found
    		if(!cdsRecords.containsKey(accession))
    			return null;
    	
    		// otherwise continue...
    		ArrayList<CDS_Information> cdsColl = cdsRecords.get(accession);
    		
    		// sort the cdsColl according to the start position...
    		cdsColl = sortCDSAccordingToStartPosition(cdsColl);
    	
    		long cdsStartLocation = cdsColl.get(0).getStart();
    	
    		long mappedStartLocation = cdsStartLocation + start * 3;
    		long mappedEndLocation   = cdsStartLocation + end * 3;
    		
    		try{
    		//...find the cds this range falls into...
    		gffMapping = determineTheLocationOfSeqOnCds(accession, cdsColl, mappedStartLocation,mappedEndLocation);
    		}catch(Exception e){
    			//e.printStackTrace();
        	}
    		return gffMapping;
    }
    
    /**
     * Sort the CDS record, with the ascending order of the start position.
     * 
     * @param cdsColl
     * @return
     */
    ArrayList<CDS_Information> sortCDSAccordingToStartPosition(ArrayList<CDS_Information> cdsCollection){
    	
    	ArrayList<CDS_Information> cdsColl = new ArrayList<CDS_Information>(cdsCollection); 
    		
    	for(int i =  0; i < cdsColl.size() - 1; i++){
    		CDS_Information cds_i = cdsColl.get(i);
    		for(int j = i+1; j < cdsColl.size(); j++){
    			CDS_Information cds_j = cdsColl.get(j);
    			if(cds_i.getStart() > cds_j.getStart()){
    				CDS_Information temp = cds_i;
    				cdsColl.set(i, cds_j);
    				cdsColl.set(j, temp);
    			}
    		}
    	}
    	return cdsColl;
    }
    
    /**
     * 
     */
    String  determineTheLocationOfSeqOnCds(String accession, ArrayList<CDS_Information> cdsColl, long mappedStartLocation, long mappedEndLocation){
    	
    	String gffEntry = new String();
    	
    	// sort the cdsColl according to the start position...
    	cdsColl = sortCDSAccordingToStartPosition(cdsColl);
    	
    	
    	ArrayList<CDS_Information> matchedIndices = new ArrayList<CDS_Information>();
    	boolean foundEnd = false;
    	boolean shiftForEndPosition = false;
    	
    	for(int i = 0 ; i < cdsColl.size(); i++){
    		long cds_start = cdsColl.get(i).getStart();
    		long cds_end = cdsColl.get(i).getEnd();
    		
    		if(mappedStartLocation >= cds_start && mappedStartLocation <= cds_end){
    			// Case - Contained within the CDS
    			if(mappedEndLocation <= cds_end){
    				foundEnd = true;
        			matchedIndices.add(cdsColl.get(i));
        			break;
        			
        		}else{
        			for(int j = i+1; (j < cdsColl.size()) && (foundEnd == false); j++){
        				long cds_start_2 = cdsColl.get(j).getStart();
        	    		long cds_end_2 = cdsColl.get(j).getEnd();
        	    		// Case - Contained across CDS
        	    		if(mappedEndLocation >= cds_start_2 && mappedEndLocation <= cds_end_2){
        	    			foundEnd = true;
        	    			for (int l = i; l <= j ; l++)
        	    				matchedIndices.add(cdsColl.get(l));
        	    			break;
        	    		}
        	    		
        	    		// Case - falls somewhere after one CDS, but before the next or the last CDS
        	    		// This means that the end needs to be "shifted" to the next CDS
        	    		if(mappedEndLocation < cds_start_2){ 
        	    			foundEnd = true;
        	    			shiftForEndPosition = true;
        	    			for (int l = i; l <= j ; l++) 
        	    				matchedIndices.add(cdsColl.get(l));
        	    		}	
        			}
        			
        			// Case - Start present inside a CDS, but end present Outside all CDS, so collect all CDS from index i
        			if(!foundEnd){
        				for (int l = i; l <= cdsColl.size() - 1; l++)
        					matchedIndices.add(cdsColl.get(l));
        			}
        			
        		}// end of else	
    		}
    	}// end of for
    	

		String feature = "peptide";
		String score = ".";
		
    	// Case - start and end present Outside CDS
    	if(matchedIndices.size() == 0){
    		String attr = "ID=pep_"+ accession + "description=peptide;Derives_from=" + accession;
    		// Just use the information from the first CDS entry with mapped start and end locations
    		gffEntry = cdsColl.get(0).getSeqID() + "\t" + cdsColl.get(0).getSource() + "\t" + feature + "\t" + mappedStartLocation 
    		+ "\t" + mappedEndLocation + "\t" + score + "\t" + cdsColl.get(0).getSePhase() + "\t" + attr + "\n";
    		
    		try{
    			if(mappedEndLocation < mappedStartLocation){
    				System.out.println("End location = " + mappedEndLocation + " smaller than start location = " + mappedStartLocation);
    				throw new Exception();
    			}
    		}catch(Exception e){
    			//e.printStackTrace();
    		}
    		
    		return gffEntry;
    	}
    	// otherwise, do usual mapping..
    	for(int i = 0 ; i < matchedIndices.size(); i++){
    		
    		String seqId = matchedIndices.get(i).getSeqID();
    		String source = matchedIndices.get(i).getSource();
    		long start = matchedIndices.get(i).getStart();
    		long end = matchedIndices.get(i).getEnd();
    		String phase = matchedIndices.get(i).getSePhase();
    		//String attr = matchedIndices.get(i).getAttribute();
    		String attr = "ID=pep_"+ accession + "_"+ i + "description=peptide;Derives_from=" + accession;
    		
    		// first index
    		if(i == 0){
    			start = mappedStartLocation;
    		}
    		
    		// last index
    		long shiftNeeded = 0;
    		if(i == matchedIndices.size() - 1){
    			
    			if(shiftForEndPosition == true){
        			shiftNeeded = mappedEndLocation - matchedIndices.get(i-1).getEnd() ; 
        			end = start + shiftNeeded;
        		}
    			else
    				end = mappedEndLocation;
    		}
    		
    		
    		gffEntry = gffEntry + seqId + "\t" + source + "\t" + feature + "\t" + start + "\t" + end 
    									+ "\t" + score + "\t" + phase + "\t" + attr + "\n";
    		
    		try{
    			if(end < start){
    				System.out.println(" - End location = " + end + " smaller than start location = " + start);
    				throw new Exception();
    			}
    		}catch(Exception e){
    			//e.printStackTrace();
    		}
    	}
    	
    	return gffEntry;
    	
    }
    
    /**
     * Test function 1
     * @param args
     * @throws Exception
     */
    public void createFastaFromGFF(String gffFile) throws Exception {
    	
		System.out.println("Fasta File produced -" + processGffFileAndCreateFasta(gffFile));		
		System.out.println("Press enter to exit...");
		System.in.read();
		
    }
    
    /**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String [] args) throws Exception{
		
		ValidateGFF vgf = new ValidateGFF();
		
		String gffFile = args[0];
		
		// GFF Reading part here...
		if(args.length == 1){	
			System.out.println("Creating Fasta file from the GFF.....");
			 vgf.createFastaFromGFF(gffFile);
			 return;
		}
		
		// GFF writing part here...
		if(args.length == 4){
			String summaryFile     		   = args[1];
			String decoyIdentifier 		   = args[2];
			String  outputGffFile          = args[3];
			ReadProteinRecordFromSummaryFile rip = new ReadProteinRecordFromSummaryFile(summaryFile,decoyIdentifier);
			ArrayList<ProteinResults> prList = rip.readProteinResultsFromFile();
			
			vgf.writingTheGFFfile(gffFile, outputGffFile, prList);
			
		}else{
	
			System.out.println("Usage For creating Fasta file : gffFile");
	
			System.out.println("Usage For writing Gff : " +
					"gffFile wholeDatabaseSummaryFile decoy-identifier outputGffFile");
			return;
		}
			
		

	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
/*	public static void main(String [] args) throws Exception{
		
		String gffFile = args[0];
		
		ValidateGFF vgf = new ValidateGFF();
		
		// Fasta creation call here
		//vgf.createFastaFromGFF(gffFile);
		
		
		// GFF writing part here...
		String summaryFile     		   = args[1];
		
		String decoyIdentifier 		   = args[2];
		String  outputGffFile          = args[3];
		ReadProteinRecordFromSummaryFile rip = new ReadProteinRecordFromSummaryFile(summaryFile,decoyIdentifier);
		ArrayList<ProteinResults> prList = rip.readProteinResultsFromFile();
		
		vgf.writingTheGFFfile(gffFile, outputGffFile, prList);

	}
	*/
}