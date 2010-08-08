package org.liverpool.utils;

import java.util.Set;

public class ValidateInputFiles {

	/**
	 * Validates whether the keywords described in the input file are allowed keywords or not.
	 * 
	 * @param allowedKeywords - The keywords read from the definition file
	 * @param inputKeys - The keywords read from the input file
	 * @return true, if all the elements of inptKeys are found in allowedKeywords, otherwise false.
	 */

	public boolean validateContentOfInputFileAgainstAllowedKeywords(Set <String> allowedKeywords, Set <String> inputKeys){
		boolean matchFound = allowedKeywords.containsAll(inputKeys);
		return matchFound;
	}
	
	/**
	 * The test method
	 * @param args
	 */
	public static void main(String[] args) {
		
	}

}
