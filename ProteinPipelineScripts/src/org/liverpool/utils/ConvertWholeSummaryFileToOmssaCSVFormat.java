package org.liverpool.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This program is written for converting WholeSummary_ pipeline output files to a OMSSA CVS output format.
 * The output file will go through other routine for protein inference.
 *
 *   The OMSSA headers are -
 *   0. Spectrum number	 
 *   1. Filename/id	 
 *   2. Peptide	 
 *   3. E-value	 
 *   4. Mass	 
 *   5. gi	 
 *   6. Accession	 
 *   7. Start	 
 *   8. Stop	 
 *   9. Defline	 
 *   10. Mods	 
 *   11. Charge	 
 *   12. Theo Mass	 
 *   13. P-value	 
 *   14. NIST score
 * @author riteshk
 *
 */

public class ConvertWholeSummaryFileToOmssaCSVFormat {
	
	static Logger log = Logger.getLogger(ConvertWholeSummaryFileToOmssaCSVFormat.class);
	
	// Set according to the columns in the summary file
	final int protAccn_column 		= 0;
	final int protScore_column 		= 1;
	final int specId_column 		= 2;
	final int pepSeq_column 		= 3;
	final int fdr_column 			= 4;
	final int exp_mass_column		= 5;
	final int group_id_column		= 6;
	final int start_column 			= 7;
	final int end_column 			= 8;
	final int simple_fdr_column	    = 9; 
	final int mod_column			= 10;
	final int charge_column 		= 11;	
	final int theo_mass_column      = 12;
	final int searchEngine_column 	= 13;
    final int spectrum_location_column 	= 14;
	
	String wholeSummaryFile;
	double fdrThreshold;
	String delimiter;
	String decoyString; 
	String omssaLikeOutputFile;
	
	/**
	 * 
	 * @param pipelineSummaryFile - Summary file for whole dataset
	 * @param fdrThreshold 		  - FDR to be used for analysis
	 * @param delimiter			  - \t or , or \w
	 * @param decoyString		  - Decoy identifier string	
	 * @param omssaLikeOutputFile - Omssa like output file 
	 */
	public ConvertWholeSummaryFileToOmssaCSVFormat(String pipelineSummaryFile, double fdrThreshold, String delimiter,String decoyString, String omssaLikeOutputFile) {
		wholeSummaryFile = new String(pipelineSummaryFile);
		this.fdrThreshold = fdrThreshold;
		this.delimiter = delimiter;
		this.decoyString = decoyString;
		this.omssaLikeOutputFile = new String(omssaLikeOutputFile);
	}
	
