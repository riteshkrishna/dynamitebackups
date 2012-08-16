package org.liverpool.pipeline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ExecuteCommands {

	static Logger log = Logger.getLogger(ExecuteCommands.class);
		
	public void execute(String commandString){
		try{
			ProcessBuilder builder;
			
			String os = System.getProperty("os.name").toLowerCase();
			
			if(os.indexOf("win") >= 0)
				builder = new ProcessBuilder("cmd.exe","/c",commandString);
			else
				builder = new ProcessBuilder("sh","-c",commandString);
			
			
			builder.redirectErrorStream(true);
			final Process process = builder.start();
			
			log.info("Search Engine started..");
			InputStream is  = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while((line = br.readLine()) != null){
				log.info(line);
			}
						
			log.info("Search Engine finished..");
			
		}catch(Exception ex){
			String errMsg = "\n\n **Error** \n\n" +
					"Unable to execute commands." +
					"Exiting ProteoAnnotator...";
			log.fatal(ex.getMessage() + errMsg);
			System.exit(0);
		}
	}
	
	
	public static void main(String[] args) {
		
		String logProperties = "resources/log4j.properties";
		PropertyConfigurator.configure(logProperties);
		
		ExecuteCommands ec = new ExecuteCommands();
		
		String command = "/opt/omssa-2.1.9.macos/omssacl -fm /Users/riteshk/Ritesh_Work/Toxo/Toxo_Test_MSDataset/Toxo_1D_Slice1.mgf -d /Users/riteshk/Ritesh_Work/Toxo/ToxoDB/TgondiiGT1AnnotatedProteins_ToxoDB-6.0.fasta -to 0.8 -te 1.5 -mf 3 -mv 1 -oc /Users/riteshk/Ritesh_Work/TestSpace/omssa_testSpace/testrun_osx.csv";
		ec.execute(command);
		
		command = "/opt/tandem-osx-intel-10-01-01-4/bin/tandem /Users/riteshk/Ritesh_Work/TestSpace/tandem_testSpace/input.xml";
		ec.execute(command);
	}

}
