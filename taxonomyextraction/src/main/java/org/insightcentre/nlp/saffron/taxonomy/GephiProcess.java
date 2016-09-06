//package org.insightcentre.nlp.saffron.taxonomy;
//
//import java.awt.Color;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.commons.io.FileUtils;
//import org.gephi.data.attributes.api.AttributeController;
//import org.gephi.data.attributes.api.AttributeModel;
//import org.gephi.graph.api.Edge;
//import org.gephi.graph.api.EdgeIterable;
//import org.gephi.graph.api.GraphController;
//import org.gephi.graph.api.GraphModel;
//import org.gephi.graph.api.Node;
//import org.gephi.graph.api.NodeIterable;
//import org.gephi.io.exporter.api.ExportController;
//import org.gephi.io.exporter.preview.SVGExporter;
//import org.gephi.io.exporter.spi.CharacterExporter;
//import org.gephi.io.exporter.spi.Exporter;
//import org.gephi.io.importer.api.Container;
//import org.gephi.io.importer.api.EdgeDefault;
//import org.gephi.io.importer.api.ImportController;
//import org.gephi.io.importer.spi.FileImporter;
//import org.gephi.io.processor.plugin.DefaultProcessor;
//import org.gephi.layout.plugin.force.StepDisplacement;
//import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
//import org.gephi.layout.plugin.forceAtlas.ForceAtlas;
//import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
//import org.gephi.project.api.ProjectController;
//import org.gephi.project.api.Workspace;
//import org.gephi.ranking.api.Ranking;
//import org.gephi.ranking.api.RankingController;
//import org.gephi.ranking.api.Transformer;
//import org.gephi.ranking.plugin.transformer.AbstractColorTransformer;
//import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;
//import org.gephi.statistics.plugin.EigenvectorCentrality;
//import org.gephi.statistics.plugin.GraphDistance;
//import org.openide.util.Lookup;
//
///**
// * http://www.slideshare.net/gephi/gephi-toolkit-tutorialtoolkit‎
// * @author barcou
// *
// */
//public class GephiProcess {
//
//	private Workspace workspace;
//	private static Color[] colorScheme = new Color[]{
//		new Color(0x2C7BB6), new Color(0XFFFFBF), new Color(0XD7191C) 
//	};
//	//Color gradient, between 0 and 1
//	private static float[] colorPositions = new float[] {0.0f, 0.5f, 1.0f}; 
//
//	public static void dotToGraphML(String dotFile, String graphMLFile) 
//			throws IOException, SQLException {
//		//Commented out lines have been left for experimenting with improving layout.
//		
//		GephiProcess gp = new GephiProcess();
//		gp.importFile(new File(dotFile));
//		
//		gp.deleteOrphans();
//		gp.setNodeLabels();
//		
//		gp.resetEdgeWeights();
//
//		//String colCentrality = gp.metricBetweennessCentrality();
//		//String colEigen = gp.metricEigenvectorCentrality(100);
//		String colOutDegree = gp.metricOutDegree();
//		String colTotalDegree = gp.metricTotalDegree();
//		
//		gp.rankNodeSize(colOutDegree, 8, 50);
//		gp.rankNodeColor(colTotalDegree);
//
//		//gp.yifanHuLayout();
//		gp.forceAtlasLayout();
//		
//		gp.exportGraphML(graphMLFile);
//	}
//
//	public GephiProcess() {
//		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
//		pc.newProject();
//		workspace = pc.getCurrentWorkspace();
//	}
//
//	public void resetEdgeWeights() {
//		//Remove edge weights because they mess with layout algorithms.
//		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//		EdgeIterable edges = graphModel.getGraph().getEdges();
//		for (Edge edge : edges) {
//			edge.setWeight(1f);
//		}
//	}
//	
//	public void setNodeLabels() throws SQLException {
//		//Set node labels from the node ID (node ID is the Topic slug)
//		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//		
//		NodeIterable nodes = graphModel.getGraph().getNodes();
//		for (Node node : nodes) {
//			String topicString = App.db.getTopicStringFromRootSequence(node.getNodeData().getId());
//			node.getNodeData().setLabel(topicString);
//		}
//	}
//	
//	public void deleteOrphans() {
//		//Delete nodes with no parents or children.
//		//TODO: Correct term for this in graph theory?
//		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//		
//		List<Node> orphans = new ArrayList<Node>();
//		NodeIterable nodes = graphModel.getGraph().getNodes();
//		for (Node node : nodes) {
//			if (graphModel.getGraph().getDegree(node) == 0) {
//				orphans.add(node);
//			}
//		}
//		
//		for (Node orphan : orphans) {
//			graphModel.getGraph().removeNode(orphan);
//		}
//	}
//	
//	public void yifanHuLayout() {
//		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//
//		YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
//		layout.setGraphModel(graphModel);
//		layout.resetPropertiesValues();
//		layout.setBarnesHutTheta(1.2f);
//		layout.setQuadTreeMaxLevel(10);
//		layout.setOptimalDistance(60f);
//		layout.setRelativeStrength(0.2f);
//		layout.setInitialStep(20f);
//		layout.setStepRatio(0.95f);
//		layout.setAdaptiveCooling(true);
//		layout.setConvergenceThreshold(0.0001f);
//
//		for (int i = 0; i < 100 && layout.canAlgo(); i++) {
//			layout.goAlgo();
//		}
//	}
//	
//	public void forceAtlasLayout() {
//		ForceAtlasLayout layout = new ForceAtlas().buildLayout();//new ForceAtlasLayout(null);
//		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//		layout.resetPropertiesValues();
//		layout.setGraphModel(graphModel);
//		layout.setInertia(0.1);
//		layout.setRepulsionStrength(300.0);
//		layout.setAdjustSizes(true);
//		layout.setSpeed(2.0);
//		
//		for (int i = 0; i < 20000 && layout.canAlgo(); i++) {
//			layout.goAlgo();
//		}
//	}
//
//	public void exportGraphML(String filename) throws IOException {
//		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
//		Exporter exporterGraphML = ec.getExporter("graphml");     //Get GraphML exporter
//		exporterGraphML.setWorkspace(workspace);
//		ec.exportWriter(new FileWriter(filename), (CharacterExporter) exporterGraphML);
//	}
//	
//	public void exportSVG(String filename) throws IOException {
//		SVGExporter pnge = new SVGExporter();
//		pnge.setWorkspace(workspace);
//		FileWriter fw = new FileWriter(filename);
//		try {
//			pnge.setWriter(fw);
//			pnge.execute();
//		} finally {
//			fw.close();
//		}
//	}
//
//	public String metricOutDegree() {
//		return Ranking.OUTDEGREE_RANKING;
//	}
//	
//	public String metricTotalDegree() {
//		return Ranking.DEGREE_RANKING;
//	}
//	
//	public String metricEigenvectorCentrality(int numRuns) {
//		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//		AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
//
//		
//		EigenvectorCentrality distance = new EigenvectorCentrality();
//		distance.setNumRuns(numRuns);
//		distance.setDirected(true);
//		distance.execute(graphModel, attributeModel);
//		
//		return EigenvectorCentrality.EIGENVECTOR;
// 	}
//	
//	public String metricBetweennessCentrality() {
//		//Get graph model and attribute model of current workspace
//		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//		AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
//		
//		//Get Centrality
//		GraphDistance distance = new GraphDistance();
//		distance.setDirected(true);
//		distance.execute(graphModel, attributeModel);
//		
//		return GraphDistance.BETWEENNESS;
// 	}
//	
//	@SuppressWarnings("rawtypes")
//	public void rankNodeSize(String metricId, int minSize, int maxSize) {
//		RankingController rc = Lookup.getDefault().lookup(RankingController.class);
//		
//		Ranking ranking = rc.getModel().getRanking(Ranking.NODE_ELEMENT, metricId);
//		AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rc.getModel()
//					.getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
//		sizeTransformer.setMinSize(minSize);
//		sizeTransformer.setMaxSize(maxSize);
//		rc.transform(ranking, sizeTransformer);
//	}
//
//	@SuppressWarnings("rawtypes")
//	public void rankNodeColor(String metricId) {
//		RankingController rc = Lookup.getDefault().lookup(RankingController.class);
//		
//		Ranking ranking = rc.getModel().getRanking(Ranking.NODE_ELEMENT, metricId);
//		AbstractColorTransformer colorTransformer = (AbstractColorTransformer) rc.getModel()
//					.getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR);
//		colorTransformer.setColors(colorScheme);
//		colorTransformer.setColorPositions(colorPositions);
//		rc.transform(ranking, colorTransformer);
//	}
//	
//	public void importFile(File inputFile) throws IOException {
//		InputStream stream = FileUtils.openInputStream(inputFile);
//		ImportController importController = Lookup.getDefault().lookup(ImportController.class);
//		FileImporter fileImporter = importController.getFileImporter(inputFile);
//		Container container = importController.importFile(stream, fileImporter);
//		container.getLoader().setEdgeDefault(EdgeDefault.DIRECTED);
//
//		// Append imported data to GraphAPI
//		importController.process(container, new DefaultProcessor(), workspace);
//	}
//}
