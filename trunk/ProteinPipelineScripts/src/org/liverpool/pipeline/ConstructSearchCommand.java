package org.liverpool.pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.utils.InputCleaning;
import org.liverpool.utils.ReadConfigurationFiles;
import org.liverpool.utils.ResolveOmssaModificationIdentifiers;
import org.liverpool.utils.ValidateInputFiles;

import com.sun.xml.internal.ws.util.StringUtils;

public class ConstructSearchCommand {

	static Logger log = Logger.getLogger(ConstructSearchCommand.class);
	
	ReadConfigurationFiles rc;
	ValidateInputFiles vif;
	
	File inputFile;
	File templateFile;
	File allowedKeywordFile;
	String inputFileDelimiter;
	
	File umodFile; 
	String umodFileDelimiter;	
	String omssaIdentifierInHeaderInUmodFile;	
	File enzymeFile; 
	String enzymeFileDelimiter;

	HashMap<String,String> inputContent;
	
	/**
	 * 
	 * @param input
	 * @param template
	 * @param allowedKeyword
	 * @param inputDelimiter
	 * @param umodFile
	 * @param umodFileDelimiter
	 * @param omssaIdentifierInHeaderInUmodFile
	 * @param enzymeFile
	 * @param enzymeFileDelimiter
	 */
	public ConstructSearchCommand(File input,File template,File allowedKeyword,String inputDelimiter,
								File umodFile, String umodFileDelimiter, 
								String omssaIdentifierInHeaderInUmodFile,
								File enzymeFile, String enzymeFileDelimiter) {
		
		rc = new ReadConfigurationFiles();
		vif = new ValidateInputFiles();
		 
		this.inputFile = input;
		this.templateFile = template;
		this.allowedKeywordFile = allowedKeyword;
		this.inputFileDelimiter = inputDelimiter;
		
		this.umodFile = umodFile;
		this.umodFileDelimiter = umodFileDelimiter;
		this.omssaIdentifierInHeaderInUmodFile = omssaIdentifierInHeaderInUmodFile;
		this.enzymeFile = enzymeFile;
		this.enzymeFileDelimiter = enzymeFileDelimiter;
		
		inputContent = new HashMap<String,String>();
	}
	
	
	HashMap <String, String> readInputFile(){
		HashMap <String, String> inputs = new HashMap <String, String> ();
		try{
			inputs = rc.readInputCsvFile(inputFile, inputFileDelimiter);
		}catch(Exception ex){
			log.fatal("Problem with Input File");
			ex.printStackTrace();
		}
		return inputs;
	}
	
	
	String readTemplateCommandFile(){
		String command = new String();
		try{
			command = rc.readTemplateCommandFile(templateFile);
		}catch(Exception ex){
			log.fatal("Problem with Template File");
			ex.printStackTrace();
		}
		return command;
	}
	
	
	Set <String> readAllowedKeywordFile() throws Exception{
		Set <String> keywords = rc.readKeywordDefinitionFile(allowedKeywordFile);
		return keywords;
	}
	
	/**
	 * In the template file, the text to be replaces is indicated like - {{ text }}. So, we need to search for
	 * these texts surrounded by double curly brackets and replace them by appropriate values read from the input file.
	 *  
	 * @param inputHash
	 * @param templateCommand
	 * @return
	 */
	String fillTheCommandTemplate(HashMap <String, String> inputHash, String templateCommand){
		String command = new String(templateCommand);
		Iterator <String> inputkeys = inputHash.keySet().iterator();
		while(inputkeys.hasNext()){
			String key = inputkeys.next();
			String textToReplace = "{{ " + key.trim() + " }}";
			command = command.replace(textToReplace,inputHash.get(key));
		}
		
		// Remove all the -te,-to etc
		InputCleaning ic = new InputCleaning();
		inputHash = ic.cleanTheSearchEngineInputFromExtraFlags(inputHash);
		// Store the information in the class
		this.inputContent.putAll(inputHash);
		
		return command;
	}
	
	/**
	 * Omssa specific command template resolving...
	 * @param inputHash
	 * @param templateCommand
	 * @return
	 */
	String fillTheCommandTemplateForOmssa(HashMap <String, String> inputHash, String templateCommand){
		String command = new String(templateCommand);
		Iterator <String> inputkeys = inputHash.keySet().iterator();
		while(inputkeys.hasNext()){
			String key = inputkeys.next();
			String textToReplace = "{{ " + key.trim() + " }}";
			String omssaCompatibleParameter = omssaParameterReplacementFlags(key.trim(),inputHash.get(key));
			//command = command.replace(textToReplace,inputHash.get(key));
			command = command.replace(textToReplace,omssaCompatibleParameter);
		}
		
		// Remove all the -te,-to etc
		InputCleaning ic = new InputCleaning();
		inputHash = ic.cleanTheSearchEngineInputFromExtraFlags(inputHash);
		// Store the information in the class
		this.inputContent.putAll(inputHash);
		
		return command;
	}
	
