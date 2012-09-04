package org.liverpool.pipeline;

import java.util.HashMap;

import org.liverpool.gff.ValidateGFF;
import org.liverpool.utils.CreateSummaryOfTheCompleteDataset;
import org.liverpool.utils.PerformPostProcessingUsingMzLib;

public class ProteoAnnotator {

    public static String directorymode_Usage = " inputTemplateFile parserInputFile inputMgfDir outputMgfDir parserInputFile";
    public static String directorymode_Example = "directorymode inputTemplateFile.txt parserInputFile.txt inputMgfDir outputMgfDir parserInputFile.txt";
    
    public static String userFeedback="java -jar jar-location/proteoAnnotator.jar ";
    
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
     * 
     * @param args
     */
	public static void main(String[] args) {
		try{
			
			ProteoAnnotator pa = new ProteoAnnotator();

			if((args.length < 1) || (!pa.allFunctions.containsKey(args[0]))){
				System.out.println("Use options :" + pa.allFunctions.keySet().toString());
				System.exit(0);
			}
				
			if(args[0].trim().equals("directory_mode")){
				if(args.length != 5){
					// print error
				}else{
					String [] prog_args = new String[4];
					prog_args[0] = args[1];
					prog_args[1] = args[2];
					prog_args[2] = args[3];
					prog_args[3] = args[4];
			
					RunPipelineForWholeDirectory.main(prog_args);
				}
			}else if(args[0].trim().equals("single_mode")){
				if(args.length != 5){
					// print error
				}else{
					String [] prog_args = new String[4];
					prog_args[0] = args[1];
					prog_args[1] = args[2];
					prog_args[2] = args[3];
					prog_args[3] = args[4];
					
					RunPipelinePerMgfForCluster.main(prog_args);
				}
			}else if(args[0].trim().equals("gff_operation-fasta")){
				if(args.length != 2){
					// print error
				}else{
					String [] prog_args = new String[1];
					prog_args[0] = args[1];
					ValidateGFF.main(prog_args);
				}
			}else if(args[0].trim().equals("gff_operation-map")){
				if(args.length != 4){
					// print error
				}else{
					String [] prog_args = new String[4];
					prog_args[0] = args[1];
					prog_args[1] = args[2];
					prog_args[2] = args[3];
					prog_args[3] = args[4];
					ValidateGFF.main(prog_args);
				}
			}else if(args[0].trim().equals("create_summary")){
				if(args.length != 3){
					// print error
				}else{
					String [] prog_args = new String[2];
					prog_args[0] = args[1];
					prog_args[1] = args[2];
					CreateSummaryOfTheCompleteDataset.main(prog_args);
				}
			}else if(args[0].trim().equals("post_processing")){
				if(args.length != 8){
					// print error
				}else{
					String [] prog_args = new String[7];
					prog_args[0] = args[1];
					prog_args[1] = args[2];
					prog_args[2] = args[3];
					prog_args[3] = args[4];
					prog_args[4] = args[5];
					prog_args[5] = args[6];
					prog_args[6] = args[7];
					
					PerformPostProcessingUsingMzLib.main(prog_args);
				}
			}else{
				// signal error....
			}	
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
