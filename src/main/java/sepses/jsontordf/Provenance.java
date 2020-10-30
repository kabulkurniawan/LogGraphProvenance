package sepses.jsontordf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Provenance {
	public Provenance() {
		// TODO Auto-generated constructor stub
	}
	
	public static HashMap<String, String> generateEventProvenance(Model masterModel, Model provModel, String outputdir, String inputdir, String outputFileName, HashMap<String, String> hdtOutput, String namegraph, String sparqlEp, String outputFile, String backupfile, String triplestore, String initRdfFile ) throws Exception{
		//first load the previous.hdt file and combine them with the new generated model
		
		
		String outputFileHDTTemp = hdtOutput.get("master");
		String outputFileProvHDTTemp = hdtOutput.get("prov");
		
		masterModel = RDFDataMgr.loadModel(outputFileName) ;
		
		String outputFileHDT = outputdir+Utility.getOriginalFileName(outputFileName)+".hdt";
		String outputFileProvHDT = outputdir+Utility.getOriginalFileName(outputFileName)+"_prov.hdt";
		
		
		//check if previous created hdt model exists, if yes then combine!
		//System.out.println("outputfileHDTTemp =>"+outputFileHDTTemp);
		
		File hdtFilePath = new File(outputFileHDTTemp);
		if(hdtFilePath.exists()) {
			System.out.println("master hdtFile exists!");
			Model prevMasterModel = Utility.loadHDTToJenaModel(outputFileHDTTemp);
			masterModel.add(prevMasterModel);
			prevMasterModel.close();
			//when loading hdt file, this file is created then it's better to remove it
			//Utility.deleteFile(outputFileHDTTemp);
			Utility.deleteFile(outputFileHDTTemp+".index.v1-1");		
		}
		
		//not sure how to say, but hdt file need an initial empty file to load and combine the model
		//test.ttl must be available in the input dir!
		provModel = RDFDataMgr.loadModel(initRdfFile) ;
		
		//System.out.println("outputFileProvHDTTemp =>"+outputFileProvHDTTemp);
		
		File hdtProvFilePath = new File(outputFileProvHDTTemp);
		if(hdtProvFilePath.exists()) {
			System.out.println("prov hdtFile exists!");
			Model prevProvModel = Utility.loadHDTToJenaModel(outputFileProvHDTTemp);
			provModel.add(prevProvModel);
			prevProvModel.close();
			//Utility.deleteFile(outputFileProvHDTTemp);
			Utility.deleteFile(outputFileProvHDTTemp+".index.v1-1");
		}
	
			
			//saving prov model
			System.out.println("adding provenance model...");
	
			Model currentProvModel = generateProvenanceFromHDT(masterModel, provModel);
			
			System.out.println("save provmodel to rdf file...");
			String outputProvFile = outputFile+"_prov"+".ttl";
			String provModelFile = Utility.saveToFile(currentProvModel,outputdir,outputProvFile);
			
			//generate hdt file for provenance
			System.out.println("save provmodel to hdt...."+outputFileProvHDT);
			Utility.generateHDTFile(namegraph, provModelFile, "TURTLE", outputFileProvHDT);
			hdtOutput.put("prov", outputFileProvHDT);
			
			
			//clear current event graph (remove event graphs that have already been generated as provenance ) 
			clearCurrentEventGraph(masterModel);
			
			//generate hdt file for masterModel
			System.out.println("save mastermodel to rdf file...");
			String masterModelFile = Utility.saveToFile(masterModel,outputdir,outputFile);
			System.out.println("save mastermodel to hdt..."+outputFileHDT);
			Utility.generateHDTFile(namegraph, masterModelFile, "TURTLE", outputFileHDT);
			hdtOutput.put("master", outputFileHDT);
			
		   // store prov model
			Utility.storeFileInRepo(triplestore,provModelFile, sparqlEp, namegraph+"_prov", "dba", "dba");
			
			//clear prov tail
			System.out.println("remove provenance tail..."+outputFileHDT);
			Provenance.removeProvTail(sparqlEp);
			
			
			
		if(backupfile=="false"){
				System.out.print("delete prov file..");
				Utility.deleteFile(provModelFile);
				System.out.println("done!");
			}
		System.out.println("done!");

		
		return hdtOutput;
	}
	
	private static Model generateProvenanceFromHDT(Model masterModel, Model provModel) throws IOException {
		
		
		
		 String provQuery = "prefix cl: <http://sepses.log/coreLog#>\r\n" + 
	        		"prefix darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"\r\n" + 
	        		"CONSTRUCT {?subj0 ?action ?objf. ?subj0 darpa:hasSubject ?subj. ?subj0 darpa:hasObject ?obj. ?subj0 ?action ?objh. ?objf a ?tpo.?objh a ?tpo. ?subj0 a ?tps}\r\n" + 
	        		"WHERE {  \r\n" + 
	        		"    ?s a darpa:Event.\r\n" + 
	        		"    ?s darpa:subject ?subj.\r\n"
	        		+    "?subj a ?tps.\r\n"+ 
	        		"    ?s darpa:exec ?ex.\r\n" + 
	        		"    ?s darpa:eventAction ?ae.\r\n" + 
	        		"    ?s darpa:predicateObject ?po.\r\n"
	        		+    "?po a ?tpo." + 
	        		"OPTIONAL {?s darpa:predicateObjectPath ?pop.}\r\n" + 
	        		"OPTIONAL {?po darpa:remoteAddress ?ra.}\r\n" + 
	        		"OPTIONAL {?po darpa:remotePort ?rp.}\r\n" + 
	        		"    BIND (uri(concat(\"http://sepses.res/darpa/proc/\",concat(concat(substr(str(?subj),32,37),\"#\"),str(?ex)))) as ?subj0).\r\n" + 
	        		"    BIND (uri(concat(\"http://sepses.log/darpa#\", lcase(substr(str(?ae),7,20)))) as ?action).\r\n" + 
	        		"    BIND (uri(concat(\"http://sepses.res/darpa/obj#\",str(?pop))) as ?objf).\r\n" + 
	        		"    #WITH PORT\r\n" + 
	        		"    BIND (uri(concat(\"http://sepses.log/darpa/obj#\",concat(concat(str(?ra),\":\"),str(?rp)))) as ?objh).\r\n"
	        		+  " BIND (exists{?s darpa:eventAction \"EVENT_FORK\".} AS ?forkExist)\r\n" + 
	        		"     BIND (IF(?forkExist, ?po,?pup) AS ?obj)" + 
	        		"   \r\n" + 
	        		"    FILTER NOT EXISTS{?s darpa:eventAction \"EVENT_WRITE\". ?s darpa:predicateObjectPath \"<unknown>\"}\r\n" + 
	        		"    FILTER NOT EXISTS{?s darpa:eventAction \"EVENT_READ\". ?s darpa:predicateObjectPath \"<unknown>\"}\r\n" + 
	        		"   # FILTER (?ae !=\"EVENT_EXECUTE\")\r\n" + 
	        		"   # FILTER (?ae !=\"EVENT_RECVFROM\")\r\n" + 
	        		"   # FILTER (?ae !=\"EVENT_READ\")\r\n" + 
	        		"   #FILTER (?ae !=\"EVENT_WRITE\")\r\n" + 
	        		"}";
//	        System.out.print(query);
//	        System.exit(0);
	        QueryExecution provExec = QueryExecutionFactory.create(provQuery, masterModel);
	        Model currentProvModel = provExec.execConstruct();
	        provModel.add(currentProvModel);
	        
	        String forkQuery = "prefix darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"CONSTRUCT { ?s darpa:fork ?s2.}\r\n" + 
	        		"WHERE { \r\n" + 
	        		"    ?s darpa:hasObject ?o.\r\n" + 
	        		"     ?s2   darpa:hasSubject ?o.\r\n" + 
	        		" }";
	        
	        QueryExecution forkExec = QueryExecutionFactory.create(forkQuery, provModel);
	        Model forkModel = forkExec.execConstruct();
	        
	        provModel.add(forkModel);
	    
	        //delete temporary Subject Relation, we cannot delete object relation because it could
	        //be used to connect upcoming subject relation
	        
	        
	        String delSubjQuery = "prefix darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE { ?s darpa:hasSubject ?o. } \r\n" + 
	        		"where { \r\n" + 
	        		"    ?s darpa:hasSubject ?o.\r\n" + 
	        		"    \r\n" + 
	        		" }";
	        
	        UpdateRequest delSubjUpdate = UpdateFactory.create(delSubjQuery);
	        UpdateAction.execute(delSubjUpdate,provModel) ;
	        
	        
	        
	        //remodeled the graph as used to be
	        //1. create a simple execution structure
	        System.out.println("fixing file execution relation..");
	        String execQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n " + 
	        		" DELETE {?bashA darpa:fork ?bashB. ?bashB darpa:execute ?objy }\r\n" +
	        		"INSERT {?bashA darpa:fork ?objx. ?objy darpa:executedBy ?objx }\r\n" +
	        		"\r\n" + 
	        		"WHERE { \r\n" + 
	        		"   ?bashA darpa:fork ?bashB.\r\n" + 
	        		"   ?bashB darpa:execute ?objy.\r\n" + 
	        		"   ?bashA darpa:fork ?objx.\r\n" + 
	        		"   BIND (strafter(str(?objx),\"#\") as ?obja)\r\n" + 
	        		"   BIND (substr(str(?objy),strlen(str(?objy))-strlen(str(?obja))) as ?objb)\r\n"
	        		+ "BIND (concat(\"/\",str(?obja)) as ?objaa)" + 
	        		"   \r\n" + 
	        		"   FILTER (?objb = ?objaa )\r\n" + 
	        		"   \r\n" + 
	        		"}";
	        
	        //System.out.print(execQuery);
	        UpdateRequest execRequest = UpdateFactory.create(execQuery);
	        UpdateAction.execute(execRequest,provModel) ;
	      
	        //1b. removed the old execution structure

	        String delOldExecQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE {?bashB darpa:execute ?objy  }\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?bashB darpa:execute ?objy.\r\n" + 
	        		"   \r\n" + 
	        		"}";
	        
		    UpdateRequest delOldExecUpdate = UpdateFactory.create(delOldExecQuery);
		    UpdateAction.execute(delOldExecUpdate,provModel) ;
		    
	
	        //2.a read (from ?s read ?o, to ?o readyBy ?s)
	        System.out.println("fixing read-write and send-receive relation..");
	        String readQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE { ?s darpa:read ?o  }\r\n" + 
	        		"INSERT { ?o darpa:readBy ?s }\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?s darpa:read ?o\r\n" + 
	        		"}\r\n" + 
	        		"";
	        UpdateRequest readExec = UpdateFactory.create(readQuery);
	        UpdateAction.execute(readExec,provModel) ;
		    
	        
	        //2.b write (from ?s write ?o, to ?s writeTo ?o)
	        String writeQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE { ?s darpa:write ?o }\r\n" + 
	        		"INSERT { ?s darpa:writeTo ?o}\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?s darpa:write ?o\r\n" + 
	        		"}\r\n" + 
	        		"";
	        
		    UpdateRequest writeExec = UpdateFactory.create(writeQuery);
		    UpdateAction.execute(writeExec,provModel) ;
		    
	        
	        //3a. send (from ?s sendto ?o, to ?s sendTo ?o)
	        String sendQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE {  ?s darpa:sendto ?o }\r\n" + 
	        		"INSERT {  ?s darpa:sendTo ?o }\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?s darpa:sendto ?o\r\n" + 
	        		"}\r\n" + 
	        		"";
	        
		    UpdateRequest sendExec = UpdateFactory.create(sendQuery);
		    UpdateAction.execute(sendExec,provModel) ;
		    
	        
	        //3b. receive (from ?s recvfrom ?o, to ?o receivedBy ?s)
	        String receiveQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE { ?s darpa:recvfrom ?o } \r\n" + 
	        		"INSERT { ?o darpa:receivedBy ?s }\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?s darpa:recvfrom ?o\r\n" + 
	        		"}\r\n" + 
	        		"";
	        
		    UpdateRequest receiveExec = UpdateFactory.create(receiveQuery);
		    UpdateAction.execute(receiveExec,provModel) ;
		    //provModel.write(System.out,"TURTLE");
	  return provModel;

	}
	
	private static void generateProvenance(String sparqlEp, String graphname) throws FileNotFoundException {
		   
		//only for graphdb
		sparqlEp = sparqlEp+"/statements";
	       
		    
		   String query = "prefix cl: <http://sepses.log/coreLog#>\r\n" + 
		        		"prefix darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"\r\n" + 
		        		"INSERT {GRAPH <"+graphname+"_prov> { ?subj0 ?action ?objf. ?subj0 darpa:hasSubject ?subj. ?subj0 darpa:hasObject ?obj. ?subj0 ?action ?objh. ?objf a ?tpo.?objh a ?tpo. ?subj0 a ?tps}} \r\n" + 
		        		"WHERE { GRAPH <"+graphname+"> { \r\n" + 
		        		"    ?s a darpa:Event.\r\n" + 
		        		"    ?s darpa:subject ?subj.\r\n"
		        		+    "?subj a ?tps.\r\n"+ 
		        		"    ?s darpa:exec ?ex.\r\n" + 
		        		"    ?s darpa:eventAction ?ae.\r\n" + 
		        		"    ?s darpa:predicateObject ?po.\r\n"
		        		+    "?po a ?tpo." + 
		        		"OPTIONAL {?s darpa:predicateObjectPath ?pop.}\r\n" + 
		        		"OPTIONAL {?po darpa:remoteAddress ?ra.}\r\n" + 
		        		"OPTIONAL {?po darpa:remotePort ?rp.}\r\n" + 
		        		"    BIND (uri(concat(\"http://sepses.res/darpa/proc/\",concat(concat(substr(str(?subj),32,37),\"#\"),str(?ex)))) as ?subj0).\r\n" + 
		        		"    BIND (uri(concat(\"http://sepses.log/darpa#\", lcase(substr(str(?ae),7,20)))) as ?action).\r\n" + 
		        		"    BIND (uri(concat(\"http://sepses.res/darpa/obj#\",str(?pop))) as ?objf).\r\n" + 
		        		"    #WITH PORT\r\n" + 
		        		"    BIND (uri(concat(\"http://sepses.log/darpa/obj#\",concat(concat(str(?ra),\":\"),str(?rp)))) as ?objh).\r\n"
		        		+  " BIND (exists{?s darpa:eventAction \"EVENT_FORK\".} AS ?forkExist)\r\n" + 
		        		"     BIND (IF(?forkExist, ?po,?pup) AS ?obj)" + 
		        		"   \r\n" + 
		        		"    FILTER NOT EXISTS{?s darpa:eventAction \"EVENT_WRITE\". ?s darpa:predicateObjectPath \"<unknown>\"}\r\n" + 
		        		"    FILTER NOT EXISTS{?s darpa:eventAction \"EVENT_READ\". ?s darpa:predicateObjectPath \"<unknown>\"}\r\n" + 
		        		"   # FILTER (?ae !=\"EVENT_EXECUTE\")\r\n" + 
		        		"   # FILTER (?ae !=\"EVENT_RECVFROM\")\r\n" + 
		        		"   # FILTER (?ae !=\"EVENT_READ\")\r\n" + 
		        		"   #FILTER (?ae !=\"EVENT_WRITE\")\r\n" + 
		        		"}}";
//		        System.out.print(query);
//		        System.exit(0);
		        UpdateRequest qexec = UpdateFactory.create(query);
		        UpdateProcessor  proc = UpdateExecutionFactory.createRemote(qexec,sparqlEp);
		        proc.execute();
		        
		        //create forkConnection
		        
		        String query2 = "prefix darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"INSERT { GRAPH <"+graphname+"_prov> { ?s darpa:fork ?s2.}}\r\n" + 
		        		"WHERE { GRAPH <"+graphname+"_prov> { \r\n" + 
		        		"    ?s darpa:hasObject ?o.\r\n" + 
		        		"     ?s2   darpa:hasSubject ?o.\r\n" + 
		        		" }}";
		        
		        UpdateRequest qexec2 = UpdateFactory.create(query2);
		        UpdateProcessor  proc2 = UpdateExecutionFactory.createRemote(qexec2,sparqlEp);
		        proc2.execute();
		        
		        //delete temporary Subject Relation, we cannot delete object relation because it could
		        //be used to connect upcoming subject relation
		        
		        
		        String query4 = "prefix darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"DELETE { GRAPH <"+graphname+"_prov> { ?s darpa:hasSubject ?o. }} \r\n" + 
		        		"where { GRAPH <"+graphname+"_prov> {\r\n" + 
		        		"    ?s darpa:hasSubject ?o.\r\n" + 
		        		"    \r\n" + 
		        		" }}";
		        
		        UpdateRequest qexec4 = UpdateFactory.create(query4);
		        UpdateProcessor  proc4 = UpdateExecutionFactory.createRemote(qexec4,sparqlEp);
		        proc4.execute();
		        
		        //clear current event graph
		        
			    String query5 = "prefix darpa: <http://sepses.log/darpa#>\r\n" 
			    		+ "DELETE { GRAPH <"+graphname+"> {?s ?p ?o.}}  \r\n" + 
		        		"WHERE { GRAPH <"+graphname+"> {\r\n" + 
		        		"  ?s a darpa:Event.\r\n"
		        		+ " ?s ?p ?o.\r\n" + 
		        		" }}";
		        
			    UpdateRequest qexec5 = UpdateFactory.create(query5);
		        UpdateProcessor  proc5 = UpdateExecutionFactory.createRemote(qexec5,sparqlEp);
		        proc5.execute();
		        
		        //remodeled the graph as used to be
		        //1. create a simple execution structure
		        System.out.println("fixing file execution relation..");
		        String query6 = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"INSERT { GRAPH <"+graphname+"_prov> {?objy darpa:executedBy ?objx }}\r\n" + 
		        		"\r\n" + 
		        		"where { \r\n" + 
		        		"   ?bashA darpa:fork ?bashB.\r\n" + 
		        		"   ?bashB darpa:execute ?objy.\r\n" + 
		        		"   ?bashA darpa:fork ?objx.\r\n" + 
		        		"   BIND (strafter(str(?objx),\"#\") as ?obja)\r\n" + 
		        		"   BIND (substr(str(?objy),strlen(str(?objy))-strlen(str(?obja))+1) as ?objb)\r\n" + 
		        		"   \r\n" + 
		        		"   FILTER (?objb = ?obja )\r\n" + 
		        		"   \r\n" + 
		        		"}";
		        
			    UpdateRequest qexec6 = UpdateFactory.create(query6);
		        UpdateProcessor  proc6 = UpdateExecutionFactory.createRemote(qexec6,sparqlEp);
		        proc6.execute();
		        
		      //1b. removed the old execution structure
		        String query6b = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"DELETE { GRAPH <"+graphname+"_prov> {?bashA darpa:fork ?bashB. ?bashB darpa:execute ?objy } }\r\n" + 
		        		"\r\n" + 
		        		"where { \r\n" + 
		        		"   ?bashA darpa:fork ?bashB.\r\n" + 
		        		"   ?bashB darpa:execute ?objy.\r\n" + 
		        		"   \r\n" + 
		        		"}";
		        
			    UpdateRequest qexec6b = UpdateFactory.create(query6b);
		        UpdateProcessor  proc6b = UpdateExecutionFactory.createRemote(qexec6b,sparqlEp);
		        proc6b.execute();
		        
		        
		        
		        //2.a read (from ?s read ?o, to ?o readyBy ?s)
		        System.out.println("fixing read-write and send-receive relation..");
		        String query7 = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"DELETE { GRAPH <"+graphname+"_prov> {?s darpa:read ?o } }\r\n" + 
		        		"INSERT { GRAPH <"+graphname+"_prov>{?o darpa:readBy ?s }}\r\n" + 
		        		"\r\n" + 
		        		"where { \r\n" + 
		        		"   ?s darpa:read ?o\r\n" + 
		        		"}\r\n" + 
		        		"";
		        UpdateRequest qexec7 = UpdateFactory.create(query7);
		        UpdateProcessor  proc7 = UpdateExecutionFactory.createRemote(qexec7,sparqlEp);
		        proc7.execute();
		        
		        //2.b write (from ?s write ?o, to ?s writeTo ?o)
		        String query7a = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"DELETE { GRAPH <"+graphname+"_prov> {?s darpa:write ?o } }\r\n" + 
		        		"INSERT { GRAPH <"+graphname+"_prov>{?s darpa:writeTo ?o }}\r\n" + 
		        		"\r\n" + 
		        		"where { \r\n" + 
		        		"   ?s darpa:write ?o\r\n" + 
		        		"}\r\n" + 
		        		"";
		        
			    UpdateRequest qexec7a = UpdateFactory.create(query7a);
		        UpdateProcessor  proc7a = UpdateExecutionFactory.createRemote(qexec7a,sparqlEp);
		        proc7a.execute();
		        
		        //3a. send (from ?s sendto ?o, to ?s sendTo ?o)
		        String query8 = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"DELETE { GRAPH <"+graphname+"_prov> {?s darpa:sendto ?o } }\r\n" + 
		        		"INSERT { GRAPH <"+graphname+"_prov> {?s darpa:sendTo ?o }}\r\n" + 
		        		"\r\n" + 
		        		"where { \r\n" + 
		        		"   ?s darpa:sendto ?o\r\n" + 
		        		"}\r\n" + 
		        		"";
		        
			    UpdateRequest qexec8 = UpdateFactory.create(query8);
		        UpdateProcessor  proc8 = UpdateExecutionFactory.createRemote(qexec8,sparqlEp);
		        proc8.execute();
		        
		        //3b. receive (from ?s recvfrom ?o, to ?o receivedBy ?s)
		        String query9 = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
		        		"DELETE { GRAPH <"+graphname+"_prov> {?s darpa:recvfrom ?o } }\r\n" + 
		        		"INSERT { GRAPH <"+graphname+"_prov> {?o darpa:receivedBy ?s }}\r\n" + 
		        		"\r\n" + 
		        		"where { \r\n" + 
		        		"   ?s darpa:recvfrom ?o\r\n" + 
		        		"}\r\n" + 
		        		"";
		        
			    UpdateRequest qexec9 = UpdateFactory.create(query9);
		        UpdateProcessor  proc9 = UpdateExecutionFactory.createRemote(qexec9,sparqlEp);
		        proc9.execute();
		        
		   
	   }


	private static Model generateIOCProvenance(String sparqlEp,String namegraph,String modelFile, JSONObject iocJson) throws FileNotFoundException {
		JSONArray fileObject = (JSONArray) iocJson.get("fileObject");
		JSONArray netFlowObject = (JSONArray) iocJson.get("remoteAddress");
		
		 Model models = ModelFactory.createDefaultModel();
		 Model provModelTemp = ModelFactory.createDefaultModel();
		 Model provModel = ModelFactory.createDefaultModel();
	        //models.setNsPrefixes(Utility.getPrefixes());
	        InputStream tempInput = new FileInputStream(modelFile);
	        RDFDataMgr.read(models, tempInput, Lang.TURTLE);
	        
	        //VirtGraph set = new VirtGraph(namegraph,"jdbc:virtuoso://localhost:1111", "dba", "dba");
			

		for(int i=0;i<fileObject.size();i++) {
			
			String query = "prefix darpa: <http://sepses.log/darpa#>\r\n" + 
					"    SELECT distinct ?subj where {\r\n" + 
					"          ?s a darpa:Event.\r\n" + 
					"          ?s darpa:subject ?subj.\r\n" + 
					"         ?s darpa:predicateObjectPath \""+fileObject.get(i)+"\"\r\n" + 
					"    }";
			QueryExecution qexec = QueryExecutionFactory.create(query, models);
			 
			
			
			 ResultSet result = qexec.execSelect();
			 while (result.hasNext())
		       {
		         QuerySolution soln = result.nextSolution() ;
		         String subj = soln.get("subj").toString() ; 
		         //System.out.println(subj);
		         //create provenance
		         String constructQ = "prefix darpa: <http://sepses.log/darpa#>\r\n" + 
		         		"CONSTRUCT {?ps darpa:prov true} where {\r\n" + 
		         		"    <"+subj+"> darpa:parentSubject* ?ps. \r\n" + 
		         		"    }";
		         
		         //VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(constructQ, set);
		         QueryExecution qexec2 = QueryExecutionFactory.sparqlService(sparqlEp, constructQ);
		          provModelTemp = qexec2.execConstruct();
		          provModel.add(provModelTemp);
		          provModelTemp.close();
		           
		         
		         
		       }
			 
		}
		
		

		return provModel;
		
		
		//System.exit(0);
		
	}



	public static void clearCurrentEventGraph(Model masterModel) throws IOException{
		
        //clear current event graph
        
	    String eventQuery = "prefix darpa: <http://sepses.log/darpa#>\r\n" 
	    		+ "DELETE { ?s ?p ?o.}  \r\n" + 
        		"WHERE { \r\n" + 
        		"  ?s a darpa:Event.\r\n"
        		+ " ?s ?p ?o.\r\n" + 
        		" }";
        
	    UpdateRequest eventExec = UpdateFactory.create(eventQuery);
	    UpdateAction.execute(eventExec,masterModel) ;
	    
	}


	public static void removeProvTail(String sparqlEp) throws IOException{
		
        //clear current event graph
        
	    String removeTailQuery = "prefix darpa: <http://sepses.log/darpa#>\r\n" 
	    		+ "DELETE { ?s darpa:hasObject ?o.}  \r\n" + 
        		"WHERE { \r\n" 
        		+ " ?s darpa:hasObject ?o.\r\n" + 
        		" }";
        
	    UpdateRequest removeTailReq = UpdateFactory.create(removeTailQuery);
	    UpdateProcessor proc = UpdateExecutionFactory.createRemote(removeTailReq, sparqlEp+"/statements");
	    proc.execute();
	    
	}
	

	
}
