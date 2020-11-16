package sepses.jsontordf;

import java.io.File;
import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

public class AttackGraphGeneration {
	
	protected static void preprocessingAttackGraph(Model masterModel, Model provModel, Model knowledgeModel,  Model alertModel, String ng, String inputdir) throws Exception {
		String initFile = Utility.getOriginalFileName(ng);
		String initHDTFile = inputdir+initFile+".hdt";
		String initHDTProvFile = inputdir+initFile+"_prov.hdt";
		String initHDTKnowledgeFile = inputdir+initFile+"_knowledge.hdt";
		String initHDTAlertFile = inputdir+initFile+"_alert.hdt";
		
		loadExistingHDTFile(masterModel,initHDTFile);
		loadExistingHDTFile(provModel,initHDTProvFile);
		loadExistingHDTFile(knowledgeModel,initHDTKnowledgeFile);
		loadExistingHDTFile(alertModel,initHDTAlertFile);	
	}

	protected static void generateAttackGraph(Model masterModel, Model provModel, Model bgKnowledgeModel,  String outputdir, String namegraph, String sparqlEp, String outputFile, String triplestore, String livestore, String provrule, String propagationrule, String bgknowledge, ArrayList<String> confidentialdir, ArrayList<String> recognizedhost ) throws Exception {
		//generate current provenance model 
		Model currentProvModel = Provenance.generateEventProvenance(masterModel, provModel, outputdir, namegraph, sparqlEp, outputFile, triplestore, livestore, provrule);
		provModel.add(currentProvModel);
		
		//generate initial flag based on initial configuration file (see config.yaml) as bg-knowledge
		Model currentFlagModel = KnowledgeGeneration.generateInitialFlag(currentProvModel, confidentialdir, recognizedhost);
		bgKnowledgeModel.add(currentFlagModel);
		
		//apply propagation rules/policies to generate more flag on process/file
		Propagation.flagGenerationByPropagationRules(provModel, bgKnowledgeModel, propagationrule);
		
		//======detect alerts, save to rdf and store to database===========
			//Model tempModel = currentProvModel.union(bgKnowledgeModel).union(alerts);
			//Model currentAlerts = JenaReasoner.parseRule(tempModel,alertrule);
			//String outputAlertFile = Utility.saveToFile(currentAlerts,outputdir,alertFile);
			//Utility.storeFileInRepo(triplestore,outputAlertFile, sparqlEp, namegraph+"_alerts", "dba", "dba");
			//alerts.add(currentAlerts);
		    //currentAlerts.close();
		//======end alert detection ========================================
	    
		currentProvModel.close();
		currentFlagModel.close();
		
	}

	protected static void postProcessingAttackGraph(Model masterModel, Model provModel, Model bgKnowledgeModel, String namegraph, String inputdir, String outputdir, String bgknowledge, String livestore, String triplestore, String sparqlEp) throws Exception{
	
		//save mastermodel to rdf..
		System.out.println("save master model to rdf file...");
		String outputFileName = Utility.getOriginalFileName(namegraph)+".ttl";
		String outputMasterFile = Utility.saveToFile(masterModel,outputdir,outputFileName);
		
		//save provModel to rdf..
		System.out.println("save prov model to rdf file...");
		String outputProvFileName = Utility.getOriginalFileName(namegraph)+"_prov.ttl";
		String outputProvFile = Utility.saveToFile(provModel,outputdir,outputProvFileName);
		
		//save bgknowledge to rdf..
		System.out.println("save bgknowledge model to rdf file...");
		String outputKnowledgeFileName = Utility.getOriginalFileName(namegraph)+"_knowledge.ttl";
		String outputKnowledgeFile = Utility.saveToFile(bgKnowledgeModel,outputdir,outputKnowledgeFileName);
		
		//store prov & knowledge to triplestore
		if(livestore=="false") {
	       Utility.storeFileInRepo(triplestore,outputProvFile, sparqlEp, namegraph+"_prov", "dba", "dba");
		   Utility.storeFileInRepo(triplestore,outputKnowledgeFile, sparqlEp, namegraph+"_knowledge", "dba", "dba");
		   System.out.println("remove provenance tail...");
		   Provenance.removeProvTail(sparqlEp);
		}		
		
		//save rdf to .hdt
		System.out.println("save master and prov model rdf to hdt....");
		String outputMasterHDT = inputdir+Utility.getOriginalFileName(namegraph)+".hdt";
		Utility.generateHDTFile(namegraph, outputMasterFile, "TURTLE", outputMasterHDT);
		String outputProvHDT = inputdir+Utility.getOriginalFileName(namegraph)+"_prov.hdt";
		Utility.generateHDTFile(namegraph, outputProvFile, "TURTLE", outputProvHDT);
		String outputKnowledgeHDT = inputdir+Utility.getOriginalFileName(namegraph)+"_knowledge.hdt";
		Utility.generateHDTFile(namegraph, outputKnowledgeFile, "TURTLE", outputKnowledgeHDT);

		//alert as well
		//	String outputAlertHDT = inputdir+Utility.getOriginalFileName(namegraph)+"_alert.hdt";
		//	Utility.generateHDTFile(namegraph, outputdir+alertFile, "TURTLE", outputAlertHDT);
		
		//clean data
		System.out.println("clean files....");
		Utility.deleteFile(outputMasterFile);
		Utility.deleteFile(outputProvFile);
		Utility.deleteFile(outputKnowledgeFile);
		//Utility.deleteFile(outputdir+alertFile);
		
		provModel.close();
		masterModel.close();

	}
	
	protected static void loadExistingHDTFile(Model masterModel, String initHDTFile) throws Exception {
		String initRdfFile = "init-file.ttl";
		File initRdfFileObj = new File(initRdfFile);
		initRdfFileObj.createNewFile();
		masterModel = RDFDataMgr.loadModel(initRdfFile) ;
		File hdtFilePath = new File(initHDTFile);
		if(hdtFilePath.exists()) {
			System.out.println("master hdtFile exists!");
			Model prevModel = Utility.loadHDTToJenaModel(initHDTFile);
			masterModel.add(prevModel);
			prevModel.close();
			//when loading hdt file, this file is created then it's better to remove it
			//Utility.deleteFile(outputFileHDTTemp);
			Utility.deleteFile(initHDTFile+".index.v1-1");		
		}
	}

}
