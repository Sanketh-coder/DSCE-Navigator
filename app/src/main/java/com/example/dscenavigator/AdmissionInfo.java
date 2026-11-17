package com.example.dscenavigator;


import java.util.ArrayList;
import java.util.List;

public class AdmissionInfo {
    public String icon;
    public String title;
    public String description;
    public String placeName;   // must match a place name in CampusData exactly
    public String phone;

    public AdmissionInfo(String icon, String title, String description,
                         String placeName, String phone) {
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.placeName = placeName;
        this.phone = phone;
    }

    // TODO: replace every 0000000000 with the real number before the demo
    public static List<AdmissionInfo> getAll() {
        List<AdmissionInfo> list = new ArrayList<>();
        list.add(new AdmissionInfo("💰", "Fee Counter",
                "Pay admission and tuition fees",
                "Heritage Block", "0000000000"));
        list.add(new AdmissionInfo("🏛️", "Government Quota Admissions",
                "For KCET, DCET and COMEDK admissions",
                "Library", "0000000000"));
        list.add(new AdmissionInfo("🏢", "Management Admission",
                "For management quota admissions",
                "Admission Hall", "0000000000"));
        return list;
    }
}