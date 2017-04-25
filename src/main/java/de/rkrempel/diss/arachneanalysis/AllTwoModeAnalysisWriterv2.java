package de.rkrempel.diss.arachneanalysis;

import de.rkrempel.diss.core.commontools.GraphContainer;
import de.rkrempel.diss.core.commontools.TwoModeToOneModeNetworkConverter;
import de.rkrempel.diss.core.dbtools.ArachneTableInfoTool;
import de.rkrempel.diss.core.dbtools.MYSQLRetriver;
import de.rkrempel.diss.core.dbtools.MysqlNodeCreator;
import de.rkrempel.diss.core.dbtools.SemanticConnectionMysqlCorelationGetter;
import de.rkrempel.diss.core.exporters.XMLStandardConverter;
import de.rkrempel.diss.core.report.ReportWriter;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//TODO Data not Reproducible Available
/**
 * Analysis Pipeline for the Arachne Used in Dissertation for creating the Output.
 * Main program code Responsible for Chapter "3.5 Patterns im automatischen Retrieval"
 * You probably wont be able to reproduce. This is more For demonstration purposes.
 * wanna Try with real data Contact http://arachne.uni-koeln.de or the German archaeological Institute (Förtsch)
 */
public final class AllTwoModeAnalysisWriterv2 {
	//TODO Better Settings

	/**
	 * Mysql Connection
	 */

	private static String password = "";
	private static String server = "";
	private static String url = "jdbc:mysql://"+server+":3306/arachne"+"?dontTrackOpenResources=true";
	private static String user = "";
	

	private static Connection con;
	/**
	 * SETUP VARS
	 */
	private static final Float minimumUniquity= 1.0f/1000.0f;
	private static MYSQLRetriver retriver;
	private final static int endFieldSizeEdge=6;

	private final static String path = "/Put/Target/Directory/Here";
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		
		
		ReportWriter.getInstance().CloseReport();
		List<String> types = new ArrayList<String>();
		//Many, if you are stuck comment out....
		types.add("objekt");
		//Messy
		//types.add("datierung");
		types.add("gruppen");
		// Only Single Connecty alot of senseles work
		//types.add("marbilder");
		

		types.add("typus");
		
		types.add("person");
		types.add("realien");
		
		types.add("relief");
		
		types.add("reproduktion");
		types.add("rezeption");
		types.add("bauwerk");
		types.add("bauwerksteil");
	
		types.add("literatur");
		
		types.add("sammlungen");
		//Strange subproject
		//types.add("sarkophag");

		//Alot of Data but propably Worth it.
		types.add("buchseite");

		types.add("ort");
	
		types.add("topographie");
		
		types.add("buch");
		
		
		
		for (int a = 0; a < types.size(); a++) {
			try {
				Setup();
			} catch (InstantiationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			retriver = new MYSQLRetriver(con,"Arachne");
			for(int j = a;j<types.size();j++){
				
				if(types.get(a).equals(types.get(j)))
						continue;
				
				String label1;
				String label2;
				if(types.get(a).compareTo(types.get(j))<0){
					label1 = types.get(a);
					label2 =types.get(j);
				}
				else{
					label1 = types.get(j);
					label2 =types.get(a);
				}
				
				File file = new File(filename(label1,label2));
				
				if(file.exists())
					continue;
				
				WriteNet( label1,  label2);

			}
			
		
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
				
		

		System.out.println("---------------------------------------------------------------" );
		}
	}