	/**
	 * This function adds the OMSSA specific flags for creating a proper command
	 * @param omssakeyword
	 * @param userInput
	 * @return
	 */
	String omssaParameterReplacementFlags(String omssakeyword, String userInput){
		String replacementText = new String();
				
		if(omssakeyword.contains("input_file"))
			replacementText = "-fm " + userInput;	
		
		if(omssakeyword.contains("fasta_file"))
			replacementText = "-d " + userInput;
		
		if(omssakeyword.contains("enzyme_name"))
			replacementText = "-e " + userInput;

		if(omssakeyword.contains("missed_cleavages"))
			replacementText = "-v " + userInput;
			
		if(omssakeyword.contains("product_tolerance"))
			replacementText = "-to " + userInput;
		
		if(omssakeyword.contains("precursor_tolerance"))
			replacementText = "-te " + userInput;
			
		if(omssakeyword.contains("fixed_mod_id"))
			replacementText = "-mf " + userInput;
			
		if(omssakeyword.contains("variable_mod_id"))
			replacementText = "-mv " + userInput;
			
		if(omssakeyword.contains("output_file"))
			replacementText = "-oc " + userInput;
			
		if(replacementText == null)
			log.fatal("Problem with Input Parameters - Check the Input Parameter file");
		
		return replacementText;	
		
	}
	/**
	 * This function will take the command string constructed after filling the template, and remove
	 * the unused "{{ text }}" blocks to produce the usuable command string. 
	 */
	String removeExtraPalceholdersFromTemplate(String commandString){
		return commandString.replaceAll("\\{\\{ .* \\}\\}", "");
	}
	
