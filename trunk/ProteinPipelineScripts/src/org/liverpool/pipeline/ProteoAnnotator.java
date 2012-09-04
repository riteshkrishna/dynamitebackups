package org.liverpool.pipeline;

import java.util.HashMap;

import org.liverpool.gff.ValidateGFF;
import org.liverpool.utils.CreateSummaryOfTheCompleteDataset;
import org.liverpool.utils.PerformPostProcessingUsingMzLib;

import uk.ac.liv.mzidlib.util.Utils;

public class ProteoAnnotator {

	public static String userFeedback="java -jar jar-location/proteoAnnotator.jar ";
	
	public static String create_summary_Usage = "create_summary -resultDir outputDirectory -summaryFile WholeDatasetSummary.txt";
	public static String  post_processing_Usage = "post_processing -searchInput searchInput.txt -databaseInput databaseInput.txt " +
												  "-summaryFile WholeDatasetSummary.txt -csvOutput WholeDatasetSummary.csv " +
												  "-csvThreshold 0.1 -fdrThreshold 0.05 -mzIdentMLFile WholeDatasetSummary.mzid";
    public static String directorymode_Usage = " directory_mode -searchInput searchInput.txt -databaseInput databaseInput.txt -inputMgfDir inputMgfDirectory -outputResultDir outputDirectory";
    public static String single_mode_Usage = " single_mode -searchInput searchInput.txt -databaseInput databaseInput.txt -inputMgf inputMgfFile -outputResultDir outputDirectory";
    public static String gff_operation_fasta_Usage = "gff_operation-fasta -gff3 inputGff.gff";
    public static String gff_operation_map_Usage = "gff_operation-map -gff3 inputGff.gff -summaryFile WholeDatasetSummary.txt " +
    												"-decoyIdentifier decoyIdentifier -output-gff3 outputGff.gff";
    
    
    
    
    private HashMap<String, String> allFunctions = new HashMap<String, String>();
    
    public ProteoAnnotator(){
    	allFunctions.put("directory_mode", "RunPipelineForWholeDirectory");
    	allFunctions.put("single_mode", "RunPipelinePerMgfForCluster");
    	allFunctions.put("gff_operation-fasta", "ValidateGFF");
    	allFunctions.put("gff_operation-map", "ValidateGFF");
    	allFunctions.put("create_summary", "CreateSummaryOfTheCompleteDataset");
    	allFunctions.put("post_processing", "PerformPostProcessingUsingMzLib");
    }
    
	/**
	 * Returns the value of a command-line parameter
	 * 
	 * @param args : command-line arguments (assuming couples in the form "-argname", "argvalue" )
	 * @param name : the parameter 'name' 
	 * @return returns null if the parameter is not found (and is not required). If the parameter is not
	 * found but is required, it throws an error.
	 */
	public String getCmdParameter(String[] args, String name, boolean required) 
	{
		for (int i = 0; i < args.length; i++)
		{
			String argName = args[i];
			if (argName.equals("-" + name))
			{
				String argValue = "";
				if (i + 1 < args.length)
					argValue = args[i+1];
				if (required && (argValue.trim().length() == 0 || argValue.startsWith("-")))
				{
					System.err.println("Parameter value expected for " + argName);
					throw new RuntimeException("Expected parameter value not found: " + argName);
				}
				else if (argValue.trim().length() == 0 || argValue.startsWith("-"))
					return "";
				else
					return argValue;
			}	
		}
		//Nothing found, if required, throw error, else return "";
		if (required)
		{
			System.err.println("Parameter -" + name + " expected ");
			throw new RuntimeException("Expected parameter not found: " + name);
		}
		
		return null;
	}
	
