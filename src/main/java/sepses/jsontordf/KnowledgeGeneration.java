package sepses.jsontordf;


import java.util.ArrayList;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;

public class KnowledgeGeneration {
	
	public static Model generateInitialFlag(Model currentProvModel, ArrayList<String>  confidentialdir, ArrayList<String> recognizedhost ){	
		Model confFileFlagModel = generateConfidentialFileFlag(currentProvModel, confidentialdir); 
		Model lowIntegrityNetFlowObjectFlagModel = generatelowIntegrityNetFlowFlag(currentProvModel, recognizedhost); 
		confFileFlagModel.add(lowIntegrityNetFlowObjectFlagModel);	
		return confFileFlagModel;
	}
	
	public static Model generateConfidentialFileFlag(Model model, ArrayList<String> confidentialdir){
		String containString ="";
		for (int i=0;i<confidentialdir.size();i++) {
			if(containString!="") {
				containString+=" || \r\n";
			}
			containString+="contains(str(?s),\""+confidentialdir.get(i)+"\")";
		}
		
		String queryTemplate = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
				"CONSTRUCT {?s darpa:hasConfidentiality \"high\"} \r\n WHERE { \r\n" + 
				"	?s a darpa:FileObject.\r\n" + 
				"    FILTER ( \r\n" + 
				    containString +
				"    )\r\n" + 
				"} \r\n";
	
		
		QueryExecution confFlagEx = QueryExecutionFactory.create(queryTemplate, model);
        Model confFlagModel = confFlagEx.execConstruct();
        return confFlagModel;
	
	}
	
	
	public static Model generatelowIntegrityNetFlowFlag(Model model, ArrayList<String> recognizedhost){
		
		String containString ="";	
		for (int i=0;i<recognizedhost.size();i++) {
			if(containString!="") {
				containString+=" || \r\n";
			}
			containString+="contains(str(?s),\""+recognizedhost.get(i)+"\")";
		}
		
		String queryTemplate = "PREFIX darpa: <http://sepses.log/darpa#>\r\n" + 
				"CONSTRUCT {?s darpa:hasIntegrity \"low\"} \r\n WHERE { \r\n" + 
				"	?s a darpa:NetFlowObject.\r\n" + 
				"    FILTER NOT EXISTS {FILTER ( \r\n" + 
				    containString +
				"    )}\r\n" + 
				"} \r\n";
	
		
		QueryExecution confFlagEx = QueryExecutionFactory.create(queryTemplate, model);
        Model confFlagModel = confFlagEx.execConstruct();
        return confFlagModel;
	
	}
	
}