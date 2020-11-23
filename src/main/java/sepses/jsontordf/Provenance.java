package sepses.jsontordf;

import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class Provenance {
	
	public static Model generateEventProvenance(Model masterModel, Model provModel, String provrule, String outputdir, String namegraph, String sparqlEp, String outputFile, String triplestore, String livestore) throws Exception{

			System.out.println("adding provenance model...");
			Model currentProvModel = generateProvenance(masterModel, provModel, provrule);

			//livestore
			if(livestore == "true") {
				//save prov model to rdf.. 	then store prov model to triplestore (uncomment this if you want live storing)
				String outputProvFile = outputFile+"_provenance"+".ttl";
				String provModelFile = Utility.saveToFile(currentProvModel,outputdir,outputProvFile);
				Utility.storeFileInRepo(triplestore,provModelFile, sparqlEp, namegraph+"_provenance", "dba", "dba");
				Utility.deleteFile(provModelFile);
				//clear prov tail
				Provenance.removeProvTail(sparqlEp);
			}

			//clear current event graph (remove event graphs that have already been generated as provenance ) 
			clearCurrentEventGraph(masterModel);
			
			System.out.println("done!");
			return currentProvModel;
			
	}
	
	private static Model generateProvenance(Model masterModel, Model provModel, String provrule) throws IOException, URISyntaxException {
				 
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
	        		
	        		"}";

	        QueryExecution provExec = QueryExecutionFactory.create(provQuery, masterModel);
	        Model currentProvModel = provExec.execConstruct();

	        // update fork to forks
	    	String queryForkToForks = "prefix darpa: <http://sepses.log/darpa#>\r\n" 
	    			+ "DELETE { ?s darpa:fork ?o.}  \r\n" + 
	    			  "INSERT { ?s darpa:forks ?o.}  \r\n" + 
	        		"WHERE { \r\n" + 
	        		  " ?s darpa:fork ?o.\r\n" + 
	        		  " }";
		    UpdateRequest f2fRequest = UpdateFactory.create(queryForkToForks);
		    UpdateAction.execute(f2fRequest,currentProvModel) ;
	        
	        
	        Model tempModel = currentProvModel.union(provModel);
	        
	        String forkQuery = "prefix darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"CONSTRUCT { ?s darpa:forks ?s2.}\r\n" + 
	        		"WHERE { \r\n" + 
	        		"    ?s darpa:hasObject ?o.\r\n" + 
	        		"     ?s2   darpa:hasSubject ?o.\r\n" + 
	        		" }";
	        
	        QueryExecution forkExec = QueryExecutionFactory.create(forkQuery, tempModel);
	        Model forkModel = forkExec.execConstruct();
	        tempModel.close();
	        currentProvModel.add(forkModel);
	    	        
	        	  	        
	        //remodeled the graph as used to be
	        //1. create a simple execution structure
	        System.out.println("fixing file execution relation..");
	        String execQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n " + 
	        		"INSERT {?bashA darpa:forks ?objx. ?objy darpa:isExecutedBy ?objx }\r\n" +
	        		"\r\n" + 
	        		"WHERE { \r\n" + 
	        		"   ?bashA darpa:forks ?bashB.\r\n" + 
	        		"   ?bashB darpa:execute ?objy.\r\n" + 
	        		"   ?bashA darpa:forks ?objx.\r\n" + 
	        		"   BIND (strafter(str(?objx),\"#\") as ?obja)\r\n" + 
	        		"   BIND (substr(str(?objy),strlen(str(?objy))-strlen(str(?obja))) as ?objb)\r\n"
	        		+  "BIND (concat(\"/\",str(?obja)) as ?objaa)" + 
	        		"   \r\n" + 
	        		"   FILTER (?objb = ?objaa )\r\n" + 
	        		"   \r\n" + 
	        		"}";
	        
	        UpdateRequest execRequest = UpdateFactory.create(execQuery);
	        UpdateAction.execute(execRequest,currentProvModel) ;
	      
	        //remove old exec
	        String oldExecQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n " + 
	        		"DELETE {?bashA darpa:forks ?bashB. ?bashB darpa:execute ?objy }\r\n" +
	        		"\r\n" + 
	        		"WHERE { \r\n" + 
	        		"   ?bashA darpa:forks ?bashB.\r\n" + 
	        		"   ?bashB darpa:execute ?objy.\r\n" + 
	        		"   ?bashA darpa:forks ?objx.\r\n" + 
	        		"   BIND (strafter(str(?objx),\"#\") as ?obja)\r\n" + 
	        		"   BIND (substr(str(?objy),strlen(str(?objy))-strlen(str(?obja))) as ?objb)\r\n"
	        		+ " BIND (concat(\"/\",str(?obja)) as ?objaa)" + 
	        		"   \r\n" + 
	        		"   FILTER (?objb = ?objaa )\r\n"
	        		+ "FILTER NOT EXISTS {?bashB darpa:forks ?proc}" + 
	        		"   \r\n" + 
	        		"}";
	        
	        UpdateRequest oldExecRequest = UpdateFactory.create(oldExecQuery);
	        UpdateAction.execute(oldExecRequest,currentProvModel) ;
	  
	     /*
	        //delete temporary Subject Relation, we cannot delete object relation because it could
  		    //be used to connect upcoming subject relation
  		    
  		    String delSubjQuery = "prefix darpa: <http://sepses.log/darpa#>\r\n" + 
  		    		"DELETE { ?s darpa:hasSubject ?o. } \r\n" + 
  		    		"where { \r\n" + 
  		    		"    ?s darpa:hasSubject ?o.\r\n" + 
  		    		"    \r\n" + 
  		    		" }";
  		    
  		    UpdateRequest delSubjUpdate = UpdateFactory.create(delSubjQuery);
  	        UpdateAction.execute(delSubjUpdate,currentProvModel) ;

	        
	        //1b. removed the old execution structure

	        String delOldExecQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE {?bashB darpa:execute ?objy  }\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"    ?bashB darpa:execute ?objy.\r\n" + 
	        		"   \r\n" + 
	        		"}";
	        
		    UpdateRequest delOldExecUpdate = UpdateFactory.create(delOldExecQuery);
		    UpdateAction.execute(delOldExecUpdate,currentProvModel) ;
	
		  

		    //2.a read (from ?s read ?o, to ?o readyBy ?s)
	        System.out.println("fixing read-write and send-receive relation..");
	        String readQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE { ?s darpa:read ?o  }\r\n" + 
	        		"INSERT { ?o darpa:isReadBy ?s }\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?s darpa:read ?o\r\n" + 
	        		"}\r\n" + 
	        		"";
	        UpdateRequest readExec = UpdateFactory.create(readQuery);
	        UpdateAction.execute(readExec,currentProvModel) ;
		    
	        
	        //2.b write (from ?s write ?o, to ?s writeTo ?o)
	        String writeQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE { ?s darpa:write ?o }\r\n" + 
	        		"INSERT { ?s darpa:writes ?o}\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?s darpa:write ?o\r\n" + 
	        		"}\r\n" + 
	        		"";
	        
		    UpdateRequest writeExec = UpdateFactory.create(writeQuery);
		    UpdateAction.execute(writeExec,currentProvModel) ;
		    
	        
	        //3a. send (from ?s sendto ?o, to ?s sendTo ?o)
	        String sendQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE {  ?s darpa:sendto ?o }\r\n" + 
	        		"INSERT {  ?s darpa:sends ?o }\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?s darpa:sendto ?o\r\n" + 
	        		"}\r\n" +  
	        		"";
	        
		    UpdateRequest sendExec = UpdateFactory.create(sendQuery);
		    UpdateAction.execute(sendExec,currentProvModel) ;
		    
	        
	        //3b. receive (from ?s recvfrom ?o, to ?o receivedBy ?s)
	        String receiveQuery = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
	        		"DELETE { ?s darpa:recvfrom ?o } \r\n" + 
	        		"INSERT { ?o darpa:isReceivedBy ?s }\r\n" + 
	        		"\r\n" + 
	        		"where { \r\n" + 
	        		"   ?s darpa:recvfrom ?o\r\n" + 
	        		"}\r\n" + 
	        		"";
	        
		    UpdateRequest receiveExec = UpdateFactory.create(receiveQuery);
		    UpdateAction.execute(receiveExec,currentProvModel) ;
		    */
		  System.out.println("run reasoning (provenance rule)..");
			Model deduction = JenaReasoner.parseRule(currentProvModel,provrule);
		 	currentProvModel.add(deduction);
			deduction.close();
	  return currentProvModel;

	}


	public static void clearCurrentEventGraph(Model masterModel) throws IOException{
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
