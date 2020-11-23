package sepses.jsontordf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class AlertDetection {
	
	public AlertDetection() {
		// TODO Auto-generated constructor stub
	}
	
	
	public static void generateAlert(Model provModel, Model bgKnowledgeModel, Model alertModel, String alertrule,  String outputdir, String namegraph, String sparqlEp, String outputFile, String triplestore, String livestore) throws Exception {
		System.out.println("generateAlert");
		Model tempModel = provModel.union(bgKnowledgeModel);
		Model currentAlerts = JenaReasoner.parseRule(tempModel,alertrule);
		alertModel.add(currentAlerts);
		
		//livestore
		if(livestore == "true") {
			//save  model to rdf.. 	then store to triplestore
			String outputAlertFile = outputFile+"_alert"+".ttl";
			String alertModelFile = Utility.saveToFile(alertModel,outputdir,outputAlertFile);
			Utility.storeFileInRepo(triplestore,alertModelFile, sparqlEp, namegraph+"_alert", "dba", "dba");
			Utility.deleteFile(alertModelFile);
		}
	    currentAlerts.close();
		
	}
	
	public static Model generateAlertFromShacl(Model model, Model bgmodel, String ruledir, Model alerts) throws FileNotFoundException{
		
				System.out.println("generate alert...");
				Model tempmodel = model.union(bgmodel).union(alerts);
				Model currentAlerts = ModelFactory.createDefaultModel();
				
				File ruled = new File(ruledir);
				ArrayList<String> rulefiles = Utility.listFilesForFolder(ruled);
				for(int i=0;i<rulefiles.size();i++) {
					//System.out.print(ruledir+rulefiles.get(i));
					Model alert =  Utility.executeRule(ruledir+rulefiles.get(i), tempmodel);
					currentAlerts.add(alert);
				}
				
				tempmodel.close();
				
				return currentAlerts;			
	}
	
}