	/**
	 * 
	 */
	public void createOmssaLikeFile(){
		
		Scanner scanner ;
		
		try{
			
            BufferedWriter out_prot = new BufferedWriter(new FileWriter(this.omssaLikeOutputFile));
            out_prot.write("Spectrum number,Filename/id,Peptide,FDRScore,Mass,gi,Accession,Start,Stop,Defline,Mods,Charge,Theo Mass,Simple FDRScore,NIST score,spectrum location\n");

			HashMap<String,Integer> specTitleToIDMap = new HashMap();
                        int specCounter = 0;
			scanner = new Scanner(new FileReader(new File(wholeSummaryFile)));
			
			String prevProtein = new String();
			double prevProteinScore = 0.0;
			
			int counter = 1;
			while(scanner.hasNextLine()){
				 String line = scanner.nextLine();
				 if(line.isEmpty())
					 continue;
				 String [] values = line.split(this.delimiter);
				 
				 try{
				 if(values.length < 14)
					 throw new Exception(" Error : Less than 14 columns found in the line \n " + line);
				 }catch(Exception e){
					 String errMsg =  e.getMessage() + "\n Please check the summary file provided";
					 System.out.println(errMsg);
					 log.fatal(errMsg);
					 System.exit(1);
				 }
				 
				 String protAccn;
				 if(values[protAccn_column].trim().isEmpty())
					 protAccn = prevProtein;
				 else protAccn = values[protAccn_column].trim();
				 
				 double protScore;
				 if(values[protScore_column].trim().isEmpty())
					 protScore = prevProteinScore;
				 else protScore = Double.parseDouble(values[protScore_column].trim());
				 /*
				 String specID = values[specId_column].trim();
                                 if(!specTitleToIDMap.containsKey(specID)){
                                     specTitleToIDMap.put(specID,specCounter);
                                     specCounter++;
                                 }
                                 * 
                                 */
                                 
                String[] temp = values[specId_column].trim().split("=");
                String specID = temp[1];
                                 
                                 
				String pepSeq = values[pepSeq_column].trim();
				int start = Integer.parseInt(values[start_column].trim());
				int end = Integer.parseInt(values[end_column].trim());
				double fdrScore = Double.parseDouble(values[fdr_column].trim());
				 
				double expMass = Double.parseDouble(values[exp_mass_column].trim());
				String groupID = values[group_id_column].trim();
				double theoMass = Double.parseDouble(values[theo_mass_column].trim());
				int charge = Integer.parseInt(values[charge_column].trim());
				String mods = values[mod_column].trim();
                String spectraLocation = values[spectrum_location_column].trim();
                
                /*
                if(mods != ""){
                        String[] modSplit = mods.split("##");
                        String newModString = "\"";
                        for(String newMod : modSplit){
                                        
                               newMod = newMod.trim();
                               if(!newMod.equals("") && newMod != null){
                                          
	                              //System.out.println("NewMod:" + newMod + "]");
	                              newMod = newMod.replace("15.99492_M","Oxidation (M)");
	                              newMod = newMod.replace("57.02147_C","Carbamidomethyl (C)");
	                              newMod = newMod.replace("-999_M","Oxidation (M)");
	                              newMod = newMod.replace("-999_Q","Pyro-glu of Q (N-term)");
	                              newMod = newMod.replace("-999_E","Pyro-glu of E (N-term)");
	                       
	                              newMod = newMod.replace("-17.02655_Q","Pyro-glu of Q (N-term)");
	                              newMod = newMod.replace("-18.01056_E","Pyro-glu of E (N-term)");
	                              newMod = newMod.replace("-17.02655_C","Ammonia-loss (C)");
	                              newMod = newMod.replace("-17.02655_N","Ammonia-loss (N)");
	                              //Pattern p = Pattern.compile("42.01057_[A-Z.:1");
	                              if(newMod.contains("42.01057")){
	                                       newMod = "Acetyl (N-term):0";                                                
	                              }
	                              //newMod = newMod.replace("42.01057_\\w:1","Acetyl (N-term):0");                                        
	                              newModString += newMod + ",";
                               }                                     
                          }

                          //Remove last comma
                          if(newModString.lastIndexOf(",")==newModString.length()-1){
                                newModString = newModString.substring(0,newModString.length()-1);
                          }
                          
                          mods = newModString + "\"";
                   }
                   */
                                       
				   double simple_fdr = Double.parseDouble(values[simple_fdr_column].trim());
				 
                   // Only include if FDR score is greater than threshold
                   if (fdrScore <= this.fdrThreshold){
                                // Skip if protein is a decoy
                                //if(protAccn.contains(this.decoyString))
                                       //continue;


                                 // else write in omssa format...
                                 int gi = 0;
                                 int nistScore = 0;
                                 String omssaString = specID + ","+spectraLocation+" index=" + specID + "," + pepSeq + "," + fdrScore + "," 
                                                                   + expMass + "," + gi + "," + protAccn + "," + start + "," 
                                                                   + end + "," + protAccn + "," + mods + "," + charge + "," 
                                                                   + theoMass + "," + simple_fdr + "," + nistScore + "," + spectraLocation + "\n";

                                    out_prot.write(omssaString);

                                    counter++  ;
                      }
                      prevProtein = protAccn;
                      prevProteinScore = protScore;
                                 
			}	
			
			scanner.close();
			
			out_prot.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String [] args) throws Exception{
	   
	   String logProperties = "resources/log4j.properties";
	   PropertyConfigurator.configure(logProperties);
		
       if(args.length!=4){
           System.out.println("Arguments needed: inputFile.txt outputFile.csv fdrThreshold DecoyString");
           System.exit(1);
        }
            
		String pipelineSummaryFile = args[0]; 
        String omssaLikeOutputFile = args[1]; 
		double fdrThreshold = Double.parseDouble(args[2]); 
		String delimiter = "\t";
		String decoyString = args[3];
		
		ConvertWholeSummaryFileToOmssaCSVFormat cv = new ConvertWholeSummaryFileToOmssaCSVFormat(pipelineSummaryFile, fdrThreshold, delimiter, decoyString, omssaLikeOutputFile);
		cv.createOmssaLikeFile();
		
	}

}
