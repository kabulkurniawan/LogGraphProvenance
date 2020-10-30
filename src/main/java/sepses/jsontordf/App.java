package sepses.jsontordf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.yaml.snakeyaml.Yaml;

/**
 * Hello world!
 *
 */
public class App	
{

	public Map<String, Object> configData;
	public ArrayList<String> objectName;
	public String objectType;
	public String actionType;
	boolean joinFilter;
	
	public App(String configFile) {
		this.objectName = new ArrayList<String>();
		this.joinFilter = false;
		InputStream input = JsonReader.class.getClassLoader().getResourceAsStream(configFile);
		Yaml yaml = new Yaml();
	    this.configData = yaml.load(input);
	}
	
	public  void parsePseudocode(String txt) throws IOException, ParseException
    {
		
    	
    	String text = (txt.replaceAll("\\(\r\n\\s+", "(")).replaceAll("\r\n\\s+\\)",")");
    	
    	System.out.println("=============pseudocode==========================");		
    	System.out.println(text);
    	System.out.println("");
    	
    	//1. split string by line
    	String[] s = text.split("\r\n");
    	
    	
    	HashMap<String, String>  SPARQLStatementJoin = new HashMap<String, String>(); 
    	HashMap<String, String> SPARQLStatement = new HashMap<String, String>(); 
    	String SPARQLFilterJoin = ""; 
    	 
    	//foreach line, check the line type
		  TextSplitter ts = new TextSplitter();
		  
    	for (String string : s) {

    		 // checkLineType(string);
    		  //check is it object-action-type?
    		  String objectAction = ts.findTextByRegex(string.trim(), this.configData.get("object-action-type").toString());
    		  String objectNameFilter =ts.findTextByRegex(string.trim(), this.configData.get("object-name-filter").toString());
    		  String objectNameJoin =ts.findTextByRegex(string.trim(), this.configData.get("object-name-join").toString());
    		  String completeStatement="";
    		  String completeJoinStatement="";
    		  if(objectAction!=null) {
				  this.objectType = ts.getObjectType(objectAction);
				  this.actionType = ts.getActionType(objectAction);
			  }else if(objectNameFilter!=null) {
				  this.objectName.add(ts.getObjectName(string));
		    	  String objectName;	
				  objectName = ts.getObjectName(string);
				  String tripleFilterStatement = parseLineFilter(string);
				  String closeStatement ="}\r\n";
				  completeStatement = ts.generateSelectQuery()+tripleFilterStatement+closeStatement;
				  completeJoinStatement = "{ "+ts.generateSelectQuery()+tripleFilterStatement;
				  SPARQLStatement.put(objectName, completeStatement);
				  SPARQLStatementJoin.put(objectName, completeJoinStatement);
				 		    		
			  }else if(objectNameJoin!=null) {
				  String closeJoin = "  }}\r\n";
				  String filterStatement = parseLineJoin(SPARQLStatementJoin,string);
				  for (String objectName: this.objectName) {
				//	System.out.println(objectName.trim()); 
					//System.exit(0);
					String filterTriples = parseTripleFromJoinFilter(objectName.trim(), string);  
					  //System.out.println(filterTriples);
				    String SPstatement = SPARQLStatementJoin.get(objectName);
				    //System.out.print(objectName);
				    SPARQLStatementJoin.put(objectName, SPstatement+filterTriples+closeJoin);
				  
				}
				 // System.exit(0);
				  //String SPstatement = SPARQLStatementJoin.get(objectName);
				//  SPARQLStatementJoin.add(objectCount,);
				  
				  
				  
				 SPARQLFilterJoin=filterStatement; 
				  this.joinFilter=true;
			  }
    	}	
    	
    	//System.out.println(SPARQLStatement);
    	
    	
    	
    	
    	System.out.println("==========SPARQL Query===========================");
    	
    	  String prefix = ts.generatePrefix(this.configData.get("prefix").toString());
    	  String statements = "";
    	  
    	  if(this.joinFilter) {
    		  String globalSelect = ts.generateGlobalSelectQuery();
    		  String closingGlobalSelect = "}\r\n";
    		  for (String i : SPARQLStatementJoin.values()) {
    			  //System.out.println(i);
    			  statements = statements+i;
    			}
    		  System.out.print(prefix+globalSelect+statements+SPARQLFilterJoin+closingGlobalSelect);
    	}else {
    		for (String i : SPARQLStatement.values()) {
    			 //System.out.println(i);
    			  statements = statements+i;
    			}
    		  System.out.print(prefix+statements);
    	}
    

    	
    }
    
