package org.liverpool.utils;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.log4j.Logger;

public class FileLookup {

	static Logger log = Logger.getLogger(FileLookup.class);
	
	/**
	 * X!Tandem has it's own way of naming files, so we need to look for the actual name of the file
	 * produced with the help of the fileNameToLookFor
	 * 
	 * @param dirToStoreTempFile
	 * @param fileNameToLookFor
	 * @return
	 */
	public String retrieveTheFileProducedByTandem(String dirToStoreTempFile,String fileNameToLookFor){
		
		String actualName = null;  
		fileNameToLookFor	= (new File(fileNameToLookFor)).getName().split("\\.")[0];
		
		File dir = new File(dirToStoreTempFile);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		};
		
		File [] xmlFiles = dir.listFiles(filter);
		
		for(int i = 0 ; i < xmlFiles.length ; i++){
			if(xmlFiles[i].getName().contains(fileNameToLookFor) ){
				actualName = xmlFiles[i].getAbsolutePath();
				break;
			}
		}
		
		if(actualName == null){
			log.fatal("Unable to find the file produced by X!Tandem. Looking for Tag - " + fileNameToLookFor);
		}
		return actualName;
	}
	

}