	/**
	 * 
	 * @return
	 */
	public String accpetInputAndCreateCommand(String searchEngineName) {

		String command = new String();
		try{
			HashMap <String, String> inputHash = readInputFile();
			Set <String> allowedKeywords = readAllowedKeywordFile();
			String templateCommand = readTemplateCommandFile();
			
			if(vif.validateContentOfInputFileAgainstAllowedKeywords(allowedKeywords, inputHash.keySet())){
				if(searchEngineName.equals(Constants.OMSSA_ID))
					command = fillTheCommandTemplateForOmssa(inputHash,templateCommand);
				if(searchEngineName.equals(Constants.TANDEM_ID)){
					command = fillTheCommandTemplateAfterResolvingForTandem(inputHash,templateCommand);
				}
			}
			else{
				log.error("Invalid input keywords found in the input file");
				throw new Exception();
			}
			
			// Once the template has been filled with user-given entries, we must remove the unused "{{ text }}"
			// place-holders in the constructed command string
			command = removeExtraPalceholdersFromTemplate(command);
			log.info("Created Command :: "+ command);
			
		}catch(Exception ex){
			log.fatal("Failed to create command.");
			ex.printStackTrace();
		}
		return command;
	}
	
		
	/**
	 * Special processing is required for X!Tandem
	 *  
	 * @return
	 */
	String fillTheCommandTemplateAfterResolvingForTandem(HashMap <String, String> inputHash, String templateCommand){
		
		String command = new String(templateCommand);
		
		try{
			// Remove all -te, -to etc strings from the input content
			InputCleaning ic = new InputCleaning();
			inputHash = ic.cleanTheSearchEngineInputFromExtraFlags(inputHash);
			
			// Resolve the mode identifiers
			ResolveOmssaModificationIdentifiers rom = new ResolveOmssaModificationIdentifiers(this.inputFile, this.inputFileDelimiter,
												  this.umodFile,this.umodFileDelimiter,
												  this.omssaIdentifierInHeaderInUmodFile);
			int [][]modArray = rom.omssaModificationsFromSearchInput();
			String lineDelimiter = "\n";
			String modificationString = rom.resolveOmssaModificationNumbers(modArray,lineDelimiter);	
			
			// Resolve all other identifier
			String enzyme = null;
			String missedCleavages = null;
			String product_tol = null;
			String precursor_tol = null;
			
			HashMap <String, String> enzymeFileContent = rc.readInputCsvFile(this.enzymeFile, this.enzymeFileDelimiter);
			Iterator<String> keys = inputHash.keySet().iterator();
			int entryFlag = 0; // To ensure that the mod part is processed only once
			
			while(keys.hasNext()){
				String key = keys.next(); 
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_ENZYME)){
						enzyme = enzymeFileContent.get(inputHash.get(key));
						
						//TODO - At present, we have support for only Trypsin.It should be possible to
						// provide support for other enzymes as well, if we know the regular expression for 
						// other enzymes.
						if(enzyme.contains("Trypsin")){
							String xmlForEnzyme = "<note type=\"input\" label=\"protein, cleavage site\">[KR]|{P}</note> ";
							inputHash.put(key, xmlForEnzyme);
						}
						
				}
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_MISSED_CLEAVAGES)){
					missedCleavages = inputHash.get(key);
					String xmlForMissedCleavages = "<note type = \"input\" label = \"scoring, maximum missed cleavages sites\">" + missedCleavages + "</note>";
					inputHash.put(key, xmlForMissedCleavages);
					log.info("XML for missed cleavages" + xmlForMissedCleavages);
				}
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_PRODUCT)){
					product_tol = inputHash.get(key);
					String xmlForFragmentMass  = "<note type=\"heading\">Spectrum general</note> \n" 
												 + "\t<note type = \"input\" label = \"spectrum, fragment monoisotopic mass error\">" + product_tol + "</note> \n" 
												 + "\t<note type=\"input\" label=\"spectrum, fragment monoisotopic mass error units\">Da</note>" ;
					inputHash.put(key, xmlForFragmentMass);
					log.info("XML for Fragment mass" + xmlForFragmentMass);
				}
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_PRECURSOR)){
					precursor_tol = inputHash.get(key);
					String xmlForParentMass = "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">"  + precursor_tol  + "</note> \n"
											  + "\t<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">"+ precursor_tol + "</note> \n"
											  + "\t<note type=\"input\" label=\"spectrum, parent monoisotopic mass error units\">Da</note> \n"
											  + "\t<note type=\"input\" label=\"spectrum, parent monoisotopic mass isotope error\">yes</note>";
					inputHash.put(key, xmlForParentMass);
					log.info("XML for Parent mass" + xmlForParentMass );
				}

				// If we have already processed the modification, then do nothing
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_MOD_IN_SEARCHINPUT) && (entryFlag == 1)){
					inputHash.put(key,"");
				}
				// Otherwise, process the information...
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_MOD_IN_SEARCHINPUT) && (entryFlag == 0)){
					try{
						String xmlForFixedAndVarModification = new String();	
						String [] modifications = modificationString.split(lineDelimiter);
						for(int i = 0; i < modifications.length; i++){
							String xmlForMod = null;
							String [] thisMod = modifications[i].split(","); // Each record is comma separated
							String varOrFixed = thisMod[4];
							String site = thisMod[3]; 
							String massDelta = thisMod[5];
							if(varOrFixed.equalsIgnoreCase("TRUE")){
								if(site.contains("term"))
									xmlForMod = "\t<note type=\"input\" label=\"residue, modification mass\">" + massDelta.concat("@").concat("[") + "</note>";
								else
									xmlForMod = "\t<note type=\"input\" label=\"residue, modification mass\">" + massDelta.concat("@").concat(site) + "</note>";
							}else{
								xmlForMod = "\t<note type=\"input\" label=\"residue, potential modification mass\">" + massDelta.concat("@").concat(site) + "</note>";
							}
							if(xmlForMod != null)
								xmlForFixedAndVarModification = xmlForFixedAndVarModification + "\n"+ xmlForMod;
						}
						
						entryFlag = 1;
						inputHash.put(key, xmlForFixedAndVarModification);
						log.info("XML for Modifications " + xmlForFixedAndVarModification );
						
					}catch(Exception e){
						log.info("Problem in finding information for Modification");
					}
				}
				
				// Create the taxonomy file
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_TAXONOMY_FILE)){
					try{
							File taxonomyFile = new File(inputHash.get(key));
							if(taxonomyFile.exists())
								taxonomyFile.delete();
							taxonomyFile.createNewFile();
							String taxonomyTemplate = "<?xml version=\"1.0\"?> \n" +
													  "<bioml label=\"x! taxon-to-file matching list\"> \n" +
													  "<taxon label = \"{{ " + Constants.STRING_TO_IDENTIFY_SPECIES +" }}\">" +
													  "<file format=\"peptide\" URL=\"{{ "+ Constants.STRING_TO_IDENTIFY_FASTA_FILE +" }}\" />" +
													  "</taxon>" +
													  "</bioml>";
							HashMap<String,String> inputHashCopy = new HashMap<String,String>(inputHash);
							String taxonomyContent = fillTheCommandTemplate(inputHashCopy,taxonomyTemplate);
							taxonomyContent = removeExtraPalceholdersFromTemplate(taxonomyContent);
							BufferedWriter br = new BufferedWriter(new FileWriter(taxonomyFile));
							br.write(taxonomyContent);
							br.close();
							
							log.info("Taxonomy file content : \n" + taxonomyContent);
							
						}catch(Exception e){
							log.fatal("Problem creating Taxonomy file");
							e.printStackTrace();
						}
				}
				
				// Make sure that the output file has xml extension...and also a unique identifier
				if(key.contains(Constants.STRING_TO_IDENTIFY_OUTPUT_FILE)){
					// Generate a random number between 2000 to 3000
					int Min = 2000,Max = 3000;
					int rand = Min + (int)(Math.random() * ((Max - Min) + 1));
					String randomNumberIdentifier = Integer.toString(rand);
					
					String xmlOutputFile = "";
					String presentName = inputHash.get(key);
					
					//String [] name_ext = presentName.split("\\.");
					//if(name_ext.length != 0){
					//	xmlOutputFile = name_ext[0].concat(randomNumberIdentifier).concat(".xml");
					//}else xmlOutputFile = presentName.concat(randomNumberIdentifier).concat(".xml");
					
					// The above commented code fails if some directory in the path contains '.' in it's name.
					// So, do a through processing of the string by skipping the slashes and treating on the
					// last string as the file name - Shweta does this.
					String fileName1 = "";
					int lastSlashIndex = presentName.lastIndexOf("/");
					if(lastSlashIndex > -1){
						String temp = presentName.substring(lastSlashIndex + 1);
						int extDotIndex = temp.indexOf(".");
						if(extDotIndex > -1){
							fileName1 = presentName.substring(0,presentName.length()-(temp.length()-extDotIndex));
						}
						else
						{
							fileName1 = presentName;
						}
					}else {
						int extDotIndex = presentName.indexOf(".");
						if(extDotIndex>-1){
							fileName1 = presentName.substring(0,extDotIndex);
						}else {
							fileName1 = presentName;
						}
					}
					
					xmlOutputFile = fileName1.concat(randomNumberIdentifier).concat(".xml");	
					
					inputHash.put(key, xmlOutputFile);
				}
			}
		

			// After resolving all the references, fill the template.
			Iterator <String> inputkeys = inputHash.keySet().iterator();
			while(inputkeys.hasNext()){
				String key = inputkeys.next();
				String textToReplace = "{{ " + key.trim() + " }}";
				command = command.replace(textToReplace,inputHash.get(key));
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		// Store the information in the class
		this.inputContent.putAll(inputHash);
		return command;
	}
	
	
	/**
	 * 
	 * Get the name of the output file produced
	 * 
	 */
	public String getNameOfOutputFile(){
		return this.inputContent.get(Constants.STRING_TO_IDENTIFY_OUTPUT_FILE);
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		File input = new File("inputFiles/omssa_inputFile.txt");
		File template = new File("templates/omssa_template.txt");
		File allowedKeyword = new File("resources/omssaKeywords.txt");
		String inputDelimiter = "=";
		
		File umodFile								= new File("resources/UMOD_TABLE.csv"); 
		String umodFileDelimiter					= ",";	
		String omssaIdentifierInHeaderInUmodFile	= "Omssa_ID";	
		File enzymeFile							    = new File("resources/enzymeList.csv"); 
		String enzymeFileDelimiter					= "=";

		String searchEngineName = Constants.OMSSA_ID;
		ConstructSearchCommand cs = new ConstructSearchCommand(input,template,allowedKeyword,inputDelimiter,
																umodFile, umodFileDelimiter, 
																omssaIdentifierInHeaderInUmodFile,
																enzymeFile, enzymeFileDelimiter);
				
		String command = cs.accpetInputAndCreateCommand(searchEngineName);
		String outputFileProduced = cs.getNameOfOutputFile();
		
		
		// For X!Tandem
		log.info("Constructing command for X!Tandem...");
		template = new File("templates/tandem_template.txt");
		searchEngineName = Constants.TANDEM_ID;
		
		cs = new ConstructSearchCommand(input,template,allowedKeyword,inputDelimiter,
										umodFile, umodFileDelimiter, 
										omssaIdentifierInHeaderInUmodFile,
										enzymeFile, enzymeFileDelimiter);

		command = cs.accpetInputAndCreateCommand(searchEngineName);
		outputFileProduced = cs.getNameOfOutputFile();
		
	}

}
