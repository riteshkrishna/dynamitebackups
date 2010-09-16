package org.liverpool.pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.liverpool.utils.ReadConfigurationFiles;
import org.liverpool.utils.ResolveOmssaModificationIdentifiers;
import org.liverpool.utils.ValidateInputFiles;

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
		return command;
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
					command = fillTheCommandTemplate(inputHash,templateCommand);
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
	 * TODO - We need to create Taxonomy file + need to fill the inputHash with the file location
	 *  
	 * @return
	 */
	String fillTheCommandTemplateAfterResolvingForTandem(HashMap <String, String> inputHash, String templateCommand){
		
		String command = new String(templateCommand);
		
		try{
			// Remove all -te, -to etc strings from the input content
			Iterator<String> keys = inputHash.keySet().iterator();
			while(keys.hasNext()){
				String key = keys.next();
				String [] value = inputHash.get(key).split("\\s"); // split the value for space
				if(value.length > 1)
					inputHash.put(key, value[1]);
			}	
			
			// Resolve the mode identifiers
			ResolveOmssaModificationIdentifiers rom = new ResolveOmssaModificationIdentifiers(this.inputFile, this.inputFileDelimiter,
												  this.umodFile,this.umodFileDelimiter,
												  this.omssaIdentifierInHeaderInUmodFile);
			int [][]modArray = rom.omssaModificationsFromSearchInput();
			String lineDelimiter = "\n";
			String modificationString = rom.resolveOmssaModificationNumbers(modArray,lineDelimiter);
			// TODO create xml string here and put in the hash for fixed, variable mod keys
			// - WHAT IS THE XML FOR X!TANDEM
			// Construct xml snippet for  - {{ fixed_mod_id }} 
			// Construct xml snippet for  - {{ variable_mod_id }}
			
			
			// Resolve all other identifier
			String enzyme = null;
			String missedCleavages = null;
			String product_tol = null;
			String precursor_tol = null;
			
			HashMap <String, String> enzymeFileContent = rc.readInputCsvFile(this.enzymeFile, this.enzymeFileDelimiter);
			keys = null;
			keys = inputHash.keySet().iterator();
			while(keys.hasNext()){
				String key = keys.next(); 
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_ENZYME)){
						enzyme = enzymeFileContent.get(inputHash.get(key));
						//TODO create xml string here and put in the hash - WHAT IS THE XML FOR X!TANDEM
						// Construct xml snippet for  - {{ enzyme_name }}
						
				}
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_MISSED_CLEAVAGES)){
					missedCleavages = inputHash.get(key);
					String xmlForMissedCleavages = "<note type = \"input\" label = \"scoring, maximum missed cleavages sites\">" + missedCleavages + "</note>";
					inputHash.put(key, xmlForMissedCleavages);
				}
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_PRODUCT)){
					product_tol = inputHash.get(key);
					String xmlForFragmentMass  = "<note type=\"heading\">Spectrum general</note> \n" 
												 + "\t<note type = \"input\" label = \"spectrum, fragment monoisotopic mass error\">" + product_tol + "</note> \n" 
												 + "\t<note type=\"input\" label=\"spectrum, fragment monoisotopic mass error units\">Da</note>" ;
					inputHash.put(key, xmlForFragmentMass);
				}
				if(key.contains(Constants.SUBSTRING_TO_IDENTIFY_PRECURSOR)){
					precursor_tol = inputHash.get(key);
					String xmlForParenttMass = "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">"  + precursor_tol  + "</note> \n"
											  + "\t<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">"+ precursor_tol + "</note> \n"
											  + "\t<note type=\"input\" label=\"spectrum, parent monoisotopic mass error units\">ppm</note> \n"
											  + "\t<note type=\"input\" label=\"spectrum, parent monoisotopic mass isotope error\">yes</note>";
					inputHash.put(key, xmlForParenttMass);
				}
				
				// Check if the taxonomy file exists, if not, then create a new one on that location
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
							String taxonomyContent = fillTheCommandTemplate(inputHash,taxonomyTemplate);
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
		
		
		return command;
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
		String searchEngineName = Constants.OMSSA_ID;
		
		File umodFile								= new File("resources/UMOD_TABLE.csv"); 
		String umodFileDelimiter					= ",";	
		String omssaIdentifierInHeaderInUmodFile	= "Omssa_ID";	
		File enzymeFile							    = new File("resources/enzymeList.csv"); 
		String enzymeFileDelimiter					= "=";

		
		ConstructSearchCommand cs = new ConstructSearchCommand(input,template,allowedKeyword,inputDelimiter,
																umodFile, umodFileDelimiter, 
																omssaIdentifierInHeaderInUmodFile,
																enzymeFile, enzymeFileDelimiter);
				
		String command = cs.accpetInputAndCreateCommand(searchEngineName);
		
		// For X!Tandem
		log.info("Constructing command for X!Tandem...");
		template = new File("templates/tandem_template.txt");
		searchEngineName = Constants.TANDEM_ID;
		
		cs = new ConstructSearchCommand(input,template,allowedKeyword,inputDelimiter,
										umodFile, umodFileDelimiter, 
										omssaIdentifierInHeaderInUmodFile,
										enzymeFile, enzymeFileDelimiter);

		command = cs.accpetInputAndCreateCommand(searchEngineName);
		
		
	}

}