	private static boolean WriteNet(String label1, String label2){
		if((label1.equals("buch") || label1.equals("buchseite") )&&(label2.equals("buchseite") ||label2.equals("buch")))
				return false;
		
		ArachneTableInfoTool ATIT = new ArachneTableInfoTool();
		
		List<List<Long>> allNodeIdsinSets= new ArrayList<List<Long>>();
		StringBuilder localDescription = new StringBuilder();
		StringBuilder Description = new StringBuilder();
		ReportWriter.getInstance().appendTechEvent("Started Constructing");
		ReportWriter.getInstance().appendTechEvent("Using Server : " +server);
		ReportWriter.getInstance().appendTechEvent("Using Connection parameters : " +url);
		ReportWriter.getInstance().appendReportEvent(label1 + "<-> "+label2);
		
		localDescription.append(label1 + "<-> "+label2+"\n");
		
		final String PreambleTwoMode="This Two-Mode network was Taken from the Arachne Database. It represents the Connection of " +label1+" to "+label2+".\n The Following Networks will be Constructed to be Assambled Together.\n";
		final String PreambleOneMode1="This a One-Mode network was Taken from the Arachne Database. It was Induced from the a Two mode Network that represents the Connection of " +label1+" to "+label2+".\n The Following Network has "+label1+" as nodes.\n The edges are formed by the Connections over "+label2+".\n";
		final String PreambleOneMode2="This a One-Mode network was Taken from the Arachne Database. It was Induced from the a Two mode Network that represents the Connection of " +label1+" to "+label2+".\n The Following Network has "+label2+" as nodes.\n The edges are formed by the Connections over "+label1+".\n";
		
		System.out.println("-----------------------"+label1 + "<-> "+label2+"-----------------------------" );
		
		///////////////////Twomode Start
		List<Long> connectedNodes = null;
		List<Long>  connectedNodes1 = null;
		List<Long>  connectedNodes2 = null;
		List<Object[]>  Nodes1 = null;
		List<Object[]>  Nodes2 = null;
		
		String twoModeFileName="";
		
		Long nodessize =new Long(0);
		Long nodessize1 =new Long(0);
		Long nodessize2 =new Long(0);
		
		MysqlNodeCreator nc = new MysqlNodeCreator(retriver);
		nc.setURIPrefix("http://arachne.uni-koeln.de/entity/");
		String localPath= path+ "/"+label1+"_"+label2+"/" ;
		
		File f = new File(localPath+"desc.txt");
		
		if(f.exists()) {
			System.out.println("The Analysis is Allready completed ");
			return false;
		}
		
		List<Long[]> twoMode = ArachneTwoMode(label1,label2);
		System.out.println("Two Mode Retrived");
		ArrayList<Object[]> newLinksListComplete = new ArrayList<Object[]>(twoMode.size());
		
		if(twoMode.isEmpty()){
			System.out.println("Its Empty Nothing Here!");
			return false;
			
		}
		try {
			String sql= "SELECT Count(*) FROM arachneentityidentification WHERE (TableName= '"+label1+"' OR TableName= '"+label2+"' ) AND isDeleted =0;";
			nodessize = retriver.getNthfieldsAsLong(1, sql).get(0);
			//nodesConnected = retriver.getNthfieldsAsLong(1, "SELECT Count(*) FROM arachneentityidentification WHERE TableName= '"+types.get(j)+"' AND isDeleted =0;").get(0);
			ReportWriter.getInstance().appendTechEvent("fetched Size for all nodes :"+sql);
			//Fist Type Nodes
			sql = "SELECT Count(*) FROM arachneentityidentification WHERE TableName= '"+label1+"' AND isDeleted =0;";
			nodessize1 = retriver.getNthfieldsAsLong(1,sql ).get(0);
			ReportWriter.getInstance().appendTechEvent("fetched Size for "+label1+" nodes :"+sql);
			
			
			//			sql = "SELECT Source FROM SemanticConnection WHERE TypeSource= '"+label1+"' AND  TypeTarget= '"+label2+"' GROUP BY Source;";

			sql = "SELECT Source,"+ATIT.getDescriptionNamebyName(label1)+" FROM SemanticConnection LEFT JOIN `arachneentityidentification` ON `arachneentityidentification`.`ArachneEntityID` = `SemanticConnection`.`Source` LEFT JOIN `"+label1+"` ON `"+label1+"`.`"+ATIT.getKeyNamebyName(label1) +"` =`arachneentityidentification`.`ForeignKey` WHERE `SemanticConnection`.`TypeSource`= '"+label1+"' AND  `SemanticConnection`.`TypeTarget`= '"+label2+"' GROUP BY Source;";
			
			connectedNodes1 = retriver.getNthfieldsAsLong(1,sql );
			//Labels
			List<String> connectedNodes1Labels = retriver.getNthfieldsAsString(2, sql);
			for (int i = 0; i< connectedNodes1Labels.size();i++) {
				connectedNodes1Labels.set(i,connectedNodes1Labels.get(i).replaceAll("\\s+", " ").trim());
			}
			Nodes1= nc.createNodesByArachneEntityGroupsIds(connectedNodes1,connectedNodes1Labels,1);
			
			 //Second Type Nodes
			sql = "SELECT Count(*) FROM arachneentityidentification WHERE TableName= '"+label2+"' AND isDeleted =0;";
			nodessize2 = retriver.getNthfieldsAsLong(1, sql ).get(0);
			ReportWriter.getInstance().appendTechEvent("fetched Size for "+label2+" nodes :"+sql);
			
			//sql = "SELECT Source FROM SemanticConnection WHERE TypeSource= '"+label2+"' AND  TypeTarget= '"+label1+"' GROUP BY Source;";
			//sql = "SELECT Source FROM SemanticConnection WHERE TypeSource= '"+label2+"' AND  TypeTarget= '"+label1+"' GROUP BY Source;";
			sql = "SELECT Source ,"+ATIT.getDescriptionNamebyName(label2)+" FROM SemanticConnection LEFT JOIN `arachneentityidentification` ON `arachneentityidentification`.`ArachneEntityID` = `SemanticConnection`.`Source` LEFT JOIN `"+label2+"` ON `"+label2+"`.`"+ATIT.getKeyNamebyName(label2) +"` =`arachneentityidentification`.`ForeignKey` WHERE `SemanticConnection`.`TypeSource`= '"+label2+"' AND  `SemanticConnection`.`TypeTarget`= '"+label1+"' GROUP BY Source;";

			connectedNodes2 = retriver.getNthfieldsAsLong(1,sql );
			//Labels
			
			List<String> connectedNodes2Labels= retriver.getNthfieldsAsString(2, sql);
			for (int i = 0; i< connectedNodes2Labels.size();i++) {
				connectedNodes2Labels.set(i,connectedNodes2Labels.get(i).replaceAll("\\s+", " ").trim());
			}
			
			Nodes2= nc.createNodesByArachneEntityGroupsIds(connectedNodes2,connectedNodes2Labels,2);	 
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("DataBase Access Kaputt?");
			e.printStackTrace();
		}
		
		System.out.println("Other Sql Finished");
		
		for (int i = 0; i < twoMode.size(); i++) {
			Object[] insert = new Object[endFieldSizeEdge];
			
			insert[0]= twoMode.get(i)[0];
			insert[1]= twoMode.get(i)[1];
			
			for (int k = 2; k < endFieldSizeEdge-1 ; k++) {
				insert[k] = new Float( 1.0f);
			}
			
			//Mark The Two Mode Network with 0;
			insert[endFieldSizeEdge-1] =new Integer(0);
			newLinksListComplete.add(insert);
		}
		
		String desc = GraphContainer.ShowEdgelistAttributes(newLinksListComplete, nodessize);
		localDescription.append(desc+"\n");
		
		//Create Folder
		
		boolean success = (new File(localPath )).mkdirs();
		
		if (!success) {
		    System.out.println("There is A Problem with the Folder Creation");
		}
		
		List<Object[]>allNodes = new ArrayList<Object[]>(Nodes1.size()+Nodes2.size());
		allNodes.addAll(Nodes1);
		allNodes.addAll(Nodes2);
		
		twoModeFileName = "twoMode_"+label1 + "_"+label2 ;
		ReportWriter.getInstance().description(PreambleTwoMode+localDescription.toString());
		NetOutput(newLinksListComplete,allNodes,  "", localPath, twoModeFileName );
		
		allNodes.clear();
		newLinksListComplete.clear();
		
		Description.append(localDescription);
		localDescription = new StringBuilder();
		ReportWriter.getInstance().clear();
		
		System.out.println( desc);
		
		//////////////////Twomode End

		System.out.println("<<< One Modes" );
		System.out.println(label1+"-"+label2+"-"+label1 +"\n");
		
		Description.append("<<< One Modes\n");
		localDescription.append(label1+"-"+label2+"-"+label1 +"\n");
		//Description.append(localDescription);
		
		ReportWriter.getInstance().appendTechEvent("Started Constructing");
		ReportWriter.getInstance().appendTechEvent("Source: see File "+twoModeFileName);
		
		System.out.println(label1+"-"+label2+"-"+label1+"\n" );
		TwoModeToOneModeNetworkConverter twoModeToOneMode = new TwoModeToOneModeNetworkConverter();
		
		if(minimumUniquity != 0.0f)
			twoModeToOneMode.setMinimumRelevance(minimumUniquity);
		
		List<Object[]> result = twoModeToOneMode.makeCTEdgesAllWeightsMarked(twoMode,1);
		
		desc = GraphContainer.ShowEdgelistAttributes(result, nodessize1);
		System.out.println(desc);
		localDescription.append(desc+"\n");
		ReportWriter.getInstance().description(PreambleOneMode1+localDescription.toString());
		
		if(result.size()>0)
			NetOutput(result,Nodes1,  "", localPath, "OneMode_"+label1 + "_over_"+label2 );
		
		result.clear();
		Nodes1.clear();
		System.out.println("");
		
		Description.append(localDescription);
		localDescription = new StringBuilder();
		ReportWriter.getInstance().clear();
		System.gc();
		
		///////////Second One Mode
		
		ReportWriter.getInstance().appendTechEvent("Started Constructing");
		ReportWriter.getInstance().appendTechEvent("Source: see File "+twoModeFileName);
		
		System.out.println(label2+"-"+label1+"-"+label2 );
		localDescription.append(label2+"-"+label1+"-"+label2+"\n");
		
		twoModeToOneMode = new TwoModeToOneModeNetworkConverter();
		
		if(minimumUniquity != 0.0f)
			twoModeToOneMode.setMinimumRelevance(minimumUniquity);
		
		for (int i = 0; i < twoMode.size(); i++) {
			
			Long temp = twoMode.get(i)[0];
			twoMode.get(i)[0] = twoMode.get(i)[1];
			twoMode.get(i)[1] = temp;	
		}
		
		result = twoModeToOneMode.makeCTEdgesAllWeightsMarked(twoMode,2);
		desc = GraphContainer.ShowEdgelistAttributes(result, nodessize2);
		allNodeIdsinSets.add(connectedNodes);
		System.out.println(desc);
		localDescription.append(desc+"\n");
		ReportWriter.getInstance().description(PreambleOneMode2+localDescription.toString());
		if(result.size()>0)
			NetOutput(result,Nodes2,  "", localPath, "OneMode_"+label2 + "_over_"+label1 );
		
		result.clear();
		Nodes2.clear();
		
		Description.append(localDescription);
		
		for (Long[] longs : twoMode) {
		
			for (int i = 0; i < longs.length; i++) {
				longs[i] =null;
			}
			
			longs= null;
		}
		
		twoMode.clear();
		
		try{
			  // Create file 
			  FileWriter fstream = new FileWriter(descfilename(),true);
			  BufferedWriter description = new BufferedWriter(fstream);
			  description.write("----------------------------------\n");
			  description.write(Description.toString());
			  //Close the output stream
			  description.close();
		}catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
		}
		
