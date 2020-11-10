package sepses.jsontordf;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

public class AlertDetection {
	
	public AlertDetection() {
		// TODO Auto-generated constructor stub
	}
	
	public static Model generateAlert(Model model, Model bgmodel, String ruledir, Model alerts) throws FileNotFoundException{
		
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
