package org.liverpool.pipeline;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ExecuteCommands {

	static Logger log = Logger.getLogger(ExecuteCommands.class);
	
	String commandString;
	
	public ExecuteCommands(String command) {
		commandString = new String(command);
	}
	
	/**
	 * 
	 */
	public void executeOmssa(){
		try{
			ProcessBuilder builder = new ProcessBuilder("sh","-c",commandString);
			builder.redirectErrorStream(true);
			final Process process = builder.start();
			
			log.info("Omssa Search Engine started..");
			InputStream is  = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while((line = br.readLine()) != null){
				log.info(line);
			}
			log.info("Omssa Search Engine finished..");
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public void executeTandem(){
		
	}
	
	
	public static void main(String[] args) {
		
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		String command = "/opt/omssa-2.1.9.macos/omssacl -fm /Users/riteshk/Ritesh_Work/Toxo/Toxo_Test_MSDataset/Toxo_1D_Slice1.mgf -d /Users/riteshk/Ritesh_Work/Toxo/ToxoDB/TgondiiGT1AnnotatedProteins_ToxoDB-6.0.fasta -to 0.8 -te 1.5 -mf 3 -mv 1 -oc /Users/riteshk/Ritesh_Work/TestSpace/omssa_testSpace/testrun_osx.csv";
		ExecuteCommands ec = new ExecuteCommands(command);
		ec.executeOmssa();
	}

}