	public String parseTripleFromJoinFilter(String objectName, String string) {
		//System.out.print(string);
		//System.exit(0);
		String triples = "";
		 TextSplitter ts = new TextSplitter();
		 String[] st = string.split(" and ");
	    	for (String str : st) {
	    		String strFilter = ts.findTextByRegex(str.trim(), this.configData.get("filter-operator").toString() );
	    		String triple = ts.parseFilterTripleByObjectName(objectName,strFilter);
	    		triples = triples+triple;
	    	}
	    //System.out.print(triples);
	   // System.exit(0);
		return triples;
	}
	
	 public String parseLineJoin(Map<String, String> SPARQLStatementJoin, String string) {
		 TextSplitter ts = new TextSplitter();
		 String filterStatement="";
		 String[] st = string.split(" and ");
	    	for (String str : st) {
	    		
	    		//System.out.println(string3.trim());
	    		String strFilter = ts.findTextByRegex(str.trim(), this.configData.get("filter-operator").toString() );
	    		
	    		if(strFilter!=null) {
	 		  		filterStatement = filterStatement+ts.generateFilterJoin(strFilter);
	    		}    
	    	} 
	    return filterStatement;
	 		
	 }
	 
     public String parseLineFilter(String string) {
    	 String tripleStatement="";
    	 String filterStatement="";
    	TextSplitter ts = new TextSplitter();
    	      
    		String objectName="";
    		String s1 = ts.findTextByRegex(string.trim(), this.configData.get("object-name-filter").toString() );
    		if(s1!=null) {
    			 objectName = ts.getObjectName(s1);
    			    tripleStatement = tripleStatement+ts.generateObjectActionTriple(this.objectType,this.actionType, objectName);
    			    tripleStatement = tripleStatement+ts.generateObjectNameTriple(objectName);
    		}
    		
    		
			
		    	String[] s3 = string.split(" and ");
		    	for (String string3 : s3) {
		    		//System.out.println(string3.trim());
		    		String s4 = ts.findTextByRegex(string3.trim(), this.configData.get("filter-operator").toString() );
		    		String s5 = ts.findTextByRegex(string3.trim(),  this.configData.get("filter-match").toString() );
		    		
		    		if(s4!=null) {
		    			 tripleStatement =  tripleStatement + ts.generateFieldTriple(s4,objectName);
				         
				       
		 		    }else if(s5!=null) {
		 			     
		 		    	tripleStatement = tripleStatement+ ts.generateFieldTriple(s5,objectName);
		 		    	
				        
		 		    }
				
				}
		    	for (String string3 : s3) {
		    		//System.out.println(string3.trim());
		    		String s4 = ts.findTextByRegex(string3.trim(),  this.configData.get("filter-operator").toString() );
		    		String s5 = ts.findTextByRegex(string3.trim(),  this.configData.get("filter-match").toString() );
		    		
		    		if(s4!=null) {
		    			filterStatement = filterStatement +ts.generateFilter(s4,objectName);
				         
				       
		 		    } else if(s5!=null) {
		 		    	filterStatement = filterStatement + ts.generateFilter(s5,objectName);
				        
		 		    }
				
				}
		    	
		    	return tripleStatement+filterStatement;
		    }

    
}
