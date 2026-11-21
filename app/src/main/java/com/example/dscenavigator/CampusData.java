package com.example.dscenavigator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CampusData {

    public static List<Place> getPlaces() {
        List<Place> places = new ArrayList<>();
        places.add(new Place("Main Gate", "Entry point of the campus", 12.909473, 77.566848));
        places.add(new Place("CSE Department", "Floor and block details", 12.907944, 77.566058));
        places.add(new Place("Library", "Floor and block details", 12.909266, 77.567103));
        places.add(new Place("Heritage Block", "Floor and block details", 12.908958, 77.566545));
        places.add(new Place("Auditorium Front Gate", "Floor and block details", 12.908926, 77.567053));
        places.add(new Place("Auditorium Back Gate", "Floor and block details", 12.908923, 77.567496));
        places.add(new Place("Conveno", "Floor and block details", 12.908711, 77.566315));
        places.add(new Place("Dental Block", "Floor and block details", 12.908643, 77.565975));
        places.add(new Place("Diploma Polytechnic", "Floor and block details", 12.908560, 77.565754));
        places.add(new Place("Amphitheatre", "Floor and block details", 12.908101733301637, 77.56611152967048));
        places.add(new Place("AIML Department", "Floor and block details", 12.907622, 77.566029));
        places.add(new Place("Girls Hostel", "Floor and block details", 12.907585, 77.566336));
        places.add(new Place("Dayananda Sagar PUC College", "Floor and block details", 12.906493, 77.566692));
        places.add(new Place("NRI Hostel", "Floor and block details", 12.907141, 77.567376));
        places.add(new Place("Chemical Engineering Department", "Floor and block details", 12.907905, 77.567748));
        places.add(new Place("Aeronautical Engineering", "Floor and block details", 12.908702, 77.568060));
        places.add(new Place("Civil Engineering", "Floor and block details", 12.908702, 77.568060));
        places.add(new Place("Automobile Engineering", "Floor and block details", 12.909067, 77.568171));
        places.add(new Place("Student Parking Lot", "Floor and block details", 12.909237, 77.567709));
        places.add(new Place("Shavige Malleshwar Temple", "Floor and block details", 12.908541, 77.566566));
        places.add(new Place("Indian Boys Hostel", "Floor and block details", 12.908452, 77.565324));
        places.add(new Place("DSCE Rock Garden", "Floor and block details", 12.907820, 77.567192));
        places.add(new Place("College Canteen", "Floor and block details", 12.908475, 77.566277));
        places.add(new Place("Hostel Mess", "Floor and block details", 12.908493, 77.566407));
        places.add(new Place("Football Ground", "Floor and block details", 12.906981, 77.566707));
        places.add(new Place("Basketball Court", "Floor and block details", 12.906981, 77.566707));
        places.add(new Place("Tennis Court", "Floor and block details", 12.906912, 77.567126));
        places.add(new Place("Mechanical Department", "Floor and block details", 12.908553, 77.567690));
        places.add(new Place("CD Sagar Auditorium", "Floor and block details",12.907749905052118, 77.56761766399603));
        places.add(new Place("Admission Hall", "Floor and block details", 12.908722287841645, 77.56637959258036));

        places.add(new Place("Architecture", "Floor and block details", 12.90679384937078, 77.56621360837234));
        places.add(new Place("Arts, Science and Commerce", "Floor and block details", 12.907142072610876, 77.56623117802113));
        places.add(new Place("Electronic Block", "Floor and block details", 12.907690490062638, 77.56561601351667));
        places.add(new Place("Biological Department", "Floor and block details", 12.908250304402378, 77.56775802405691));
        places.add(new Place("Electronics and Instrumentation", "Floor and block details", 12.90833332817328, 77.56560114128118));
        places.add(new Place("PGDM (DSBS)", "Floor and block details", 12.907326101760127, 77.5658296316933));

        return places;
    }

    // Joint buildings: the route to the first place goes through the second one
    public static HashMap<String, String> getViaRules() {
        HashMap<String, String> via = new HashMap<>();
        via.put("AIML Department", "CSE Department");
        return via;
    }

    public static CampusGraph buildGraph() {
        CampusGraph graph = new CampusGraph();

        int p_junction_1 = graph.addNode("Junction 1", 12.9094052, 77.5668583);
        int p_library_junction = graph.addNode("Library Junction", 12.9092662, 77.5668455);
        int p_bend_1 = graph.addNode("Bend 1", 12.9089289, 77.5668348);
        int p_bend_2 = graph.addNode("Bend 2", 12.9089320, 77.5666606);
        int p_junction_2 = graph.addNode("Junction 2", 12.9086175, 77.5666449);
        int p_junction_3 = graph.addNode("Junction 3", 12.9086280, 77.5663599);
        int p_roundabout = graph.addNode("Roundabout", 12.9086244, 77.5662287);
        int p_junction_cse_4 = graph.addNode("Junction CSE 4", 12.9084451, 77.5659496);
        int p_junction_cse_5 = graph.addNode("Junction CSE 5", 12.9081077, 77.5660107);
        int p_junction_6 = graph.addNode("Junction 6", 12.9083832, 77.5657356);
        int p_bend_3 = graph.addNode("Bend 3", 12.9084233, 77.5654849);
        int p_bend_4 = graph.addNode("Bend 4", 12.9081039, 77.5663270);
        int p_bend_5 = graph.addNode("Bend 5", 12.9082578, 77.5663728);
        int p_bend_6 = graph.addNode("Bend 6", 12.9082532, 77.5665524);
        int p_junction_7 = graph.addNode("Junction 7", 12.9080376, 77.5666879);
        int p_bend_7 = graph.addNode("Bend 7", 12.9080789, 77.5668159);
        int p_bend_8 = graph.addNode("Bend 8", 12.9081806, 77.5669238);
        int p_bend_9 = graph.addNode("Bend 9", 12.9083541, 77.5669492);
        int p_bend_10 = graph.addNode("Bend 10", 12.9083768, 77.5670771);
        int p_junction_8 = graph.addNode("Junction 8", 12.9086280, 77.5671137);
        int p_bend_11 = graph.addNode("Bend 11", 12.9086326, 77.5668333);
        int p_junction_9 = graph.addNode("Junction 9", 12.9094722, 77.5663899);
        int p_bend_12 = graph.addNode("Bend 12", 12.9091443, 77.5662471);
        int p_junction_10 = graph.addNode("Junction 10", 12.9093885, 77.5675922);
        int p_junction_11 = graph.addNode("Junction 11", 12.9086310, 77.5675715);
        int p_junction_13 = graph.addNode("Junction 13", 12.9084267, 77.5675558);
        int p_junction_14 = graph.addNode("Junction 14", 12.9075774, 77.5669868);
        int p_junction_15 = graph.addNode("Junction 15", 12.9074503, 77.5672472);
        int p_junction_16 = graph.addNode("Junction 16", 12.9075440, 77.5667621);
        int p_junction_17 = graph.addNode("Junction 17", 12.9074901, 77.5664218);
        int p_bend_13 = graph.addNode("Bend 13", 12.9074379, 77.5659815);
        int p_bend_14 = graph.addNode("Bend 14", 12.9089624, 77.5675859);
        int p_bend_15 = graph.addNode("Bend 15", 12.9081433, 77.5675357);
        int p_bend_16 = graph.addNode("Bend 16", 12.9077303, 77.5673889);
        int p_bend_17 = graph.addNode("Bend 17", 12.9077856, 77.5667137);
        int p_bend_18 = graph.addNode("Bend 18", 12.9070822, 77.5663907);
        int p_bend_19 = graph.addNode("Bend 19", 12.9071994, 77.5669767);
        int p_junction_18 = graph.addNode("Junction 18", 12.9079693, 77.5656873);
        int p_bend_20 = graph.addNode("Bend 20", 12.9084620, 77.5663578);
        int p_bend_22 = graph.addNode("Bend 22", 12.9067329, 77.5669762);
        int p_point_42 = graph.addNode("Point 42", 12.9074695, 77.5669757);
        int p_point_43 = graph.addNode("Point 43", 12.9078798, 77.5674921);
        int p_point_44 = graph.addNode("Point 44", 12.9073679, 77.5675854);
        int p_point_45 = graph.addNode("Point 45", 12.9094050, 77.5680605);
        int p_point_46 = graph.addNode("Point 46", 12.9093030, 77.5681497);
        int p_point_47 = graph.addNode("Point 47", 12.9085478, 77.5661648);
        int p_point_48 = graph.addNode("Point 48", 12.9076531, 77.5671837);
        int p_point_49 = graph.addNode("Point 49", 12.9093918, 77.5673870);

        graph.connect(p_junction_1, p_library_junction);
        graph.connect(p_library_junction, p_bend_1);
        graph.connect(p_bend_1, p_bend_2);
        graph.connect(p_bend_2, p_junction_2);
        graph.connect(p_junction_2, p_junction_3);
        graph.connect(p_junction_3, p_roundabout);
        graph.connect(p_junction_9, p_bend_12);
        graph.connect(p_bend_12, p_roundabout);
        graph.connect(p_junction_cse_4, p_junction_cse_5);
        graph.connect(p_junction_cse_4, p_junction_6);
        graph.connect(p_junction_6, p_bend_3);
        graph.connect(p_junction_cse_5, p_bend_4);
        graph.connect(p_bend_4, p_bend_5);
        graph.connect(p_bend_5, p_bend_6);
        graph.connect(p_bend_6, p_junction_7);
        graph.connect(p_junction_7, p_bend_7);
        graph.connect(p_bend_7, p_bend_8);
        graph.connect(p_bend_8, p_bend_9);
        graph.connect(p_bend_9, p_bend_10);
        graph.connect(p_bend_10, p_junction_8);
        graph.connect(p_junction_8, p_bend_11);
        graph.connect(p_bend_11, p_junction_2);
        graph.connect(p_junction_10, p_bend_14);
        graph.connect(p_bend_14, p_junction_11);
        graph.connect(p_junction_11, p_junction_13);
        graph.connect(p_junction_13, p_bend_15);
        graph.connect(p_bend_13, p_junction_17);
        graph.connect(p_junction_17, p_junction_16);
        graph.connect(p_junction_16, p_junction_14);
        graph.connect(p_junction_3, p_bend_20);
        graph.connect(p_junction_7, p_bend_17);
        graph.connect(p_bend_17, p_junction_16);
        graph.connect(p_junction_17, p_bend_18);
        graph.connect(p_junction_14, p_bend_19);
        graph.connect(p_bend_19, p_bend_22);
        graph.connect(p_junction_6, p_junction_18);
        graph.connect(p_junction_14, p_point_42);
        graph.connect(p_junction_15, p_point_42);
        graph.connect(p_point_43, p_bend_16);
        graph.connect(p_point_43, p_bend_15);
        graph.connect(p_junction_8, p_junction_11);
        graph.connect(p_junction_15, p_point_44);
        graph.connect(p_junction_10, p_point_45);
        graph.connect(p_point_45, p_point_46);
        graph.connect(p_bend_20, p_bend_5);
        graph.connect(p_roundabout, p_point_47);
        graph.connect(p_point_47, p_junction_cse_4);
        graph.connect(p_bend_16, p_point_48);
        graph.connect(p_point_48, p_junction_14);
        graph.connect(p_junction_1, p_point_49);
        graph.connect(p_point_49, p_junction_10);
        return graph;
    }

    // Buildings with a second entrance: name -> {latitude, longitude} of the back gate
    public static HashMap<String, double[]> getBackGates() {
        HashMap<String, double[]> gates = new HashMap<>();
        gates.put("AIML Department", new double[]{12.907448, 77.566131});
        return gates;
    }


    // Departments (or other names) that live inside a building: search name -> place name.
// Typing either name finds the same building and the same route.
// To add more, copy a line and change the two names. The second name must match a place exactly.
    public static HashMap<String, String> getAliases() {
        HashMap<String, String> a = new HashMap<>();
//        HashMap<String, String> b = new HashMap<>();
        a.put("ISE Department", "CSE Department");     // CHECK: ISE is in the same building as CSE
        a.put("Building No. 19", "CSE Department");

        a.put("Building No. 10","CD Sagar Auditorium");

        // CHECK: Building No. 19 is the CSE building
        return a;


    }

}