		try{
			  // Create file 
			  FileWriter fstream = new FileWriter(localPath+"desc.txt",true);
			  BufferedWriter description = new BufferedWriter(fstream);
			  description.write("----------------------------------\n");
			  description.write(Description.toString());
			  //Close the output stream
			  description.close();
		}catch (Exception e){
			//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}		
		
		System.out.println("---------------------------------------------------------------" );
		ReportWriter.getInstance().clear();
		return true;
	}
	
	public static String filename(String one, String two){
		
		return path+"twomode_"+one+"_"+two +
		".GraphMl";
	}
	
	public static String descfilename(){
		return path+"desc.txt";	
	}
	
	
	private static void NetOutput(List<Object[]> edges,List<Object[]> nodes, String Description, String localpath, String filename ){
		//TODO Node Desctiptions Hinzufügen!
		//TODO Descriptions für Felder in Graphen Machen //Kanten Gemacht
		//TODO Reports Nachtragen Von Ignores Also Minimum Relevance
		//TODO Schreiben aller "Interessanten" Netze in Verschienden Formaten.
		
		GraphContainer out;
		ReportWriter.getInstance().description(Description.toString());
		out = new GraphContainer(nodes, edges);
		
		out.autoInitializeLabels();
		
		List<String> edgeLabels = new ArrayList<String>(endFieldSizeEdge);
		
		edgeLabels.add("Source");
		edgeLabels.add("Target");
		edgeLabels.add("Weight");
		edgeLabels.add("Uniquity");
		edgeLabels.add("Avarage Uniquity");
		edgeLabels.add("Type");
		
		out.setEdgeLabels(edgeLabels);
		
		//TODO Reports Nachtragen Von Ignores Also Minimum Relevance
		
		//Produce Export
		XMLStandardConverter xsc = new XMLStandardConverter(out);
		//xsc.setEdges(result);
		//xsc.setNodes(nodes);
		//StringBuffer output = xsc.getResAsStringBuffer();
	
		//TODO Schreiben aller "Interessanten" Netze in Verschienden Formaten. 
	
		try {
			FileWriter fstream = new FileWriter(localpath+filename+".graphml");
			//fstream = new FileWriter("/Users/archaeopool/paralells.net");	
			Writer to = new BufferedWriter(fstream);
			xsc.writeResWithWriter(to);
			to.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	private static List<Long[]> ArachneTwoMode(String primaryKeyNodes,String primaryKeyEdges){

		System.out.println(primaryKeyNodes +" - "+primaryKeyEdges );
		SemanticConnectionMysqlCorelationGetter corget = new SemanticConnectionMysqlCorelationGetter(primaryKeyNodes, primaryKeyEdges,new MYSQLRetriver(con,"Arachne"));
		corget.setMinimumRelevance(1.0f/1000.0f);
		List<Long[]> out = corget.twoModeAsListofArrays();
		
		return out;
}
	
	private static void Setup() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		con = DriverManager.getConnection(url, user, password);
	}
}
