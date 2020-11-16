package sepses.jsontordf;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.jena.rdf.model.Model;

public class Propagation {
	
	public static void flagGenerationByPropagationRules(Model provModel, Model bgknowledgemodel, String propagationrule ) throws URISyntaxException, IOException{
		Model tempModel = provModel.union(bgknowledgemodel);
		Model currentPropagatedFlag = JenaReasoner.parseRule(tempModel,propagationrule);
		if(!currentPropagatedFlag.isEmpty()) {
			bgknowledgemodel.add(currentPropagatedFlag);
			flagGenerationByPropagationRules(provModel, bgknowledgemodel, propagationrule);
		}
		tempModel.close();
	}

}