    /**
     * 
     * @param args
     */
	public static void main(String[] args) {
		try{
			
			ProteoAnnotator pa = new ProteoAnnotator();

			if((args.length < 1) || (!pa.allFunctions.containsKey(args[0]))){
				System.out.println("Allowed operations  :" + userFeedback + pa.allFunctions.keySet().toString());
				System.exit(0);
			}
				
			if(args[0].trim().equals("directory_mode")){
				if(args.length != 9){
					userFeedback+=directorymode_Usage;
					System.out.println("Expected Usage for option - directory_mode");
					System.out.println(userFeedback);
				}else{
					String searchInput     =  pa.getCmdParameter(args, "searchInput", true);
	                String databaseInput   =  pa.getCmdParameter(args, "databaseInput", true);
	                String inputMgfDir     =  pa.getCmdParameter(args, "inputMgfDir", true);
	                String outputResultDir =  pa.getCmdParameter(args, "outputResultDir", true);
	                
	                String [] prog_args = new String[4];
					prog_args[0] = searchInput;
					prog_args[1] = databaseInput;
					prog_args[2] = inputMgfDir;
					prog_args[3] = outputResultDir;
			
					RunPipelineForWholeDirectory.main(prog_args);
				}
			}else if(args[0].trim().equals("single_mode")){
				if(args.length != 9){
					userFeedback+=single_mode_Usage;
					System.out.println("Expected Usage for option - single_mode");
					System.out.println(userFeedback);
				}else{
					String searchInput      =  pa.getCmdParameter(args, "searchInput", true);
	                String databaseInput    =  pa.getCmdParameter(args, "databaseInput", true);
	                String inputMgf         =  pa.getCmdParameter(args, "inputMgf", true);
	                String outputResultDir  =  pa.getCmdParameter(args, "outputResultDir", true);
	                
	                String [] prog_args = new String[4];
					prog_args[0] = searchInput;
					prog_args[1] = databaseInput;
					prog_args[2] = inputMgf;
					prog_args[3] = outputResultDir;
			
					RunPipelinePerMgfForCluster.main(prog_args);
				}
			}else if(args[0].trim().equals("gff_operation-fasta")){
				if(args.length != 3){
					userFeedback+=gff_operation_fasta_Usage;
					System.out.println("Expected Usage for option - gff_operation-fasta");
					System.out.println(userFeedback);
				}else{
					String gffFile   =  pa.getCmdParameter(args, "gff3", true);
					String [] prog_args = new String[1];
					prog_args[0] = gffFile;
					ValidateGFF.main(prog_args);
				}
			}else if(args[0].trim().equals("gff_operation-map")){
				if(args.length != 9){
					userFeedback+=gff_operation_map_Usage;
					System.out.println("Expected Usage for option - gff_operation-map");
					System.out.println(userFeedback);
				}else{
					String gffFile         =  pa.getCmdParameter(args, "gff3", true);
					String summaryFile     =  pa.getCmdParameter(args, "summaryFile", true);
					String decoyIdentifier =  pa.getCmdParameter(args, "decoyIdentifier", true);
					String outputGffFile   =  pa.getCmdParameter(args, "output-gff3", true);
					String [] prog_args = new String[4];
					prog_args[0] = gffFile;
					prog_args[1] = summaryFile;
					prog_args[2] = decoyIdentifier;
					prog_args[3] = outputGffFile;
					ValidateGFF.main(prog_args);
				}
			}else if(args[0].trim().equals("create_summary")){
				if(args.length != 5){
					userFeedback+=create_summary_Usage;
					System.out.println("Expected Usage for option - create_summary");
					System.out.println(userFeedback);
				}else{
					String resultDir   =  pa.getCmdParameter(args, "resultDir", true);
	                String summaryFile =  pa.getCmdParameter(args, "summaryFile", true);
	                
					String [] prog_args = new String[2];
					prog_args[0] = resultDir;
					prog_args[1] = summaryFile;
					CreateSummaryOfTheCompleteDataset.main(prog_args);
				}
			}else if(args[0].trim().equals("post_processing")){
				if(args.length != 15){
					userFeedback+=post_processing_Usage;
					System.out.println("Expected Usage for option - post_processing");
					System.out.println(userFeedback);
				}else{
					String proteoInput_search = pa.getCmdParameter(args, "searchInput", true);
					String proteoInput_database = pa.getCmdParameter(args, "databaseInput", true);
					String pipelineSummaryFile = pa.getCmdParameter(args, "summaryFile", true);
					String omssaLikeOutputFile = pa.getCmdParameter(args, "csvOutput", true);
					String csvThreshold = pa.getCmdParameter(args, "csvThreshold", true);
					String fdrThreshold = pa.getCmdParameter(args, "fdrThreshold", true);
					String summarymzIdentFile = pa.getCmdParameter(args, "mzIdentMLFile", true);;
					
					String [] prog_args = new String[7];
					prog_args[0] = proteoInput_search;
					prog_args[1] = proteoInput_database;
					prog_args[2] = pipelineSummaryFile;
					prog_args[3] = omssaLikeOutputFile;
					prog_args[4] = csvThreshold;
					prog_args[5] = fdrThreshold;
					prog_args[6] = summarymzIdentFile;
					
					PerformPostProcessingUsingMzLib.main(prog_args);
				}
			}else{
				System.out.println("Error insufficient arguments entered, options: " + userFeedback);
	            System.out.println(create_summary_Usage + "\n");
	            System.exit(1);
			}	
		}catch(Exception e){
			//e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}

}
