package sepses.jsontordf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelFactoryBase;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rdfhdt.hdt.hdt.HDTManager;

import com.jayway.jsonpath.JsonPath;

import net.sf.saxon.functions.Empty;
import sepses.jsontordf.JSONRDFParser;

public class JsonReader {
	
	public static void readJson(String t, String file, String l, String se, String ng, String sl, String outputdir, String inputdir, String rmldir, String rmlfile, String triplestore, String backupfile,  ArrayList<String> fieldfilter, String livestore) throws Exception {
		
		String initFile = Utility.getOriginalFileName(ng);
		String initRdfFile = inputdir+initFile+"-init.ttl";
		
		File initRdfFileObj = new File(initRdfFile);
		initRdfFileObj.createNewFile();
		
		
		String initHDTFile = inputdir+initFile+".hdt";
		String initHDTProvFile = inputdir+initFile+"_prov.hdt";
		
		
		
		String filename =file;
		Integer lineNumber = 1; // 1 here means the minimum line to be extracted
		if(l!=null) {lineNumber=Integer.parseInt(l);}
		String sparqlEp = se;
		String namegraph = ng;
		Integer startingLine = 1; // 1 means start from the beginning
		if(sl!=null) {startingLine=Integer.parseInt(sl);}
		
		InputStream jf = new FileInputStream(filename);
		BufferedReader in = new BufferedReader(new InputStreamReader(jf));
		
		Model masterModel = ModelFactory.createDefaultModel();	
		Model provModel = ModelFactory.createDefaultModel();
		org.eclipse.rdf4j.model.Model jmodels = new LinkedHashModel();
		JSONParser jsparser = new JSONParser(); 
		JSONObject alljsObj = new JSONObject();
		JSONArray alljson = new JSONArray();
		JSONObject json = new JSONObject();
		
			
		// create in one json object
		Integer countLine=0;
		Integer templ = 0;
		Integer group=0;
		HashMap<String, Model> outputModels = new HashMap<String, Model>();
		
		outputModels.put("masterModel", masterModel);
		outputModels.put("provModel", provModel);

		masterModel = RDFDataMgr.loadModel(initRdfFile) ;
		//check if existing .hdt model exists
		
		File hdtFilePath = new File(initHDTFile);
		if(hdtFilePath.exists()) {
			System.out.println("master hdtFile exists!");
			Model prevMasterModel = Utility.loadHDTToJenaModel(initHDTFile);
			masterModel.add(prevMasterModel);
			prevMasterModel.close();
			//when loading hdt file, this file is created then it's better to remove it
			//Utility.deleteFile(outputFileHDTTemp);
			Utility.deleteFile(initHDTFile+".index.v1-1");		
		}
		
			//System.out.println("outputFileProvHDTTemp =>"+outputFileProvHDTTemp);
		provModel = RDFDataMgr.loadModel(initRdfFile) ;
		
		
		File hdtProvFilePath = new File(initHDTProvFile);
		if(hdtProvFilePath.exists()) {
			System.out.println("prov hdtFile exists!");
			Model prevProvModel = Utility.loadHDTToJenaModel(initHDTProvFile);
			provModel.add(prevProvModel);
			prevProvModel.close();
			//Utility.deleteFile(outputFileProvHDTTemp);
			Utility.deleteFile(initHDTProvFile+".index.v1-1");
		}
		
		
		
				
		while (in.ready()) {
		
			String line = in.readLine();
			//System.out.println(countLine);
			if (countLine.equals(startingLine)) {
				System.out.println("reading from line : "+ startingLine);
					group=((int) Math.ceil((startingLine-1)/lineNumber));
			}
			if(countLine >= startingLine) {
				line = cleanLine(line); // sometimes the data should be cleaned first
		
				//skip strange character inside line
				try {
					json = (JSONObject) jsparser.parse(line);
		
				} catch (Exception e) {
					System.out.print("strange character skipped => ");
					System.out.println(line);
				}
		
				JSONObject jsondatum = (JSONObject) json.get("datum");
				JSONObject jsonevent = (JSONObject) jsondatum.get("Event");
				if(jsonevent!= null) {
					if(!filterLine(jsonevent, fieldfilter)){
						alljson.add(json);	
					}
				} else {
					alljson.add(json);
				}
				templ++;
		
				if(templ.equals(lineNumber)) {
					System.out.println("populating json...");
					group++;
					System.out.println("parsing "+group+" of "+lineNumber+" json to rdf...");
					alljsObj.put("logEntry", alljson); // add this term to group as one json object
					JSONRDFParser jps = new JSONRDFParser(rmldir+rmlfile);
					jmodels = jps.Parse(alljsObj.toJSONString());
			
					//saving main model
					System.out.println("saving main model...");
					String outputFile = Utility.getOriginalFileName(filename)+"_"+lineNumber+"_"+group+".ttl";
					String outputFileName = Utility.saveRDF4JModel(jmodels,outputdir,outputFile);
					
					//join to prev hdt model
					System.out.print("combine to prev hdt model if (exists)...");	
					Model masterModelTemp = RDFDataMgr.loadModel(outputFileName) ;
					masterModel.add(masterModelTemp);
					Utility.deleteFile(outputFileName);

					//store main model (if we need to store the parsed rdf log lines, uncomment the line bellow!)
					//Utility.storeFileInRepo(triplestore,outputFileName, sparqlEp, namegraph, "dba", "dba");
					
					Provenance.generateEventProvenance(masterModel, provModel, outputdir, namegraph, sparqlEp, outputFile, triplestore, livestore);
					
					alljson.clear();
					alljsObj.clear();
					jmodels.clear();
					templ=0;
				}
			   }
	  
			countLine++;
			

	}
	// check the rest 
	in.close();
	if(templ!=0) {
		System.out.println("the rest is less than "+lineNumber+" which is "+templ);
		alljsObj.put("logEntry", alljson); // add this term to group as one json object
		JSONRDFParser jps = new JSONRDFParser(rmldir+rmlfile);
		jmodels = jps.Parse(alljsObj.toJSONString());
	
		//saving main model
		System.out.print("saving main model...");
		String outputFile = Utility.getOriginalFileName(filename)+"_"+templ+".ttl";
		String outputFileName = Utility.saveRDF4JModel(jmodels,outputdir,outputFile);
		
		
		//join to prev hdt model
		System.out.print("combine to prev hdt model if (exists)...");	
		Model masterModelTemp = RDFDataMgr.loadModel(outputFileName) ;
		masterModel.add(masterModelTemp);
		Utility.deleteFile(outputFileName);
		
		//store main model
		//Utility.storeFileInRepo(triplestore,outputFileName, sparqlEp, namegraph, "dba", "dba");
		
		Provenance.generateEventProvenance(masterModel, provModel, outputdir, namegraph, sparqlEp, outputFile, triplestore, livestore);

		alljson.clear();
		alljsObj.clear();
		jmodels.clear();
		templ=0;
	}
	
	//save mastermodel to rdf..
	System.out.println("save master model to rdf file...");
	String outputFileName = Utility.getOriginalFileName(namegraph)+".ttl";
	String outputMasterFile = Utility.saveToFile(masterModel,outputdir,outputFileName);
	
	//save provModel to rdf..
	System.out.println("save prov model to rdf file...");
	String outputProvFileName = Utility.getOriginalFileName(namegraph)+"_prov.ttl";
	String outputProvFile = Utility.saveToFile(provModel,outputdir,outputProvFileName);
	
	//store prov to triplestore
	if(livestore=="false") {
		Utility.storeFileInRepo(triplestore,outputProvFile, sparqlEp, namegraph+"_prov", "dba", "dba");
	}
	
	//save rdf to .hdt
	System.out.println("save master and prov model rdf to hdt....");
	String outputMasterHDT = inputdir+Utility.getOriginalFileName(namegraph)+".hdt";
	Utility.generateHDTFile(namegraph, outputMasterFile, "TURTLE", outputMasterHDT);
	String outputProvHDT = inputdir+Utility.getOriginalFileName(namegraph)+"_prov.hdt";
	Utility.generateHDTFile(namegraph, outputProvFile, "TURTLE", outputProvHDT);
	
	
	provModel.close();
	masterModel.close();
	
//	alljsObj.put("logEntry", alljson);
//	System.out.println("parsing json to rdf...");
//	//System.out.print(alljsObj);
//	//System.exit(0);
//	JSONRDFParser jps = new JSONRDFParser("rml/darpa-engagement3.rml");
//	jmodels = jps.Parse(alljsObj.toJSONString());
//	System.out.println("saving rdf...");	
/*	while (in.ready()) {
			String line = in.readLine();
			//UUID uuid = UUID.randomUUID();
			JSONParser parser = new JSONParser(); 
			//System.out.println(line);
			line = line.replaceAll("com.bbn.tc.schema.avro.cdm18.","");
			//System.out.println(line);
//			System.exit(0);
			String lineLastChar =  line.substring(line.length()-1);
			if(lineLastChar.equals(",")) {
				line = line.substring(0,line.length()-1);
			}
			
			JSONObject json = (JSONObject) parser.parse(line);
			if(fileType.equals("darpa")) {
				//System.out.println("parse darpa..");
				// Model darpa = sepses.jsontordf.JSONParser.Parse(json.toJSONString(),"rml/darpa-engagement3.rml");
				JSONRDFParser jp = new JSONRDFParser("rml/darpa-engagement3.rml");
				
				org.eclipse.rdf4j.model.Model darpa = jp.Parse(json.toJSONString());
			//	System.out.print(darpa);
			//	System.exit(0);
				jmodels.addAll(darpa);				
			}else {
		
			String ts = JsonPath.read(json,"$.@timestamp");
			json.put("timestamp", ts);
			//json.put("uuid", uuid);
		
			
			String type = JsonPath.read(json,"$.agent.type");
			if(type.equals("auditbeat")) {
				//System.out.println(type);
				//System.out.println(json.toJSONString());
				Model audit = sepses.jsontordf.JSONRDFParser.Parse(json.toJSONString(),"rml/auditbeat.rml");
				models.add(audit);
				
			}
			else if(type.equals("packetbeat")) {
				//System.out.println(type);
				//System.out.println(json.toJSONString());
				Model packet = sepses.jsontordf.JSONRDFParser.Parse(json.toJSONString(),"rml/packetbeat.rml");
				models.add(packet);
				
			}else if(type.equals("winlogbeat")) {
				//System.out.println(type);
				//System.out.println(json.toJSONString());
				Model winlog = sepses.jsontordf.JSONRDFParser.Parse(json.toJSONString(),"rml/winlogbeat.rml");
				models.add(winlog);
				
			}
//			JSONRDFParser jp = new JSONRDFParser(RMLFile);
//			jp.Parse(alljsObj.toString());
			//System.out.println(json);
			//JSONArray jsonarray = new JSONArray("["+line+"]");
			//i++;
	}
	}
	*/
//	Utility.saveRDF4JModel(jmodels, "output",filename);
//	//Utility.saveToFile(models, "output",filename);
//	Utility.copyFileUsingStream(hdtOutput.get("master"), initHDTFile);
//	Utility.copyFileUsingStream(hdtOutput.get("prov"), initHDTProvFile);
//	
	System.out.println("Finished!");	
	//models.write(System.out,"TURTLE");
	//provModel.close();
	//System.out.println(i);
	}


	private static String cleanLine(String line) {
		line = line.replaceAll("com.bbn.tc.schema.avro.cdm18.","");
		line = line.replaceAll("#", "");
		line = line.replaceAll(" ", "_");
		String lineLastChar =  line.substring(line.length()-1);
		if(lineLastChar.equals(",")) {
			line = line.substring(0,line.length()-1);
		}
		return line;
	}
	
	private static Boolean filterLine(JSONObject jsonevent, ArrayList<String> fieldfilter) {
		Boolean result = false;
		
			String jsontype = (String) jsonevent.get("type");
			for (int i = 0; i < fieldfilter.size(); i++) {
				if(jsontype.equals(fieldfilter.get(i))) {
					result = true;
				}
			}
			
		return result;
		
	}


}
