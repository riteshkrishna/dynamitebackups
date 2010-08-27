package org.liverpool.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.sun.net.httpserver.Headers;

public class ReadUmodTable {

	static Logger log = Logger.getLogger(ReadConfigurationFiles.class);
	int numberOfColumnInModFile = 7;
	
	String umodFileName;
	String umodFileDelimiter;
	ArrayList <String> content;
	ArrayList<Integer> omssaIdentifiers; 
	
	/**
	 * 
	 * @param umodFile
	 * @param umodFileDelimiter
	 */
	public ReadUmodTable(String umodFile,String umodFileDelimiter) {
		umodFileName = umodFile;
		this.umodFileDelimiter = umodFileDelimiter;
		content = new ArrayList<String>();
		omssaIdentifiers = new ArrayList<Integer>();
	}
	
	/**
	 * This will read the content of the file in this.content and return the list of all the omssa identifiers in
	 * the file.
	 * 
	 * @param omssaIdentifierInHeader - The header text representing Omssa identifiers in umod_table.csv
	 * @throws Exception
	 */
	void readUmodContentFromFile(String omssaIdentifierInHeader) throws Exception{
		
		Scanner scanner = new Scanner(new File(umodFileName));	
		
		int omssa_id_headerColumn = -1;
		boolean header = true;
		
		try{
			while(scanner.hasNextLine() ){
				String line = scanner.nextLine();
				
				// Examine the first line - the header, and determine the column with Omssa ID
				if(header){
					if(line == null){
						log.fatal("Problem with the header in umod file. Null found");
						throw new Exception();
					}
					
					String [] headerTitles = line.split(this.umodFileDelimiter);
					for(int i = 0 ; i < headerTitles.length ;i++){
						if(headerTitles[i].contains(omssaIdentifierInHeader)){
							omssa_id_headerColumn = i;
							break;
						}
					}
					
					if(omssa_id_headerColumn == -1){
						log.fatal("Omssa Id header :" + omssaIdentifierInHeader + " not found in the header");
						throw new Exception();
					}
					header = false;
					continue;
				}
				
				content.add(line);
				String [] fields = line.split(this.umodFileDelimiter);
				try{
					this.omssaIdentifiers.add(Integer.parseInt(fields[omssa_id_headerColumn]));
				}catch(ArrayIndexOutOfBoundsException e){
					log.fatal("No legal Omssa-ID found in the UMOD_TABLE.csv");
					e.printStackTrace();
				}
			}
		}finally{
			scanner.close();
		}		
	}
	
	/**
	 * We assume that Omssa identifiers are not repeated in the umod_table.csv. If they are repeated then it means that
	 * they represent the same modification, and the retrieved records will be overwritten.
	 *   
	 * @param omssaIdentifierInHeader
	 * @param omssaIdsToFind
	 * @return
	 */
	public HashMap<Integer,String> getInformationForGivenOmssaIdentifiers(String omssaIdentifierInHeader,
																		ArrayList<Integer> omssaIdsToFind){
		// Read the content of the csv file
		try{
			this.readUmodContentFromFile(omssaIdentifierInHeader);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		HashMap<Integer,String> records = new HashMap<Integer,String>();
		
		try{
			for(int i = 0 ; i < omssaIdsToFind.size(); i++){
				boolean found = false;
				for(int k = 0 ; k < this.omssaIdentifiers.size() ; k++){
					if(omssaIdsToFind.get(i) == this.omssaIdentifiers.get(k)){
						String modInfo = content.get(k);
						records.put(omssaIdsToFind.get(i), modInfo);
						found = true;
						break;
					}
				}
				if(!found){
					log.fatal("Omssa Id :: " + omssaIdsToFind.get(i) + " not found in umod_table.csv");
					throw new Exception();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return records;
	}
	
	/**
	 * The test function
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		String umodFile = "resources/UMOD_TABLE.csv"; // Location of the umod file
		String omssaIdentifierInHeader = "Omssa_ID";  // The column header indicating the omssa values
		String umodFileDelimiter = ",";				  // The delimiter used in the umod file 	
		
		try{
			// An example of the Omssa identifiers we are searching for
			ArrayList<Integer> omssaIdsToFind = new ArrayList<Integer>();
			omssaIdsToFind.add(110);
			omssaIdsToFind.add(3);
			omssaIdsToFind.add(2);
			
			ReadUmodTable rut = new ReadUmodTable(umodFile,umodFileDelimiter);
			// Get the information about those identifiers in a hash
			HashMap<Integer,String> info = rut.getInformationForGivenOmssaIdentifiers(omssaIdentifierInHeader,omssaIdsToFind);
			
			log.info(info.get(110) + "\n" + info.get(3) + "\n" + info.get(2));
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
