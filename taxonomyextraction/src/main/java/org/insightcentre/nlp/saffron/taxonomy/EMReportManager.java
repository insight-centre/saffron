package org.insightcentre.nlp.saffron.taxonomy;

import org.insightcentre.nlp.saffron.taxonomy.graph.AdjacencyList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.insightcentre.nlp.saffron.taxonomy.graph.Edge;

/**
 * @author Georgeta Bordea
 * 
 *         Method used to evaluate a taxonomy in the EF and EP tasks
 */
public class EMReportManager {
//
//  private static Logger logger = Logger.getLogger(EMReportManager.class
//      .getName());
//
//  public static final String TAXONOMY_PATH =
//      "/home/georgeta/work/DevelopementLog/Experiments/VelardiTaxonomy/"
//          //+ "HierarchicalClustering/"
//          + "MyGraphs/"
//          //+ "VelardiGraphs/"
//          //-------------------------------------------- 
//          // + "UvT/5000_Sim_UvT.dot";
//          // + "UvT/6000_Sim_UvT/prunedGraph.dot";
//          //-------------------------------------------- 
//          // + "ACL/top1000.dot";
//          + "ACL/ACL_t10k_OptSubstr.dot";         
//          // + "ACL/t15k_FixedSubstr_Span50_MinDoc1.dot";
//          // + "ACL/ACL_t10k_HeadInclusion_Span5_MinDoc3.dot";
//          //+ "ACL/acl_t10k_OccSim_useFreq.dot";
//          //+"ACL/acl_t1626_DBSim_useFreq.dot";
//          //+"ACL/acl_t1626_DBSim_usePMI.dot";
//          //+"ACL/acl_t1626_OccSim_useFreq.dot";
//          //+"ACL/acl_t1626_OccSim_usePMI.dot";
//          //+ "ACL/acl_t10k_OccSim_usePMI.dot";
//          //+ "ComputationalLinguisticsTaxonomy_modified.dot";
//          //--------------------------------------------
//          //+ "SW/SW_t10k_OptimSubstr_Span5_MinDoc1.dot";
//          //+ "SW/sw_t10k_mindoc1_useFreq.dot";
//          //+ "UvT/uvt_t5k_mindoc1_useFreq.dot";
//          // + "UvT/t8k_Span50_MinDoc1.dot";
//          //+ "IR_Toine/toine_t3k_mindoc1_useFreq.dot";
//          //+ "IR/t3kToineIR.dot";
//          //+ "IR_Toine/toine_t4k_mindoc3_useFreq.dot";
//          //+ "IR/toineAll_t4k_MinDoc3_Span5.dot";
//  // + "WebSci/cleanedHierarchy.dot";
//
//
//  private static final Integer PROFILE_LIMIT = 50;
//  
//  private static final Integer MAX_PATH_LENGTH = 3;
//
//  // + "1700_Sim_UvT.dot";
//
//  /**
//   * @param args
// * @throws SQLException 
// * @throws IOException 
//   */
//  public static void main(String[] args) throws IOException, SQLException {
//    DAO db = null; // TODO: Fix this?
//      
//    String gsFile =
//        "/home/georgeta/work/DevelopementLog/"
//            + "Experiments/Experts_GoldStandard/acl/ep_gs.txt";
//    // "/home/georgeta/work/Corpora/UVT_Mine/epGS_DB.txt";
//    String gsTopicsFile =
//        "/home/georgeta/work/DevelopementLog/Experiments/"
//            + "Experts_GoldStandard/acl/gsTopics.txt";
//    // "/home/georgeta/work/Corpora/UVT_Mine/gsTopicsStrings.txt";
//
//    logger.log(Level.INFO, "Report top topics for reasearchers..");
//
//    // best 2
//      reportTopTopicsForResearchers(db, 2, "ep_tax_results.txt",
//          readGSResNames(gsFile),
//          readStemmedGSTopics(gsTopicsFile), 0);
//
//    /*
//     * System.out.println("Topic,Score,Frequency"); SaffronDotExporter<String,
//     * DefaultEdge> sde = new SaffronDotExporter<String, DefaultEdge>(); String
//     * root = "search engine";
//     * 
//     * try { FileWriter w = new FileWriter(new File(root.replace(" ", "_") +
//     * ".dot")); String[] exclude = {"web page"}; sde.export(w,
//     * extractSubtree(TAXONOMY_PATH, root, exclude, new
//     * DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class))); } catch
//     * (IOException e) { e.printStackTrace(); }
//     */
//
//    logger.log(Level.INFO, "Done..");
//  }
//
//  public static List<String> readGSResNames(String fileName) throws IOException {
//	    List<String> resNames = new ArrayList<String>();
//
//	      for (String line : FileUtils.readLines(new File(fileName))) {
//	        resNames.add(line.substring(0, line.indexOf(":") - 1));
//	      }
//	    return resNames;
//	  }
//  
//  public static List<String> readStemmedGSTopics(String fileName) throws IOException {
//	    List<String> topicNames = new ArrayList<String>();
//
//	      for (String line : FileUtils.readLines(new File(fileName))) {
//	        String stem = StemUtils.stemPhrase(line);
//	        if (!topicNames.contains(stem))
//	          topicNames.add(stem);
//	      }
//	    return topicNames;
//	  }
//  
//  /**
//   * Report the top experts for topics in the given list of topic names
//   * 
//   * @param conn
//   * @param outputFileName
//   * @param resList
//   * @throws IOException
//   * @throws SQLException
//   */
//  public static void reportTopTopicsForResearchers(DAO db, Integer taxMethod,
//      String outputFileName, List<String> resList, List<String> gsTopics,
//      Integer rankType) throws IOException, SQLException {
//    BufferedWriter out = null;
//
//    try {
//      // Create file
//      FileWriter fstream = new FileWriter(outputFileName, false);
//      out = new BufferedWriter(fstream);
//      List<String> topicList = new ArrayList<String>();
//
//      logger.log(Level.INFO, "Load taxonomy graph..");
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy =
//          SaffronDotImporter.importGraph(TAXONOMY_PATH);
//
//      for (String resId : resList) {
//
//        logger.log(Level.INFO, "Construct profile for researcher " + resId);
//        Set<String> topicsSet =
//        		db.topScoringTopicsForResearcher(resId, PROFILE_LIMIT)
//                .keySet();
//        List<String> topicsList = new ArrayList<String>(topicsSet);
//
//        switch (taxMethod) {
//        case 0:
//          // expand with parents
//          logger.log(Level.INFO, "Expand profile with parents..");
//          topicList = expandProfileWithParents(topicsList, taxonomy);
//          break;
//        case 1:
//          // expand with children
//          logger.log(Level.INFO, "Expand profile with children..");
//          topicList = expandProfileWithChildren(topicsList, taxonomy);
//          break;
//        case 2:
//          // expand with children and parents
//          logger.log(Level.INFO, "Expand profile with children and parents..");
//          topicList = expandProfileWithChildrenAndParents(topicsList, taxonomy);
//          break;
//        case 3:
//          // expand with grandparents
//          logger.log(Level.INFO, "Expand profile with grandparents..");
//          topicList = expandProfileWithParentsSecondLevel(topicsList, taxonomy);
//          break;
//        case 4:
//          // expand with grand-grandparents
//          logger.log(Level.INFO, "Expand profile with grand-grandparents..");
//          topicList = expandProfileWithParentsThirdLevel(topicsList, taxonomy);
//          break;
//        case 5:
//          // expand with grand-grand-grandparents
//          logger.log(Level.INFO,
//              "Expand profile with grand-grand-grandparents..");
//          topicList = expandProfileWithParentsFourthLevel(topicsList, taxonomy);
//          break;
//        case 6:
//          // expand with second level children
//          logger.log(Level.INFO, "Expand profile with children and parents..");
//          topicList =
//              expandProfileWithChildrenSecondLevel(topicsList, taxonomy);
//          break;
//        case 7:
//          // expand with parent and siblings
//          logger.log(Level.INFO, "Expand profile with parent and siblings..");
//          topicList = expandProfileWithParentAndSiblings(topicsList, taxonomy);
//          break;
//        case 8:
//          // expand with all the parents
//          logger.log(Level.INFO,
//              "Expand profile with all the parents up to the root..");
//          topicList = expandProfileWithAllAncestors(topicsList, taxonomy);
//          break;
//        }
//
//        logger.log(Level.INFO, "Output profiles to file..");
//        if (topicList.size() > 0) {
//          out.write(resId + " : ");
//          String topicsString = "";
//          for (String topic : topicList) {
//            String stem = StemUtils.stemPhrase(topic);
//
//            if (gsTopics.contains(stem)) {
//              topicsString += stem + ",";
//            }
//          }
//
//          if (topicsString.length() > 0) {
//            out.write(topicsString.substring(0, topicsString.length() - 1)
//                + "\n");
//          } else {
//            out.write("\n");
//          }
//        } else {
//          out.write(resId + " : \n");
//        }
//      }
//    } finally {
//      // Close the output stream
//      if (out != null) {
//        out.close();
//      }
//    }
//  }
//
//  public static List<String> expandProfileWithParents(List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.incomingEdgesOf(topic);
//        for (DefaultWeightedEdge edge : edges) {
//          String edgeString = edge.toString();
//          String source = edgeString.substring(1, edgeString.indexOf(" : "));
//          if (!expandedProfile.contains(source)) {
//            expandedProfile.add(source);
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithParentsDegreeFilter(
//      Integer minOutDegree, List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.incomingEdgesOf(topic);
//        for (DefaultWeightedEdge edge : edges) {
//          String edgeString = edge.toString();
//          String source = edgeString.substring(1, edgeString.indexOf(" : "));
//
//          Set<DefaultWeightedEdge> siblingEdges =
//              taxonomy.incomingEdgesOf(source);
//
//          if ((!expandedProfile.contains(source))
//              && (siblingEdges.size() > minOutDegree)) {
//            expandedProfile.add(source);
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithParentsSecondLevel(
//      List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.incomingEdgesOf(topic);
//        for (DefaultWeightedEdge edge : edges) {
//          String edgeString = edge.toString();
//          String source = edgeString.substring(1, edgeString.indexOf(" : "));
//          if (!expandedProfile.contains(source)) {
//            expandedProfile.add(source);
//          }
//
//          Set<DefaultWeightedEdge> secondLevelEdges =
//              taxonomy.incomingEdgesOf(source);
//          for (DefaultWeightedEdge edge1 : secondLevelEdges) {
//            edgeString = edge1.toString();
//            source = edgeString.substring(1, edgeString.indexOf(" : "));
//            if (!expandedProfile.contains(source)) {
//              expandedProfile.add(source);
//            }
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithParentsThirdLevel(
//      List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.incomingEdgesOf(topic);
//        for (DefaultWeightedEdge edge : edges) {
//          String edgeString = edge.toString();
//          String source = edgeString.substring(1, edgeString.indexOf(" : "));
//          if (!expandedProfile.contains(source)) {
//            expandedProfile.add(source);
//          }
//
//          Set<DefaultWeightedEdge> secondLevelEdges =
//              taxonomy.incomingEdgesOf(source);
//          for (DefaultWeightedEdge edge1 : secondLevelEdges) {
//            edgeString = edge1.toString();
//            source = edgeString.substring(1, edgeString.indexOf(" : "));
//            if (!expandedProfile.contains(source)) {
//              expandedProfile.add(source);
//            }
//
//            Set<DefaultWeightedEdge> thirdLevelEdges =
//                taxonomy.incomingEdgesOf(source);
//            for (DefaultWeightedEdge edge2 : thirdLevelEdges) {
//              edgeString = edge2.toString();
//              source = edgeString.substring(1, edgeString.indexOf(" : "));
//              if (!expandedProfile.contains(source)) {
//                expandedProfile.add(source);
//              }
//            }
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithParentsFourthLevel(
//      List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.incomingEdgesOf(topic);
//        for (DefaultWeightedEdge edge : edges) {
//          String edgeString = edge.toString();
//          String source = edgeString.substring(1, edgeString.indexOf(" : "));
//          if (!expandedProfile.contains(source)) {
//            expandedProfile.add(source);
//          }
//
//          Set<DefaultWeightedEdge> secondLevelEdges =
//              taxonomy.incomingEdgesOf(source);
//          for (DefaultWeightedEdge edge1 : secondLevelEdges) {
//            edgeString = edge1.toString();
//            source = edgeString.substring(1, edgeString.indexOf(" : "));
//            if (!expandedProfile.contains(source)) {
//              expandedProfile.add(source);
//            }
//
//            Set<DefaultWeightedEdge> thirdLevelEdges =
//                taxonomy.incomingEdgesOf(source);
//            for (DefaultWeightedEdge edge2 : thirdLevelEdges) {
//              edgeString = edge2.toString();
//              source = edgeString.substring(1, edgeString.indexOf(" : "));
//              if (!expandedProfile.contains(source)) {
//                expandedProfile.add(source);
//              }
//
//              Set<DefaultWeightedEdge> fourthLevelEdges =
//                  taxonomy.incomingEdgesOf(source);
//              for (DefaultWeightedEdge edge3 : fourthLevelEdges) {
//                edgeString = edge3.toString();
//                source = edgeString.substring(1, edgeString.indexOf(" : "));
//                if (!expandedProfile.contains(source)) {
//                  expandedProfile.add(source);
//                }
//              }
//            }
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static Map<String, Double> buildProfileUsingShortestPath(
//      Map<String, Double> topicsMap,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) {
//
//    Map<String, Double> profileMap = new LinkedHashMap<String, Double>();
//
//    Set<String> topicsSet = topicsMap.keySet();
//
//    // TODO for each area in the taxonomy increase the probability of all
//    // the topics from the profile that are closely related to it
//
//    Double globalSim = new Double(0);
//    Map<String, Double> localSimMap = new HashMap<String, Double>();
//    for (String topic1 : topicsSet) {
//      Double localSim = new Double(0);
//      for (String topic2 : topicsSet) {
//        if (!topic1.equals(topic2)) {
//          if ((taxonomy.containsVertex(topic1))
//              && (taxonomy.containsVertex(topic2))) {
//            List<DefaultWeightedEdge> path12 =
//                DijkstraShortestPath.findPathBetween(taxonomy, topic1, topic2);
//            List<DefaultWeightedEdge> path21 =
//                DijkstraShortestPath.findPathBetween(taxonomy, topic2, topic1);
//
//            if (path12 != null) {
//              Integer pathLength = path12.size();
//              if (pathLength <= MAX_PATH_LENGTH) {
//                localSim += (1 / (double) pathLength) * topicsMap.get(topic2);
//                globalSim += 1 / (double) pathLength;
//              }
//            }
//
//            if (path21 != null) {
//              Integer pathLength = path21.size();
//              if (pathLength <= MAX_PATH_LENGTH) {
//                localSim += (1 / (double) pathLength) * topicsMap.get(topic2);
//                globalSim += 1 / (double) pathLength;
//              }
//            }
//          }
//        }
//      }
//
//      localSimMap.put(topic1, localSim);
//    }
//
//    for (String topic : topicsSet) {
//
//      Double sim = localSimMap.get(topic);
//      Double newScore =
//          0.6 * topicsMap.get(topic) + 0.4 * sim / (1 + globalSim);
//      profileMap.put(topic, newScore);
//    }
//
//    Map<String, Double> mu = SaffronMapUtils.sortByValues(profileMap);
//
//    List<String> expandedProfile = new ArrayList<String>(profileMap.keySet());
//    Collections.reverse(expandedProfile);
//
//    Map<String, Double> finalProfileMap = new LinkedHashMap<String, Double>();
//
//    for (String topic : expandedProfile) {
//      finalProfileMap.put(topic, profileMap.get(topic));
//    }
//    return profileMap;
//    // return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithAllAncestors(
//      List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      List<String> path = new ArrayList<String>();
//      Boolean loop = false;
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.incomingEdgesOf(topic);
//
//        while (edges.size() > 0) {
//          for (DefaultWeightedEdge edge : edges) {
//            String edgeString = edge.toString();
//            String source = edgeString.substring(1, edgeString.indexOf(" : "));
//            // System.out.println(source);
//            if (!expandedProfile.contains(source)) {
//              expandedProfile.add(source);
//            }
//
//            // stay out of loops
//            if (path.contains(source)) {
//              loop = true;
//            } else {
//              path.add(source);
//            }
//
//            if (!loop) {
//              if (topic != source) {
//                topic = source;
//                // System.out.println(source);
//              }
//            }
//          }
//
//          if (!loop) {
//            edges = taxonomy.incomingEdgesOf(topic);
//          } else {
//            break;
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithChildren(List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.outgoingEdgesOf(topic);
//        for (DefaultEdge edge : edges) {
//          String edgeString = edge.toString();
//          String dest =
//              edgeString.substring(edgeString.indexOf(" : ") + 3,
//                  edgeString.length() - 1);
//          if (!expandedProfile.contains(dest)) {
//            expandedProfile.add(dest);
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithChildrenSecondLevel(
//      List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.outgoingEdgesOf(topic);
//        for (DefaultWeightedEdge edge : edges) {
//          String edgeString = edge.toString();
//          String dest =
//              edgeString.substring(edgeString.indexOf(" : ") + 3,
//                  edgeString.length() - 1);
//          if (!expandedProfile.contains(dest)) {
//            expandedProfile.add(dest);
//          }
//
//          Set<DefaultWeightedEdge> edges1 = taxonomy.outgoingEdgesOf(dest);
//          for (DefaultWeightedEdge edge1 : edges1) {
//            edgeString = edge1.toString();
//            dest =
//                edgeString.substring(edgeString.indexOf(" : ") + 3,
//                    edgeString.length() - 1);
//            if (!expandedProfile.contains(dest)) {
//              expandedProfile.add(dest);
//            }
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithParentAndSiblings(
//      List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//
//        Set<DefaultWeightedEdge> incomingEdges =
//            taxonomy.incomingEdgesOf(topic);
//        for (DefaultWeightedEdge parentEdge : incomingEdges) {
//          String edgeString = parentEdge.toString();
//          String parent = edgeString.substring(1, edgeString.indexOf(" : "));
//          if (!expandedProfile.contains(parent)) {
//            expandedProfile.add(parent);
//          }
//
//          Set<DefaultWeightedEdge> sibEdges = taxonomy.outgoingEdgesOf(parent);
//          for (DefaultWeightedEdge sibEdge : sibEdges) {
//            String sibEdgeString = sibEdge.toString();
//            String dest =
//                sibEdgeString.substring(sibEdgeString.indexOf(" : ") + 3,
//                    sibEdgeString.length() - 1);
//            if (!expandedProfile.contains(dest)) {
//              expandedProfile.add(dest);
//            }
//          }
//
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
//  public static List<String> expandProfileWithChildrenAndParents(
//      List<String> topicList,
//      DirectedGraph<String, DefaultWeightedEdge> taxonomy) throws SQLException {
//    List<String> expandedProfile = new ArrayList<String>(topicList);
//
//    for (String topic : topicList) {
//      if (taxonomy.containsVertex(topic)) {
//        Set<DefaultWeightedEdge> edges = taxonomy.outgoingEdgesOf(topic);
//        for (DefaultWeightedEdge edge : edges) {
//          String edgeString = edge.toString();
//          String dest =
//              edgeString.substring(edgeString.indexOf(" : ") + 3,
//                  edgeString.length() - 1);
//          if (!expandedProfile.contains(dest)) {
//            expandedProfile.add(dest);
//          }
//        }
//
//        Set<DefaultWeightedEdge> incomingEdges =
//            taxonomy.incomingEdgesOf(topic);
//        for (DefaultWeightedEdge edge : incomingEdges) {
//          String edgeString = edge.toString();
//          String source = edgeString.substring(1, edgeString.indexOf(" : "));
//          if (!expandedProfile.contains(source)) {
//            expandedProfile.add(source);
//          }
//        }
//      }
//    }
//
//    return expandedProfile;
//  }
//
  public static AdjacencyList normaliseEdgeWeights(AdjacencyList edges) {

    AdjacencyList normEdges = new AdjacencyList();
    Collection<Edge> edgeCollection = edges.getAllEdges();
    List<Edge> edgeList = new ArrayList<Edge>(edgeCollection);
    Double minWeight = edgeList.get(0).getWeight();
    Double maxWeight = edgeList.get(0).getWeight();
    for (Edge edge : edgeCollection) {
      Double weight = edge.getWeight();

      if (weight < minWeight) {
        minWeight = weight;
      }

      if ((!weight.isInfinite()) && (weight > maxWeight)) {
        maxWeight = weight;
      }
    }

    minWeight = Math.log(1 + minWeight);
    maxWeight = Math.log(1 + maxWeight);

    for (Edge edge : edgeCollection) {
      Double w = edge.getWeight();

      if (w.isInfinite()) {
        w = maxWeight;
      } else {
        w = Math.log(1 + edge.getWeight());
      }

      Double normWeight = 10000 * (w - minWeight) / (maxWeight - minWeight);
      Integer intWeight = normWeight.intValue() / 10;
      normEdges.addEdge(edge.getFrom(), edge.getTo(), intWeight);
    }

    return normEdges;
  }
//
//  /**
//   * Output subtrees for community analysis (for WebSci paper)
//   * 
//   * @param taxonomyPath
//   * @param subtreeRoot
//   * @return
// * @throws SQLException 
// * @throws IOException 
//   */
//  private static DirectedGraph<String, DefaultEdge> extractSubtree(
//      DAO db,
//      String taxonomyPath, String subtreeRoot, String[] exludedNodes,
//      DirectedGraph<String, DefaultEdge> subtree) throws SQLException, IOException {
//    DirectedGraph<String, DefaultWeightedEdge> taxonomy =
//        SaffronDotImporter.importGraph(taxonomyPath);
//
//    if (taxonomy.containsVertex(subtreeRoot)) {
//      Set<DefaultWeightedEdge> edges = taxonomy.outgoingEdgesOf(subtreeRoot);
//      for (DefaultWeightedEdge edge : edges) {
//        String edgeString = edge.toString();
//        String source = edgeString.substring(1, edgeString.indexOf(" : "));
//        String destination =
//            edgeString.substring(edgeString.indexOf(":") + 2,
//                edgeString.length() - 1);
//
//        subtree.addVertex(source);
//        subtree.addVertex(destination);
//        if (!Arrays.asList(exludedNodes).contains(destination)
//            && !source.equals(destination)) {
//          subtree.addEdge(source, destination);
//          Topic destTopic;
//          System.out.print(destination);
//            destTopic = db.getTopic(destination);
//            if (destTopic != null) {
//              System.out.println("," + destTopic.getRank() + ","
//                  + destTopic.getOverallOccurrence());
//            } else {
//              System.out.println("," + 0 + "," + 0);
//            }
//          extractSubtree(db, taxonomyPath, destination, exludedNodes, subtree);
//        }
//      }
//    }
//
//    return subtree;
//  }
}
