/**
 * This may require some strategic changes.....
 * Read the whole GFF line by line
 * Find unique CDSs by looking at the ID in the description column
 * Extract FASTA sequences for all the CDS - Give error in ##FASTA directive is missing
 * Use the fasta for DB preparation
 * 
 * Pipeline will inform the location of peptide on CDS
 * We know which CDS falls where on which gene
 * So we know where the peptide falls on the genome
 * 
 * mature_peptide/polypeptide are accepted types in GFF
 * 
 * 
 * Idea to think about -
 * - Escape ## directives till ##FASTA is found
 * - Parse the remaining lines for \t and 9 columns
 * - Remember the CDS - and the whole information about that
 * 		- will also need to parse the attribute column for CDS
 * - Read till ##FASTA is found
 * - Find the CDS sequences in Fasta file and write them in another file
 * - Write the CDS information in a separate file to query later while doing the peptide mapping
 */

package org.liverpool.gff;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
	public String processGffFile(String gffFile) throws Exception{
		
		try{
			BufferedReader in =  new BufferedReader(new FileReader(gffFile));
			String line;
			boolean fastaRegionFlag = false;
			
		    // Create temp file for storing the Fasta Sequences in the GFF file.
			temp = File.createTempFile(nameOfTempFastaFile, extOfTempFastaFile);
		    temp.deleteOnExit();		    
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
	 * @param args
	 * @throws Exception
	 */
	public static void main(String [] args) throws Exception{
		
		String gffFile = args[0];
		ValidateGFF vgf = new ValidateGFF();
		System.out.println("Fasta File produced -" + vgf.processGffFile(gffFile));
		
		System.out.println("Press enter to exit...");
		System.in.read();
	}
	
}