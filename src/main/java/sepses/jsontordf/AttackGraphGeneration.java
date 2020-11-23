package sepses.jsontordf;

import java.io.File;
import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

public class AttackGraphGeneration {
	
	protected static void preprocessingAttackGraph(Model masterModel, Model provModel, Model knowledgeModel,  Model alertModel, String ng, String inputdir) throws Exception {
		String initFile = Utility.getOriginalFileName(ng);
		String initHDTFile = inputdir+initFile+"_master.hdt";
		String initHDTProvFile = inputdir+initFile+"_provenance.hdt";
		String initHDTKnowledgeFile = inputdir+initFile+"_knowledge.hdt";
		String initHDTAlertFile = inputdir+initFile+"_alert.hdt";
		
		masterModel.add(loadExistingHDTFile(masterModel,initHDTFile));
		provModel.add(loadExistingHDTFile(provModel,initHDTProvFile));
		knowledgeModel.add(loadExistingHDTFile(knowledgeModel,initHDTKnowledgeFile));
		alertModel.add(loadExistingHDTFile(alertModel,initHDTAlertFile));	
	}

	protected static void generateAttackGraph(Model masterModel, Model provModel, Model bgKnowledgeModel, Model alertModel, String provrule, String bgknowledge, String propagationrule , String alertrule, ArrayList<String> confidentialdir, ArrayList<String> recognizedhost , String outputdir, String namegraph, String sparqlEp, String outputFile, String triplestore, String livestore) throws Exception {
		//generate current provenance model 
		Model currentProvModel = Provenance.generateEventProvenance(masterModel, provModel,  provrule, outputdir, namegraph, sparqlEp, outputFile, triplestore, livestore);
		provModel.add(currentProvModel);
				
		//apply propagation based on propagation rule
		Propagation.generateFlagPropagation(provModel, currentProvModel, bgKnowledgeModel, confidentialdir, recognizedhost, propagationrule, outputdir, namegraph, sparqlEp, outputFile, triplestore, livestore);
		
		//apply alert rule to generate alert
		AlertDetection.generateAlert(provModel, bgKnowledgeModel, alertModel, alertrule, outputdir, namegraph, sparqlEp, outputFile, triplestore, livestore);
	    
		currentProvModel.close();
		
		
	}

	protected static void postProcessingAttackGraph(Model masterModel, Model provModel, Model bgKnowledgeModel, Model alertModel, String namegraph, String inputdir, String outputdir, String bgknowledge, String livestore, String triplestore, String sparqlEp) throws Exception{
	
		saveStoreExportHDT(masterModel, inputdir,  outputdir,  namegraph,  "master", livestore,  triplestore, sparqlEp);
		saveStoreExportHDT(provModel, inputdir,  outputdir,  namegraph,  "provenance",  livestore,  triplestore, sparqlEp);
		saveStoreExportHDT(bgKnowledgeModel, inputdir,  outputdir,  namegraph,  "knowledge",  livestore,  triplestore, sparqlEp);
		saveStoreExportHDT(alertModel, inputdir,  outputdir,  namegraph,  "alert",  livestore,  triplestore, sparqlEp);
		System.out.println("remove provenance tail...");
		   Provenance.removeProvTail(sparqlEp);

	}
	
	protected static Model loadExistingHDTFile(Model masterModel, String initHDTFile) throws Exception {
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
		return masterModel;
	}
	
	protected static void saveStoreExportHDT(Model model, String inputdir, String outputdir, String namegraph, String spec_namegraph, String livestore, String triplestore, String sparqlEp) throws Exception {
		//save mastermodel to rdf..
		
		if(!model.isEmpty()) {
		System.out.println("save "+spec_namegraph+" model to rdf file...");
		String outputFileName = Utility.getOriginalFileName(namegraph)+"_"+spec_namegraph+".ttl";
		String outputModelFile = Utility.saveToFile(model,outputdir,outputFileName);
		
		if(livestore=="false") {
			if(spec_namegraph!="master") {
			Utility.storeFileInRepo(triplestore,outputModelFile, sparqlEp, namegraph+"_"+spec_namegraph, "dba", "dba");
			}
		}	
		//save rdf to .hdt
		System.out.println("save "+spec_namegraph+" model rdf to hdt....");
		
		String outputModelHDT = inputdir+Utility.getOriginalFileName(namegraph)+"_"+spec_namegraph+".hdt";
		Utility.generateHDTFile(namegraph+"_"+spec_namegraph, outputModelFile, "TURTLE", outputModelHDT);
		
		//clean data
		System.out.println("clean "+spec_namegraph+" files....");
		Utility.deleteFile(outputModelFile);
		model.close();
		
		}
	}

}
