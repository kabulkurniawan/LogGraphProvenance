package sepses.jsontordf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rdfhdt.hdt.exceptions.ParserException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sepses.jsontordf.JSONRDFParser;

public class JsonReader {
	
	
	public static void readJson(String t, String filename, String l, String se, String ng, String sl, String outputdir, String inputdir, String rmldir, String rmlfile, String triplestore, String backupfile,  ArrayList<String> fieldfilter, String livestore, String provrule, String propagationrule, String alertrule, String bgknowledge, ArrayList<String> confidentialdir, ArrayList<String> recognizedhost) throws Exception {
	
		Model masterModel = ModelFactory.createDefaultModel();	
		Model provModel = ModelFactory.createDefaultModel();
		Model knowledgeModel = ModelFactory.createDefaultModel();
		Model alertModel = ModelFactory.createDefaultModel();
		org.eclipse.rdf4j.model.Model jmodels = new LinkedHashModel();
		
		//attackgraph preprocessing
		AttackGraphGeneration.preprocessingAttackGraph(masterModel, provModel, knowledgeModel, alertModel,ng, inputdir);
		
		Integer lineNumber = 1; // 1 here means the minimum line to be extracted
		if(l!=null) {lineNumber=Integer.parseInt(l);}
		String sparqlEp = se;
		String namegraph = ng;
		Integer startingLine = 1; // 1 means start from the beginning
		if(sl!=null) {startingLine=Integer.parseInt(sl);}
		
		
		//JSONParser jsparser = new JSONParser(); 
		JSONObject alljsObj = new JSONObject();
		JSONArray alljson = new JSONArray();
		//JSONObject json = new JSONObject();
		
		// create in one json object
		Integer countLine=0;
		Integer templ = 0;
		Integer group=0;

		
		
		//initiate json file
		
		InputStream jf = new FileInputStream(filename);
		BufferedReader in = new BufferedReader(new InputStreamReader(jf));	
		
		while (in.ready()) {
		
			String line = in.readLine();
			//System.out.println(countLine);
			if (countLine.equals(startingLine)) {
					
				System.out.println("reading from line : "+ startingLine);
				System.out.print("parse to json ");
					group=((int) Math.ceil((startingLine-1)/lineNumber));
			}
			if(countLine >= startingLine) {
				line = cleanLine(line); // sometimes the data should be cleaned first
				//skip strange character inside line
				try {
					ObjectMapper objectMapper = new ObjectMapper();
					ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(line); 
					JsonNode jsondatum =  jsonNode.get("datum");
					JsonNode jsonevent = jsondatum.get("Event");
				
					if(jsonevent!= null) {
						if(!filterLine(jsonevent, fieldfilter)){
							jsonNode = jsonMapper.constructSimpleEventJson(jsonNode);
							 alljson.add(jsonNode);	
						}
					} else {

						if(!jsonNode.path("datum").path("FileObject").isEmpty()) {
							//System.out.println(jsonNode);	
							jsonNode =jsonMapper.constructSimpleFileJson(jsonNode);
						}else if(!jsonNode.path("datum").path("Subject").isEmpty()) {
							//System.out.println(jsonNode);	
							jsonNode =jsonMapper.constructSimpleSubjectJson(jsonNode);
						} else {
							jsonNode = jsonNode; 
						}
						
						alljson.add(jsonNode);
					}
				} catch (Exception e) {
					System.out.print("strange character skipped => ");
					System.out.println(line);
				}
				templ++;
		
				if(templ.equals(lineNumber)) {
					group++;
					System.out.print("parsing "+group+" of "+lineNumber+" json to rdf...");
					alljsObj.put("logEntry", alljson); // add this term to group as one json object
					JSONRDFParser jps = new JSONRDFParser(rmldir+rmlfile);
					jmodels = jps.Parse(alljsObj.toJSONString());
					
					//saving main model & combine to prev model...	
					System.out.println("saving main and combine model...");
					String outputFile = Utility.getOriginalFileName(filename)+"_"+lineNumber+"_"+group+".ttl";
					String outputFileName = Utility.saveRDF4JModel(jmodels,outputdir,outputFile);
					jmodels.clear();
					Model masterModelTemp = RDFDataMgr.loadModel(outputFileName) ;
					masterModel.add(masterModelTemp);
					masterModelTemp.close();
					/*store main model (if we need to store the parsed rdf log lines, uncomment the line bellow!)*/
					//Utility.storeFileInRepo(triplestore,outputFileName, sparqlEp, namegraph, "dba", "dba");
					Utility.deleteFile(outputFileName);
					
					//run attack graph generation!
					AttackGraphGeneration.generateAttackGraph(masterModel, provModel, knowledgeModel, alertModel,  provrule,  bgknowledge,propagationrule, alertrule, confidentialdir, recognizedhost,outputdir, namegraph, sparqlEp, outputFileName, triplestore, livestore);
					
					alljson.clear();
					alljsObj.clear();
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
	
		//saving main model & combine to prev model...	
		System.out.println("saving main and combine model...");
		String outputFile = Utility.getOriginalFileName(filename)+"_"+templ+".ttl";
		String outputFileName = Utility.saveRDF4JModel(jmodels,outputdir,outputFile);
		jmodels.clear();
		Model masterModelTemp = RDFDataMgr.loadModel(outputFileName) ;
		masterModel.add(masterModelTemp);
		masterModelTemp.close();
		/*store main model (if we need to store the parsed rdf log lines, uncomment the line bellow!)*/
		//Utility.storeFileInRepo(triplestore,outputFileName, sparqlEp, namegraph, "dba", "dba");
		Utility.deleteFile(outputFileName);
		
		//run attack graph generation!
		AttackGraphGeneration.generateAttackGraph(masterModel, provModel, knowledgeModel, alertModel,  provrule,  bgknowledge,propagationrule, alertrule, confidentialdir, recognizedhost,outputdir, namegraph, sparqlEp, outputFileName, triplestore, livestore);
		
		alljson.clear();
		alljsObj.clear();
		templ=0;
	}
	
	//post processing attack graph e.g. store to rdf file, .hdt
	AttackGraphGeneration.postProcessingAttackGraph(masterModel, provModel, knowledgeModel, alertModel,namegraph, inputdir, outputdir, bgknowledge, livestore, triplestore, sparqlEp);	
	
	System.out.println("Finished!");	
	
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
	
	private static Boolean filterLine(JsonNode jsonevent, ArrayList<String> fieldfilter) {
		Boolean result = false;
		
		String jsontype = jsonevent.get("type").asText();
			for (int i = 0; i < fieldfilter.size(); i++) {
				if(jsontype.equals(fieldfilter.get(i))) {
					result = true;
				}
			}
			
		return result;
		
	}
	
	
}
