package de.citec.sc.rocknrole.transforming;

import de.citec.sc.rocknrole.graph.*;
import de.citec.sc.rocknrole.graph.interpreter.GraphReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cunger
 */
public class RuleTransformer implements Transformer {

    boolean verbose = false;
    
    GraphReader reader = new GraphReader();
    
    
    @Override
    public Graph transform(Graph graph) {
        
        if (verbose) System.out.println("\nDependency graph:\n" + graph.toString());
        
        
        String who   = "who";
        String when  = "when";
        String where = "where";
        String why   = "why";
        
        List<String> COP = new ArrayList<>();
        COP.add("be");
        COP.add("is");
        COP.add("was");
        COP.add("are");
        COP.add("were");
        
        List<String> DTs = new ArrayList<>();
        DTs.add("the");
        DTs.add("this");
        DTs.add("these");
        
        List<String> WDTs = new ArrayList<>();
        WDTs.add("what");
        WDTs.add("which");
 
        
        // Normalizing edges 

        Map<String,String> normalize = new HashMap<>();
        for (String s : COP)  normalize.put(s,"BE");
        for (String s : DTs)  normalize.put(s,"THIS");
        for (String s : WDTs) normalize.put(s,"WH");

        
        for (Node n : graph.getNodes()) {
            if (normalize.containsKey(n.getForm().toLowerCase())) {
                n.setForm(normalize.get(n.getForm().toLowerCase()));
            }
        }
        
        
        if (verbose) System.out.println("\nNormalized dependency graph:\n"+graph.toString());
        
        
        // Collapsing nodes
        
        String[] compoundLabels = {"nn","number","mwe","compound"};
        for (String label : compoundLabels) {
             List<Edge> toCollapse = graph.getEdges(label);
             for (Edge e : toCollapse) {
                  Node head = graph.getNode(e.getHead());
                  Node dpnd = graph.getNode(e.getDependent());
                  head.setForm(dpnd.getForm() + " " + head.getForm());
                  graph.deleteNode(dpnd);
                  graph.deleteEdge(e);
                  graph.renameNode(dpnd.getId(),head.getId());
             }             
        }
        
        
        // Determiners 
        
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"det(*-2,THIS-1)")) {
                        
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(2),"SPEC",m.get(1)));
        }
        
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"det(*-2,WH-1)")) {
                        
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            Node n = graph.getNode(m.get(2));
            n.setForm("RETURN:" + n.getForm());
        }
        
        // TODO how many
        // TODO advmod(*/JJ-1,how-2)
        
        for (Node n : graph.getNodes()) {
             if (n.getForm().equals(who)) {
                 n.setForm("RETURN:agent");
             }
             if (n.getForm().equals(when)) {
                 n.setForm("RETURN:datetime");
             }
             if (n.getForm().equals(where)) {
                 n.setForm("RETURN:location");
             }
             if (n.getForm().equals(why)) {
                 n.setForm("RETURN:reason");
             }
             // everything else not yet covered
             if (n.getForm().equals("WH")) {
                 n.setForm("RETURN");
             }
        }
        
                
        // Copulatives
        
        String[] subs = { "cop(BE-2,*-1) \n nsubj(*-1,*-3)", 
                          "cop(*-1,BE-2) \n nsubj(*-1,*-3)", 
                          "dep(BE-2,*-1) \n nsubj(BE-2,*-3)",
                          "aux(*-1,BE-2) \n nsubj(*-1,*-3)"
                        };
        for (String sub : subs) {
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,sub)) {
            
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(3),"COP",m.get(1)));
        }}
        
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"nsubj(BE-1,*-4) \n prep(BE-1,*-2) \n pobj(*-2,*-3)")) {
            
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(4),graph.getNode(m.get(2)).getForm(),m.get(3)));
        }
        
        
        // Argument structure
                       
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"prep(*-1,*-2) \n pobj(*-2,*-3)")) {
            
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(1),graph.getNode(m.get(2)).getForm(),m.get(3)));
        }
        
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"nsubj(*-1,*-2) \n dobj(*-1,*-3)")) {
                       
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(2),graph.getNode(m.get(1)).getForm(),m.get(3)));
        }

        
        // Passives 
        
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"auxpass(*-1,BE-2) \n nsubjpass(*-1,*-3) \n dobj(*-1,*-4)")) {
            
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(3),graph.getNode(m.get(1)).getForm(),m.get(4)));
        }
        
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"auxpass(*-1,BE-2) \n nsubjpass(*-1,*-3)")) {
            
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(3),"COP",m.get(1)));
        }
        
        
        // Modifiers
        
        String[] mods = {"amod"}; // ,"advmod"
        for (String mod : mods) {
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,mod+"(*-1,*-2)")) {
                        
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(1),"MOD",m.get(2)));
        }} 
        
        // Numericals 
        // TODO also requires some normalization 
        
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"num(*-1,*-2)")) {
                        
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(1),"NUM",m.get(2)));
        }
        
        
        // Coordination
        
        for (Pair<Graph,Map<Integer,Integer>> subgraph : getSubgraphs(graph,"cc(*-1,*-2) \n conj(*-1,*-3)")) {
                        
            Graph g = subgraph.getLeft();
            Map<Integer,Integer> m = subgraph.getRight();
            
            graph.addEdge(new Edge(Edge.Color.SEM,m.get(1),graph.getNode(m.get(2)).getForm(),m.get(3)));
        }
        
        
        // Finally, keep only semantic edges
        
        Graph sem_graph = new Graph();
        
        for (Edge e : graph.getEdges()) {
             if (e.getColor() == Edge.Color.SEM) {
                 sem_graph.addEdge(e);
                 sem_graph.addNode(graph.getNode(e.getHead()));
                 sem_graph.addNode(graph.getNode(e.getDependent()));
             }
        }
        
        // and clean up errors 
        // TODO 
        
        for (Edge e : graph.getEdges()) {
             if (graph.getNode(e.getHead()).getForm().equals("BE") || 
                (graph.getNode(e.getHead()).getForm().equals("THE") && graph.getNode(e.getDependent()).getForm().equals("RETURN"))) {
                 sem_graph.deleteEdge(e);
             }
        }
        
        
        if (verbose) System.out.println("\nSemGraph:\n" + sem_graph.toString());

        
        return sem_graph;
    }
    
    
    private List<Pair<Graph,Map<Integer,Integer>>> getSubgraphs(Graph graph, String regex) {
               
        Graph subgraph = reader.interpret(regex);
                
        return subgraph.subGraphMatches(graph);
    }
    
}
