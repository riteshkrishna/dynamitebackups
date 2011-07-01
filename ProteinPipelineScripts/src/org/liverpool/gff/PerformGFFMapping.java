package org.liverpool.gff;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class PerformGFFMapping {
		
		HashMap<String, ArrayList<CDS_Information>> cdsRecords;
		String nameOfTempFastaFile;
		String extOfTempFastaFile;
		File temp;
		
		public PerformGFFMapping(){
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
								if(endPos < startPos){
									System.out.println("End position = " + endPos + " greater than start position " + startPos+ " in Gff file");
									throw new Exception();
								}
							}catch(NumberFormatException e){
								System.out.println("Problem in processing the start and end fields of Line - \n" + line + "\n...exiting");
								e.printStackTrace();
								throw e;
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
						
					}else{
						
						// Now we are in the FASTA section, so we should have all the cds information
						System.out.println("Entering the ##FASTA section...total CDS found - " + cdsRecords.size() );
						
						if(cdsRecords.size() == 0){
							System.out.println("No CDS field found.....exiting");
							throw new Exception();
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
				
				System.out.println("Out of while loop");
				
				// Signal error if no ##FASTA is encountered...
				if(fastaRegionFlag == false){
					System.out.println("No ##FASTA directive encountered.....Quitting");
					throw new Exception();
				}
				
				// Write the last fasta sequence in the file
				if(line == null){
					boolean cdsAccnFound = verifyNeededAccnOrNot(accn);
					if(cdsAccnFound){
						temp_out.write( "\n" + accn + "\n" + seq);
						fastaCount++;
						}
				}
				
				in.close();
				temp_out.close();
			
				// return the name of the file with fasta sequences 
				return temp.getAbsolutePath();
				
			}catch(Exception e){
				throw e;
			}
			
		}
		
		/**
		 * 
		 * @param attribute
		 * @return
		 */
		String parseAttributeFieldToExtractId (String attribute) throws Exception{
			String id = new String();
			
			String [] splitOnColon = attribute.split(";",-1);
			if(splitOnColon.length == 0){
				String [] keyVal = attribute.split("=",-1);
				if(keyVal.length == 0){
					System.out.println("No ID found for the CDS entry");
				}else if (keyVal[0].compareToIgnoreCase("ID")  < 0){
					System.out.println("No ID found for the CDS entry");
				}else{
					id  = keyVal[1];
				}
			}else{
				boolean found = false;
				for(int i = 0 ; i< splitOnColon.length; i++){
					
					String subAttr = splitOnColon[i];
					String [] keyVal = subAttr.split("=",-1);
					if(keyVal.length == 0){
						System.out.println("No ID found for the CDS entry");
					}else if (keyVal[0].compareToIgnoreCase("ID")  < 0){
						System.out.println("No ID found for the CDS entry");
					}else{
						id = keyVal[1];
						found = true;
					}
					if(found)
						break;
				}
				
				if(!found){
					System.out.println("No ID found for the CDS entry");
					throw new Exception();
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
								if(endPos < startPos){
									System.out.println("End position = " + endPos + " greater than start position " + startPos+ " in Gff file");
									throw new Exception();
								}
							}catch(NumberFormatException e){
								System.out.println("Problem in processing the start and end fields of Line - \n" + line + "\n...exiting");
								e.printStackTrace();
								throw e;
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
					System.out.println("No CDS field found.....exiting");
					throw new Exception();
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
			
	    }
	    
	    /*
	     * 
	     */
	    String mapToGff(ProteinResults pr){
	    	String gffMapping = new String();
	    	
	    	// Get accession from the protein object
	    	String accession = pr.getAccession();
	    	
	    	// return if no key found
	    	if(!cdsRecords.containsKey(accession))
	    		return null;
	    	
	    	//...find the cds this range falls into...
	    	ArrayList<CDS_Information> cdsColl = cdsRecords.get(accession);
	    			
	    	gffMapping = determineTheLocationOfSeqOnCds(pr, cdsColl);
	    	
	    	return gffMapping;
	    }
	    
	    /**
	     * 
	     * @param pr
	     * @param cdsColl
	     * @return
	     */
	    String determineTheLocationOfSeqOnCds(ProteinResults pr, ArrayList<CDS_Information> cdsColl){
	    	String gffEntry = new String();
	    	
	    	// Get locations from the protein object
	    	String accession = pr.getAccession();
	    	long start = pr.getstart();
	    	long end = pr.getEnd();
	  
	    	// sort the CDS collection according to the strand
	    	ArrayList<CDS_Information> sortedCDS = sortCDSAccordingToStartPosition(cdsColl);
	
	    	long mapped_start = getMappedCordinates(start, sortedCDS);
	    	long mapped_end   = getMappedCordinates(end, sortedCDS);
	    	
	    	gffEntry = createGffEntryString(accession,mapped_start,mapped_end, sortedCDS);
	    	return gffEntry;
	    }
	    
	    /**
	     * 
	     * @param accession
	     * @param mapped_start
	     * @param mapped_end
	     * @param sortedCDS
	     * @return
	     */
	    String createGffEntryString(String accession,long mapped_start,long mapped_end, ArrayList<CDS_Information> sortedCDS){
	    	String gffEntry = new String();
	    	
	    	String strand = sortedCDS.get(0).getStrand();
	    	
	    	long [] startPos = new long[sortedCDS.size()];
    		long [] endPos 	= new long[sortedCDS.size()];
    		for(int i=0; i < sortedCDS.size(); i++){
    			startPos[i] = sortedCDS.get(i).getStart();
    			endPos[i] = sortedCDS.get(i).getEnd();
    		}
    		
    		int start_idx = -1;
    		int end_idx = -1;
    		
	    	if(strand.contains("+")){
	    		// cds_index_start for start and cds_index_end for end	
	    		for(int i=0;i < startPos.length; i++){
	    			if(mapped_start <= startPos[i]){
	    				start_idx = i;
	    				break;
	    			}
	    		}
	    		
	    		// cds_index_start for start and cds_index_end for end	
	    		for(int i=0;i < endPos.length; i++){
	    			if(mapped_end <= endPos[i]){
	    				end_idx = i;
	    				break;
	    			}
	    		}
	    	}else{
	    		// cds_index_end for start and cds_index_start for end	
	    		for(int i=0;i < startPos.length; i++){
	    			if(mapped_end <= startPos[i]){
	    				start_idx = i;
	    				break;
	    			}
	    		}
	    			
	    		for(int i=0;i < endPos.length; i++){
	    			if(mapped_start <= endPos[i]){
	    				end_idx = i;
	    				break;
	    			}
	    		}	
	    	}
	    	
	    	// form GFF
	    	String feature = "peptide";
			String score = ".";
	    	String attr = "ID=pep_"+ accession + "description=peptide;Derives_from=" + accession;
	    	
	    	// Swap for -ve strand
    		if(!strand.contains("+")){
    			long temp = mapped_start;
    			mapped_start = mapped_end;
    			mapped_end = temp;
    			
    		}
	    	
	    	if(start_idx != end_idx){
	    		
	    		// First line of the block 
	    		gffEntry = sortedCDS.get(start_idx).getSeqID() + "\t" + sortedCDS.get(start_idx).getSource() + "\t" +
				feature + "\t" + mapped_start + "\t" + sortedCDS.get(start_idx).getEnd() + "\t" + score + "\t" + 
				sortedCDS.get(start_idx).getSePhase() + "\t" + attr + "\n";
	    		
	    		// between the blocks
	    		for(int i = start_idx + 1; i < end_idx ; i++){
	    			gffEntry = gffEntry +  sortedCDS.get(i).getSeqID() + "\t" + sortedCDS.get(i).getSource() + "\t" +
					feature + "\t" + sortedCDS.get(i).getStart() + "\t" + sortedCDS.get(i).getEnd() + "\t" + score + "\t" + 
					sortedCDS.get(i).getSePhase() + "\t" + attr + "\n";	
	    		}
	    		
	    		// last line of the block
	    		gffEntry = gffEntry + sortedCDS.get(end_idx).getSeqID() + "\t" + sortedCDS.get(end_idx).getSource() + "\t" +
				feature + "\t" + sortedCDS.get(end_idx).getStart() + "\t" + mapped_end + "\t" + score + "\t" + 
				sortedCDS.get(end_idx).getSePhase() + "\t" + attr + "\n";
	    		
	    	}else{
	    		
	    		gffEntry = sortedCDS.get(start_idx).getSeqID() + "\t" + sortedCDS.get(start_idx).getSource() + "\t" +
	    					feature + "\t" + mapped_start + "\t" + mapped_end + "\t" + score + "\t" + 
	    					sortedCDS.get(start_idx).getSePhase() + "\t" + attr + "\n";
	    	}
	    	
	    		
	    	return gffEntry;
	    }
	    
	    /**
	     * 
	     * @param number
	     * @param cdsColl
	     * @return
	     */
	    long getMappedCordinates(long number, ArrayList<CDS_Information> cdsColl){
	    	long mappedCord = 0;
	    	
	    	// compute the cumm array
	    	long [] cummArray = cumulativeStartPositions(cdsColl);
	    	long number_toMap = (number - 1) * 3;
	    	
	    	int idx = determineTheIndexInCummulativeArray(number_toMap, cummArray);
	    	long shift = cummArray[idx] - number;
	    	
	    	if(cdsColl.get(0).getStrand().contains("+")){
	    		long end_cds = cdsColl.get(idx).getEnd();
	    		mappedCord = end_cds - shift;
	    	}else{
	    		long start_cds = cdsColl.get(idx).getStart();
	    		mappedCord = start_cds + shift;
	    	}
	    	
	    	return mappedCord;
	    }
	    
	    /**
	     * Sort the CDS record, with the ascending or descending order of the start position. If the CDS is on +ve strand
	     * then sort in ascending order, otherwise, sort in descending order.
	     * 
	     * @param cdsColl
	     * @return
	     */
	    ArrayList<CDS_Information> sortCDSAccordingToStartPosition(ArrayList<CDS_Information> cdsCollection){
	    	
	    	ArrayList<CDS_Information> cdsColl = new ArrayList<CDS_Information>(cdsCollection); 
	    	
	    	String strand = cdsCollection.get(0).getStrand();
	    	boolean sortAscending = false;
	    	
	    	if(strand.contains("+"))
	    		sortAscending = true;
	    			
	    	// Ascending sort...
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
	    	
	    	// If we need it in descending order, then reverse the sorted array
	    	if(!sortAscending){
	    		for(int i = cdsColl.size() - 1; i >= 0; i--){
	    			int j = cdsColl.size() - 1 - i;
	    			if(j < i){
	    				CDS_Information temp = cdsColl.get(i);
	    				cdsColl.set(i, cdsColl.get(j));
	    				cdsColl.set(j, temp);
	    			}
	    		}
	    	}
	    	
	    	return cdsColl;
	    	
	    }
	    
	    /**
	     * 
	     * @param cdsCollection
	     * @return
	     */
	    long [] cumulativeStartPositions(ArrayList<CDS_Information> cdsCollection){
	    	long [] cummStartPosition = new long[cdsCollection.size()];
	    	
	    	// compute Diff = end - start
	    	for(int i = 0 ; i < cdsCollection.size() - 1; i++){
	    		cummStartPosition[i] = cdsCollection.get(i).getEnd() - cdsCollection.get(i).getStart();
	    	}
	    	
	    	// Form cumulative array 
	    	for(int i = 1; i < cummStartPosition.length  - 1; i++){
	    		cummStartPosition[i] = cummStartPosition[i] + cummStartPosition[i-1];
	    	}
	    	
	    	return cummStartPosition;
	    }
	    
	    /**
	     * 
	     * @param number
	     * @param cummArray
	     * @return
	     */
	    int determineTheIndexInCummulativeArray(long number, long[] cummArray){
	    	int idx = -1;
	    	
	    	try{
	    		
	    		for(int i = 0; i < cummArray.length; i++){
	    			if(number <= cummArray[i]){
	    				idx = i;
	    				break;
	    			}
	    		}
	    		
	    		if(idx == -1)
	    			throw new Exception("Cummulative index not found.....");
	    		
	    	}catch(Exception e){
	    		System.out.println(e.getMessage());
	    		e.printStackTrace();
	    	}
	    	
	    	return idx;
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
			
			PerformGFFMapping vgf = new PerformGFFMapping();
			
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
		
}