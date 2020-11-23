package sepses.jsontordf;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdtjena.HDTGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.rules.RuleUtil;
import org.yaml.snakeyaml.Yaml;

import sepses.jsontordf.Storage;
import sepses.jsontordf.VirtuosoStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

public class Utility {

    private static final Logger log = LoggerFactory.getLogger(Utility.class);

    public static String saveRDF4JModel(org.eclipse.rdf4j.model.Model model, String outputDir, String outputFile) throws Exception { 
        String outputFileName = outputDir + outputFile;
		OutputStream tempOutput = new FileOutputStream(outputFileName);
        Rio.write(model, tempOutput, RDFFormat.TURTLE); // write mapping
        model.clear();
        tempOutput.flush();
        tempOutput.close();
        return outputFileName;
    }
	
    public static void deleteFile(String file) throws Exception {
		File f = new File(file);
		f.delete();
	}
    
	  public static void storeFileInRepo(String triplestore, String fileLocation, String sparqlEndpoint, String namegraph, String user, String pass) {
		 
		  Storage storage = null;
		 if(triplestore.equals("graphdb")){
			  storage = GraphDBStorage.getInstance();
		 }else if (triplestore.equals("virtuoso")) {
			  storage = VirtuosoStorage.getInstance();
		 }else {
			  storage = GraphDBStorage.getInstance();
		 }
		 
		  System.out.println("Store data: "+fileLocation+" to " + sparqlEndpoint + " using graph " + namegraph);
	       storage.storeData(fileLocation, sparqlEndpoint, namegraph, true, user, pass);
	    }
	 
    public static String saveToFile(Model model, String outputDir, String fileName) {
    	String outputFileName = outputDir + fileName;
        File outputFile = new File(outputFileName);
        outputFile.getParentFile().mkdirs();

        try {
            FileWriter out = new FileWriter(outputFile);
            model.write(out, "TURTLE");
            out.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return outputFileName;
    }

	public static Map<String, Object> readYamlFile(String file) throws FileNotFoundException{
		InputStream input = readFile(file);
	    
	    Yaml yaml = new Yaml();
	    
	    Map<String, Object> yamlContent = yaml.load(input);
	    
	    
		return yamlContent;
		
	}
	
	public static InputStream readFile(String file) throws FileNotFoundException {
		final File initialFile = new File(file);
		//System.out.print(initialFile);
	    final InputStream input = new FileInputStream(initialFile);
	    return input;

	}
	public static void generateHDTFile(String baseURI, String filename, String inputType, String outputFile) throws IOException, ParserException {
		//generate file name based on the original input file name
	
			HDT hdt = HDTManager.generateHDT(filename, baseURI, RDFNotation.parse(inputType), new HDTSpecification(), null);
			
			// Add additional domain-specific properties to the header:
			Header header = hdt.getHeader();
			header.insert("myResource1", "property" , "value");
			
			// Save generated HDT to a file
			hdt.saveToHDT(outputFile, null);
		
		
		}
	
	public static Model loadHDTToJenaModel(String HDTFile) throws IOException{
		// Load HDT file using the hdt-java library
		HDT hdt = HDTManager.mapIndexedHDT(HDTFile, null);
		
		
		 
		// Create Jena Model on top of HDT.
		HDTGraph graph = new HDTGraph(hdt);
		
		Model model = ModelFactory.createModelForGraph(graph);
		
		
		return model;
	}
	
	public static String getOriginalFileName(String filename){
	   	String fileName = filename.substring(filename.lastIndexOf("/") + 1);
        if (fileName.indexOf("\\") >= 0) {
            fileName = filename.substring(filename.lastIndexOf("\\") + 1);
        }
        
        return fileName;
        
	}

	public static void copyFileUsingStream(String sourceFile, String destFile) throws IOException {
		File source = new File(sourceFile);
		File dest = new File(destFile);
		
		
	    InputStream is = null;
	    OutputStream os = null;
	    try {
	        is = new FileInputStream(source);
	        os = new FileOutputStream(dest);
	        byte[] buffer = new byte[1024];
	        int length;
	        while ((length = is.read(buffer)) > 0) {
	            os.write(buffer, 0, length);
	        }
	    } finally {
	        is.close();
	        os.close();
	    }
	}
	
    public static Model executeRule(String shaclFile, Model model) throws FileNotFoundException {

        Model constraints = ModelFactory.createDefaultModel();
        InputStream is = new FileInputStream(shaclFile);
       // InputStream is = Util.class.getClassLoader().getResourceAsStream(shaclFile);
        RDFDataMgr.read(constraints, is, Lang.TURTLE);
        Model result = RuleUtil.executeRules(model, constraints, null, null);
        // model.add(result);
        // model.write(System.out,"TURTLE");
        
        //return result.getModel();
        return result;
    }
    
    public static ArrayList<String> listFilesForFolder(final File folder) {
    	ArrayList<String> rulefiles = new ArrayList<String>();
    	
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
            	rulefiles.add(fileEntry.getName());
                // System.out.println(fileEntry.getName());
            }
        }
        
        return rulefiles;
    }

}
