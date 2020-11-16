package sepses.jsontordf;

import com.taxonic.carml.engine.RmlMapper;
import com.taxonic.carml.logical_source_resolver.JsonPathResolver;
import com.taxonic.carml.model.TriplesMap;
import com.taxonic.carml.util.RmlMappingLoader;
import com.taxonic.carml.vocab.Rdf;
import org.eclipse.rdf4j.rio.RDFFormat;

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
	
    public org.eclipse.rdf4j.model.Model Parse(String jsonString) throws Exception {
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

}
