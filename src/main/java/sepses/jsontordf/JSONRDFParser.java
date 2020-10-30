package sepses.jsontordf;

import com.taxonic.carml.engine.RmlMapper;
import com.taxonic.carml.logical_source_resolver.JsonPathResolver;
import com.taxonic.carml.model.TriplesMap;
import com.taxonic.carml.util.RmlMappingLoader;
import com.taxonic.carml.vocab.Rdf;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.*;
import java.nio.file.Paths;
import java.util.Set;

public class JSONRDFParser {
	private RmlMapper mapper;
	private Set<TriplesMap> mapping;

	/**
     * return turtle file name generated with caRML and RML mappings.
     *
     * @param jsonFileName
     * @param rmlFile
     * @return
     * @throws IOException
     */
	public JSONRDFParser(String rmlFile) {
		initParse(rmlFile);
	}
	
    public static Model Parse(String jsonString, String rmlFile) throws IOException {
    	//System.out.println(jsonFileName+" "+rmlFile);
        // load RML file and all supporting functions
        InputStream is = JSONRDFParser.class.getClassLoader().getResourceAsStream(rmlFile);
        Set<TriplesMap> mapping = RmlMappingLoader.build().load(RDFFormat.TURTLE, is);
        RmlMapper mapper = RmlMapper.newBuilder().setLogicalSourceResolver(Rdf.Ql.JsonPath, new JsonPathResolver()).build();

        // load input file and convert it to RDF
        InputStream instances = new ByteArrayInputStream(jsonString.getBytes());
        mapper.bindInputStream(instances);

        // write it out to an turtle file
        org.eclipse.rdf4j.model.Model sesameModel = mapper.map(mapping);
        is.close();
        instances.close();
        //System.out.print(sesameModel.toString());

        // create a temp file and return jena modesl
        File file = File.createTempFile("thali", ".ttl");
        file.deleteOnExit();
        OutputStream tempOutput = new FileOutputStream(file);
        Rio.write(sesameModel, tempOutput, RDFFormat.TURTLE); // write mapping
        sesameModel.clear();
        tempOutput.flush();
        tempOutput.close();

        // create jena model
        Model models = ModelFactory.createDefaultModel();
        //models.setNsPrefixes(Utility.getPrefixes());
        InputStream tempInput = new FileInputStream(file);
        RDFDataMgr.read(models, tempInput, Lang.TURTLE);
        tempInput.close();
        //models.write(System.out,"TURTLE");
       // model.close();
        return models;
    }
    
    public org.eclipse.rdf4j.model.Model Parse(String jsonString) throws Exception {
    	//System.out.print(jsonString);
    	//System.exit(0);
    			InputStream targetStream = new ByteArrayInputStream(jsonString.getBytes());
    	        this.mapper.bindInputStream(targetStream);
    	        org.eclipse.rdf4j.model.Model model = this.mapper.map(this.mapping);
    	        targetStream.close();
	return model;
    }


    public void initParse(String rmlFile) {
    
     	 this.mapping =
        		  RmlMappingLoader
        		    .build()
        		    .load(RDFFormat.TURTLE, Paths.get(rmlFile));
        
        this.mapper = RmlMapper.newBuilder().setLogicalSourceResolver(Rdf.Ql.JsonPath, new JsonPathResolver()).build();
      
 	}

    public static void main(String[] args) throws IOException {
    	String jsonData="C:\\Users\\KKurniawan\\eclipse-workspace\\jsontordf\\src\\main\\resources\\apt29day1sysmon.json";
    	String rmlFile="rml/winlogbeat.rml";
//    	Model m = Parse(jsonData,rmlFile);
    }
}
