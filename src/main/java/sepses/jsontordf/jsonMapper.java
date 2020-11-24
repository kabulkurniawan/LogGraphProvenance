package sepses.jsontordf;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class jsonMapper {
	public static ObjectNode constructSimpleEventJson(ObjectNode jsonNode) {
		
		String eventUUID = jsonNode.path("datum").path("Event").path("uuid").asText();
		String eventType = jsonNode.path("datum").path("Event").path("type").asText();
		String eventTimestampNanos = jsonNode.path("datum").path("Event").path("timestampNanos").asText();
		String eventSubjectUUID = jsonNode.path("datum").path("Event").path("subject").path("UUID").asText();
		String eventObjectUUID = jsonNode.path("datum").path("Event").path("predicateObject").path("UUID").asText();
		String eventObjectPathString = jsonNode.path("datum").path("Event").path("predicateObjectPath").path("string").asText();
		String eventPropertiesMapExec = jsonNode.path("datum").path("Event").path("properties").path("map").path("exec").asText();
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		
		 node.put("eventUUID", eventUUID);
		 node.put("eventTimestampNanos", eventTimestampNanos);
		 node.put("eventType", eventType);
		 node.put("eventSubjectUUID", eventSubjectUUID);
		 node.put("eventObjectUUID", eventObjectUUID);
		 if(eventObjectPathString!="") {
		   node.put("eventObjectPathString", cleanStrangeChar(eventObjectPathString));
		 }
		 node.put("eventPropertiesMapExec", cleanStrangeChar(eventPropertiesMapExec));
		 
	return node;

	}
	
	protected static String cleanStrangeChar(String string) {
		string = string.replace("{", "");
		string = string.replace("}", "");
		return string;
	}

	public static ObjectNode constructSimpleFileJson(ObjectNode jsonNode) {
		String fileUUID = jsonNode.path("datum").path("FileObject").path("uuid").asText();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		node.put("fileUUID", fileUUID);
		return node;
	}
	
	public static ObjectNode constructSimpleSubjectJson(ObjectNode jsonNode) {
		String subjectUUID = jsonNode.path("datum").path("Subject").path("uuid").asText();
		String parentSubjectUUID = jsonNode.path("datum").path("Subject").path("parentSubject").path("UUID").asText();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		node.put("subjectUUID", subjectUUID);
		node.put("parentSubjectUUID", parentSubjectUUID); 
		return node;
	}
}
