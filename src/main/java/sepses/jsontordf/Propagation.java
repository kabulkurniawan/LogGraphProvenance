package sepses.jsontordf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;

public class Propagation {
	
	protected static void generateFlagPropagation(Model provModel, Model currentProvModel, Model bgKnowledgeModel,  ArrayList<String>  confidentialdir, ArrayList<String> recognizedhost, String propagationrule,  String outputdir, String namegraph, String sparqlEp, String outputFile, String triplestore, String livestore ) throws Exception{
		generateInitialFlag(bgKnowledgeModel, currentProvModel, confidentialdir, recognizedhost);
		flagGenerationByPropagationRules(provModel, bgKnowledgeModel, propagationrule);	
	
		//livestore
		if(livestore == "true") {
			//save  model to rdf.. 	then store to triplestore
			String outputKnowledgeFile = outputFile+"_knowledge"+".ttl";
			String knowledgeModelFile = Utility.saveToFile(bgKnowledgeModel,outputdir,outputKnowledgeFile);
			Utility.storeFileInRepo(triplestore,knowledgeModelFile, sparqlEp, namegraph+"_knowledge", "dba", "dba");
			Utility.deleteFile(knowledgeModelFile);
		}
		
	}
	
	public static void flagGenerationByPropagationRules(Model provModel, Model bgknowledgemodel, String propagationrule ) throws URISyntaxException, IOException{
		Model tempModel = provModel.union(bgknowledgemodel);
		Model currentPropagatedFlag = JenaReasoner.parseRule(tempModel,propagationrule);
		if(!currentPropagatedFlag.isEmpty()) {
			bgknowledgemodel.add(currentPropagatedFlag);
			flagGenerationByPropagationRules(provModel, bgknowledgemodel, propagationrule);
		}
		tempModel.close();
	}
	
	public static void generateInitialFlag(Model bgKnowledgeModel, Model currentProvModel, ArrayList<String>  confidentialdir, ArrayList<String> recognizedhost ){	
		if(confidentialdir!=null) {
		 Model confFileFlagModel = generateConfidentialFileFlag(currentProvModel, confidentialdir); 
		 bgKnowledgeModel.add(confFileFlagModel);
		 confFileFlagModel.close();
		}
		if(recognizedhost!=null) {
		 Model lowIntegrityNetFlowObjectFlagModel = generatelowIntegrityNetFlowFlag(currentProvModel, recognizedhost); 
		 bgKnowledgeModel.add(lowIntegrityNetFlowObjectFlagModel);
		 lowIntegrityNetFlowObjectFlagModel.close();
		}
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
				"CONSTRUCT {?s a darpa:ConfidentialFile} \r\n WHERE { \r\n" + 
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
				"CONSTRUCT {?s a darpa:UntrustedHost} \r\n WHERE { \r\n" + 
				"	?s a darpa:NetFlowObject.\r\n" + 
				"    FILTER ( \r\n" + 
				    containString +
				"    )\r\n" + 
				"} \r\n";
	
		
		QueryExecution confFlagEx = QueryExecutionFactory.create(queryTemplate, model);
        Model confFlagModel = confFlagEx.execConstruct();
        return confFlagModel;
	
	}
	
	

}
