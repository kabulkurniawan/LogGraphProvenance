package sepses.jsontordf;


import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

public class JenaReasoner {
	public JenaReasoner() {
		// TODO Auto-generated constructor stub
	}
	public static Model parseRule(Model model, String rulefile) throws URISyntaxException, IOException{
			List<Rule> rules = Rule.rulesFromURL(rulefile);
			Reasoner reasoner = new GenericRuleReasoner(rules);
			InfModel infModel = ModelFactory.createInfModel(reasoner, model);
			Model deductions = infModel.getDeductionsModel();
		return deductions;	
		
		
	}
	
}
