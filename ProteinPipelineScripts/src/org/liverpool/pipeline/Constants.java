package org.liverpool.pipeline;

public class Constants {
	
		// Path for external executables - Mapped to ExternalSetup.conf
		public static String OMSSA_KEYWORD = "omssa_parserMzIdentML";
		public static String TANDEM_KEYWORD = "tandem_parserMzIdentML";

		//Path for external executables - Mapped to ExternalSetup.conf
		public static String OMSSA_EXECUTABLE = "omssa_executable";
		public static String TANDEM_EXECUTABLE = "tandem_executable";
	
		// Used in ConstructParamFileForMzIdentMLParser.java and ConstructSearchCommand.java -Mapped with omssaKeywords.txt
		public static String SUBSTRING_TO_IDENTIFY_MOD_IN_SEARCHINPUT = "mod";
		public static String SUBSTRING_FOR_FIXED_MOD_IN_SEARCHINPUT = "fixed"; // to check if the mod is fixed or variable
		public static String SUBSTRING_TO_IDENTIFY_ENZYME = "enzyme";
		public static String SUBSTRING_TO_IDENTIFY_MISSED_CLEAVAGES = "missed";
		public static String SUBSTRING_TO_IDENTIFY_PRECURSOR = "precursor";
		public static String SUBSTRING_TO_IDENTIFY_PRODUCT = "product";
		public static String SUBSTRING_TO_IDENTIFY_TAXONOMY_FILE = "taxonomy";
		
		public static String STRING_TO_IDENTIFY_SPECIES = "species";
		public static String STRING_TO_IDENTIFY_FASTA_FILE = "fasta_file";
			
		// Fixed Strings required for MultipleSearch jar
		public static String OMSSA_ID = "omssa";
		public static String TANDEM_ID = "X!Tandem";
		
		// Required for MultipleSearch jar - Mapped with Strings in paramKeywords.txt
		public static String DECOY_RATIO = "decoy_ratio";
		public static String RANK_THRESHOLD = "rank_threshold";
}
