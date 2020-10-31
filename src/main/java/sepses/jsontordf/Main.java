package sepses.jsontordf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;
import org.yaml.snakeyaml.Yaml;


public class Main {
	public static void main( String[] args ) throws Exception
    {
		//======yaml config=================
		 Map<String, Object> s = Utility.readYamlFile("config.yaml");
	       String outputdir= s.get("output-dir").toString();
	       String inputdir= s.get("input-dir").toString();
	       String rmldir= s.get("rml-dir").toString();
	       String rmlfile= s.get("rml-file").toString();
	       String triplestore= s.get("triple-store").toString();
	       String backupfile= s.get("backup-file").toString();
	       String livestore= s.get("live-store").toString();
	       ArrayList<String> fieldfilter= (ArrayList<String>) s.get("field-filter");
	       
	       
	       
		//=====commandline argument===========
		  Options options = new Options();
	      options.addOption("t", true, "Type of parser (elastic, darpa)");
	      options.addOption("f", true, "file location");
	      options.addOption("l", true, "line number to process for each iteration");
	      options.addOption("e", true, "sparql endpoint");
	      options.addOption("n", true, "namegraph");
	      options.addOption("sl", true, "starting line, default 0");
	      
	      CommandLineParser parser = new DefaultParser();
	      CommandLine cmd = parser.parse(options, args);
	      
	      String type = cmd.getOptionValue("t");
	      String file = cmd.getOptionValue("f");
	      String line = cmd.getOptionValue("l");
	      String sparqlEp = cmd.getOptionValue("e");
	      String namegraph = cmd.getOptionValue("n");
	      String startingLine = cmd.getOptionValue("sl");
	      
	     // String rmlFile = cmd.getOptionValue("m");
	      
	  	//====== only for experiment in IDE, please uncomment this lines when you compile ========= 
//	      type = "darpa";
//	      file = inputdir+"darpa/cadets100000.json";
//	  	  line = "100000";
//	      if(triplestore.equals("virtuoso")) {  	  
//	  	     sparqlEp ="http://localhost:8890/sparql";
//	      }else if(triplestore.equals("graphdb")){
//	  	     sparqlEp = "http://localhost:7200/repositories/cadets100000";
//	      }else {
//	  	    //default: graphdb	 
//	  	     sparqlEp = "http://localhost:7200/repositories/cadets100000";
//	  	   }
//	      namegraph = "http://w3id.org/sepses/graph/cadets100000";
//	  	  startingLine = "0";
//	      //=======end of experiment in IDE   =============
	    
	     // System.exit(0);
	      JsonReader.readJson(type, file, line, sparqlEp, namegraph, startingLine, outputdir, inputdir, rmldir, rmlfile, triplestore, backupfile, fieldfilter, livestore);
	      
	      
	      FileUtils.cleanDirectory(new File(outputdir));
	      
//		String txt = " processes = search Process:Create\r\n" + 
//				"     reg = filter processes where (exe == \"reg.exe\" and parent_exe == \"cmd.exe\")\r\n" + 
//				"       cmd = filter processes where (exe == \"cmd.exe\" and parent_exe != \"explorer.exe\"\")\r\n" + 
//				"     reg_and_cmd = join (reg, cmd) where (reg.ppid == cmd.pid and reg.hostname == cmd.hostname)     output reg_and_cmd";
//
//		String configFile = "pseudocode-regex.yaml";
//		App app = new App(configFile);
//		app.parsePseudocode(txt);
    	
//		
//		
//		InputStream input = JsonReader.class.getClassLoader().getResourceAsStream(configFile);
//		Yaml yaml = new Yaml();
//		Map<String, Object> configData = yaml.load(input);
//		String pathFolder = configData.get("car-directory").toString();
//		
//		 final File folder = new File(pathFolder);
//			
//	        ArrayList<String> listFiles = listFilesForFolder(folder);
//			 
//			
//	  
//	        for (String filename : listFiles) {
//		    	System.out.println(filename);
//		    	
//		    	//parseCAR(pathFolder+"/"+filename);
//		        //saveModelToFile(model);
//		    	
//	            
//		    }
//		
//
//		
    }
	public static ArrayList<String> listFilesForFolder(final File folder) {
		ArrayList<String> listFiles = new ArrayList<String>();
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry);
	        } else {
	        	listFiles.add(fileEntry.getName());
	        }
	    }
	   return listFiles; 
	}
	public void parseCar(String file) throws FileNotFoundException {
	
		 final File initialFile = new File(file);
	      final InputStream input = new FileInputStream(initialFile);
		Yaml yaml = new Yaml();
		Map<String, Object> configData = yaml.load(input);
		String implementation = configData.get("").toString();
	//	App app = new App(configFile);
	//	app.parsePseudocode(implementation);
	}
}
