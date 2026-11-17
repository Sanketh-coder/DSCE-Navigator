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
        places.add(new Place("Convocation Hall", "Floor and block details", 12.908711, 77.566315));
        places.add(new Place("Dental Block", "Floor and block details", 12.908643, 77.565975));
        places.add(new Place("Polytechnic", "Floor and block details", 12.908560, 77.565754));
        places.add(new Place("Amphitheatre", "Floor and block details", 12.908090, 77.566179));
        places.add(new Place("AIML Department", "Floor and block details", 12.907622, 77.566029));
        places.add(new Place("Girls Hostel", "Floor and block details", 12.907585, 77.566336));
        places.add(new Place("Dayananda Sagar PUC College", "Floor and block details", 12.906493, 77.566692));
        places.add(new Place("NRI Hostel", "Floor and block details", 12.907141, 77.567376));
        places.add(new Place("Chemical Engineering Department", "Floor and block details", 12.907905, 77.567748));
        places.add(new Place("Aeronautical Engineering", "Floor and block details", 12.908702, 77.568060));
        places.add(new Place("Automobile Engineering", "Floor and block details", 12.909067, 77.568171));
        places.add(new Place("Student Parking Lot", "Floor and block details", 12.909237, 77.567709));
        places.add(new Place("Shavige Malleshwar Temple", "Floor and block details", 12.908541, 77.566566));
        places.add(new Place("Indian Hostel", "Floor and block details", 12.908452, 77.565324));
        places.add(new Place("DSCE Rock Garden", "Floor and block details", 12.907820, 77.567192));
        places.add(new Place("College Canteen", "Floor and block details", 12.908475, 77.566277));
        places.add(new Place("Hostel Mess", "Floor and block details", 12.908493, 77.566407));
        places.add(new Place("Football Ground", "Floor and block details", 12.906981, 77.566707));
        places.add(new Place("Basketball Court", "Floor and block details", 12.906981, 77.566707));
        places.add(new Place("Tennis Court", "Floor and block details", 12.906912, 77.567126));
        places.add(new Place("Mechanical Department", "Floor and block details", 12.908553, 77.567690));
        places.add(new Place("Building No. 10", "Floor and block details", 12.907819, 77.567594));
        places.add(new Place("Admission Hall", "Floor and block details", 12.908722287841645, 77.56637959258036));

        return places;
    }

    // Joint buildings: the route to the first place goes through the second one
    public static HashMap<String, String> getViaRules() {
        HashMap<String, String> via = new HashMap<>();
        via.put("AIML Department", "CSE Department");
        return via;
    }
}