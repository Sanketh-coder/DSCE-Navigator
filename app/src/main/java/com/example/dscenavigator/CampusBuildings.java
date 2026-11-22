package com.example.dscenavigator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// Building numbers and what is inside each building.
public class CampusBuildings {

    public static class Building {
        public final int no;
        public final String title;          // "Building No. 19"
        public final String[] depts;        // departments / offices inside
        // Names of places in CampusData that belong to this building.
        // The FIRST one is where the numbered pin is drawn. Leave empty if the
        // building has no GPS position yet.
        public final String[] placeNames;
        // Optional extra text (floors, labs, timings...). Shown in the side panel.
        public final String info;

        Building(int no, String[] depts, String[] placeNames, String info) {
            this.no = no;
            this.title = "Building No. " + no;
            this.depts = depts;
            this.placeNames = placeNames;
            this.info = info;
        }

        public String anchor() {
            return placeNames.length > 0 ? placeNames[0] : null;
        }
    }

    private static final List<Building> LIST = new ArrayList<>();
    // Department name -> the exact place in CampusData to walk to (when it is not the first place)
    private static final HashMap<String, String> DEPT_PLACE = new HashMap<>();

    private static String[] d(String... s) {
        return s;
    }

    private static String[] p(String... s) {
        return s;
    }

    private static void add(int no, String[] depts, String[] places) {
        LIST.add(new Building(no, depts, places, ""));
    }

    static {
        add(1, d("Heritage Block", "Principal Chamber"), p("Heritage Block"));
        add(2, d("Library", "Xerox"), p("Library"));
        add(3, d("Automobile Engineering"), p("Automobile Engineering"));
        add(4, d("Aeronautical Engineering", "Mathematics", "Civil Engineering",
                        "Construction Tech & Management"),
                p("Civil Engineering", "Aeronautical Engineering"));
        add(5, d("Mechanical Engineering"), p("Mechanical Department"));
        add(6, d(), p());
        add(7, d("Electrical Engineering", "Biological Science"), p("Biological Department"));
        add(8, d("Robotics"), p());
        add(9, d("Chemical Engineering", "Chemistry"), p("Chemical Engineering Department"));
        add(10, d("CD Sagar Auditorium", "Biotechnology", "Innovation and Leadership", "Pharmacy"),
                p("CD Sagar Auditorium"));
        add(11, d("NRI Hostel", "Sports"), p("NRI Hostel"));
        add(12, d("Physiotherapy", "PU College", "Nursing", "Evening College"),
                p("Dayananda Sagar PUC College"));
        add(13, d("Arts, Science & Commerce", "Fine Arts", "Journalism",
                        "Junior Business School (DSJBS)", "MCA (VTU)", "RIIC"),
                p("Arts, Science and Commerce"));
        add(14, d("Architecture"), p("Architecture"));
        add(15, d("PGDM (DSBS)"), p("PGDM (DSBS)"));
        add(16, d("International School"), p());
        add(17, d("Electronics & Communication", "Telecommunication & Engineering",
                        "MCA (BU)", "MBA (BU)", "BCA (BU)"),
                p("Electronic Block"));
        add(18, d("AIML"), p("AIML Department"));
        add(19, d("CSE", "ISE", "Physics"), p("CSE Department"));
        add(20, d("Girls Hostel 2"), p());
        add(21, d("Girls Hostel 3"), p());
        add(22, d("Electronics & Instrumentation", "Medical Electronics"),
                p("Electronics and Instrumentation"));
        add(23, d("Boys Hostel (Sardar Patel Hostel)"), p());
        add(24, d("Diploma Polytechnic"), p("Diploma Polytechnic"));
        add(25, d("Dental College"), p("Dental Block"));
        add(26, d("Conveno", "Hall of Admission"), p("Conveno", "Admission Hall"));
        add(27, d("Canteen"), p("College Canteen"));

        DEPT_PLACE.put("Hall of Admission", "Admission Hall");
    }

    public static List<Building> all() {
        return LIST;
    }

    public static Building byNumber(int no) {
        for (Building b : LIST) {
            if (b.no == no) {
                return b;
            }
        }
        return null;
    }

    public static Building forDept(String dept) {
        for (Building b : LIST) {
            for (String x : b.depts) {
                if (x.equalsIgnoreCase(dept)) {
                    return b;
                }
            }
        }
        return null;
    }

    public static Building forTitle(String title) {
        for (Building b : LIST) {
            if (b.title.equalsIgnoreCase(title)) {
                return b;
            }
        }
        return null;
    }

    public static Building forPlaceName(String placeName) {
        for (Building b : LIST) {
            for (String x : b.placeNames) {
                if (x.equals(placeName)) {
                    return b;
                }
            }
        }
        return null;
    }

    // The place to walk to for a department inside a building
    public static String placeForDept(String dept, Building b) {
        for (String key : DEPT_PLACE.keySet()) {
            if (key.equalsIgnoreCase(dept)) {
                return DEPT_PLACE.get(key);
            }
        }
        return b.anchor();
    }

    // "Building 19", "Building No. 19" -> 19, anything else -> -1
    public static int parseBuildingNumber(String text) {
        String t = text.trim().toLowerCase();
        if (!t.startsWith("building")) {
            return -1;
        }
        String digits = t.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Everything the search box can suggest: "Building No. 19" and every department name
    public static List<String> searchNames() {
        List<String> names = new ArrayList<>();
        for (Building b : LIST) {
            names.add(b.title);
            for (String dept : b.depts) {
                if (!names.contains(dept)) {
                    names.add(dept);
                }
            }
        }
        return names;
    }
}